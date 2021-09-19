package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptCommandTokenReference(element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.textLength)) {

    private val name: String by lazy {
        element.name?.commandFormatted?.nullIfEmpty() ?: "{{UNDEF}}"
    }

    private val commandType:CaosCommandType by lazy {
        element.getEnclosingCommandType()
    }

    private val variants: List<CaosVariant> by lazy {
        when (myElement) {
            is CaosDefCompositeElement -> myElement.variants
            is CaosScriptCompositeElement -> myElement.containingCaosFile?.variant?.let { listOf(it) } ?: emptyList()
            else -> emptyList()
        }
    }


    /**
     * Checks whether this element points to the element passed in
     */
    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefCompositeElement)
            return false
        val command: CaosDefCommand = element as? CaosDefCommand
            ?: element.parent as? CaosDefCommand
            ?: return false
        val commandString = command.text.replace(EXTRA_SPACE_REGEX, " ")
        if (myElement is CaosScriptVarToken && myElement.varGroup.value.equals(commandString, ignoreCase = true)) {
            return true
        }
        // If is command def element, it is not reference to anything else
        if (myElement.parent.isEquivalentTo(command))
            return true


        // Uses cached check method
        return command.variants.intersect(variants).isNotEmpty() && check(command)
    }

    private val check:(commandDef:CaosDefCommand) -> Boolean by lazy {
        val innerCheck:(commandString:String) -> Boolean = when (myElement.parent) {
            // Check for code block parent, as these are unstructured,
            // and combinations need to be checked manually
            is CaosDefCodeBlock -> {
                { commandString:String ->
                    names.any { it like commandString }
                }
            }
            // Should be only two other uses CaosDefCommandWord in CaosDefCommand parent
            // Or CaosScriptIsCommandToken with command parent
            else -> {
                { commandString:String ->
                    name like commandString
                }
            }
        }

        // Check lambda to ensure that command types match between this token, and the command def element
        val checkCommandType:(commandElement:CaosDefCommandDefElement) -> Boolean = when(commandType) {
            CaosCommandType.COMMAND -> { commandElement:CaosDefCommandDefElement ->
                commandElement.isCommand
            }
            CaosCommandType.LVALUE -> { commandElement:CaosDefCommandDefElement ->
                commandElement.isLvalue
            }
            CaosCommandType.RVALUE -> { commandElement:CaosDefCommandDefElement ->
                commandElement.isRvalue
            }
            CaosCommandType.CONTROL_STATEMENT -> { commandElement:CaosDefCommandDefElement ->
                commandElement.isCommand
            }

            // Handle special case of when command is not resolved to single CommandType
            CaosCommandType.UNDEFINED -> if (myElement is CaosDefCompositeElement)
                // If this check is reached, and myElement is CaosDef composite element
                // Then it should only exist in a CaosDefCodeBlock, which does not hold enclosing command information
                {_ -> true}
            else
                // Else if not CAOSDef composite element,
                // Then this element's type could not be determined, and should not be resolved
                { _ -> false }
        }

        // Creates and caches a checking method to check whether
        // this element references passed in element
        check@{ commandDef: CaosDefCommand ->
            // Only declarations can be pointed to
            // Return the passed in element is not
            if (!isDeclaration(commandDef))
                return@check false

            // Get and format passed in elements text
            val elementText = commandDef.text.commandFormatted
                    ?: return@check false

            // Runs check for names match
            if (!innerCheck(elementText))
                return@check false

            // Get parent CaosDefCommandDefElement for checking command type
            val commandElement = commandDef.parent as? CaosDefCommandDefElement
                    ?: return@check false

            // Check that referencing element and this element have matching command types
            // i.e. lvalue, rvalue or command
            checkCommandType(commandElement)
        }
    }

    /**
     * Checks whether the passed element is a Command definition element in a CAOS Def file
     */
    private fun isDeclaration(element:PsiElement) : Boolean {
        return element is CaosDefCommandWord
                && element.parent is CaosDefCommand
                && element.parent?.parent is CaosDefCommandDefElement
    }

    /**
     * Main entry into resolving command references
     */
    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        // Cannot resolve on dumb index, return
        if (DumbService.isDumb(myElement.project)) {
            return PsiElementResolveResult.EMPTY_ARRAY
        }
        // If parent.parent is CommandDefElement
        // this element would be pointing to itself
        // Return without self referencing result
        if (myElement.parent?.parent is CaosDefCommandDefElement) {
            return PsiElementResolveResult.EMPTY_ARRAY
        }
        // If element is CaosDefCompositeElement, it is a link in the file
        // So resolve it within itself
        val elements = if (myElement is CaosDefCompositeElement)
            findFromDefElement()
        // If element is not CaosDef element, it is a CaosScript element
        // Resolve it with indices
        else
            findFromScriptElement()
        return PsiElementResolveResult.createResults(elements)
    }

    /**
     * In charge of finding references within the command def files
     */
    private fun findFromDefElement(): List<PsiElement> {
        // If parent is CaosDefCode
        if (myElement.parent is CaosDefCodeBlock) {
            return getCommandFromCodeBlockCommand()
        }
        // If Parent is CaosDefCommand
        val parentCommand = myElement.parent as? CaosDefCommand
                ?: return emptyList()
        val index = parentCommand.commandWordList.indexOf(myElement)
        val variants = (myElement.containingFile as? CaosDefFile)?.variants
        val parentCommandString = parentCommand.commandWordList.joinToString(" ") { it.text }
        val rawMatches = findCommandDefElements(parentCommandString)
        return rawMatches
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.variants.intersect(variants.orEmpty()).isNotEmpty()
                }.mapNotNull {
                    it.command.commandWordList.getOrNull(index)
                }
                .filter { it != myElement }
    }

    /**
     * Finds references from CAOS files to CAOS def files
     */
    private fun findFromScriptElement(): List<CaosDefCommandWord> {
        val type = myElement.getEnclosingCommandType()
        val formattedName = name.replace(EXTRA_SPACE_REGEX, " ")
        val variant = myElement.variant
                ?: return emptyList()
        val matchesRaw = findCommandDefElements(formattedName)
        val matches = matchesRaw
                // Filter for type and variant
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    val isVariant = it.isVariant(variant)
                    if (!isVariant)
                        return@filter false
                    val isForElement = when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.CONTROL_STATEMENT -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                    if (!isForElement) {
                        if (!(it.isLvalue || it.isRvalue || it.isCommand)) {
                            return@filter false
                        }
                    }
                    isForElement
                }
        if (matches.size <= 1)
            return matches
                    .mapNotNull {
                        it.command.commandWordList.getOrNull(0)
                    }
        val parentArgument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return matches
                        .mapNotNull {
                            it.command.commandWordList.getOrNull(0)
                        }
        val expectedType = CaosLibs[variant][type][formattedName]
                ?.parameters
                ?.getOrNull(parentArgument.index)
                ?.type
                ?: return matches
                        .mapNotNull {
                            it.command.commandWordList.getOrNull(0)
                        }

        return (matches.filter { it.returnTypeString like expectedType.simpleName }.nullIfEmpty() ?: matches)
                .mapNotNull {
                    it.command.commandWordList.getOrNull(0)
                }
    }

    /**
     * Resolves references from within DOC comment code blocks
     */
    private fun getCommandFromCodeBlockCommand(): List<PsiElement> {
        val caseName = when {
            VARx.matches(name) -> "[Vv][Aa][Rr][Xx]"
            VAxx.matches(name) -> "[Vv][Aa][Xx][Xx]"
            OBVx.matches(name) -> "[Oo][Bb][Vv][Xx]"
            OVxx.matches(name) -> "[Oo][Vv][Xx][Xx]"
            MVxx.matches(name) -> "[Mm][Vv][Xx][Xx]"
            else -> name.toCharArray().joinToString("") {
                val lowerCase = it.toLowerCase()
                val upperCase = it.toUpperCase()
                if (lowerCase != upperCase)
                    "[$lowerCase$upperCase]"
                else if (it == '!' || it == '$')
                    "\\$it"
                else
                    "$it"
            }
        }
        if (name.equalsIgnoreCase("CNAM") || name.equalsIgnoreCase("DATA")) {
            if ((myElement.getPreviousNonEmptySibling(false) as? CaosDefCodeBlockPrimitive)?.codeBlockString != null)
                return CaosDefValuesListElementsByNameIndex.Instance["PutBOption", myElement.project]
                        .filter {
                            it.variantsIntersect(variants)
                        }
                        .mapNotNull { valuesListElement ->
                            valuesListElement.valuesListValueList.firstOrNull { it.key.equalsIgnoreCase(name) }
                        }
        }
        val rawMatches = findCommandDefElementsByPattern("([^ ]{4}[ ])*$caseName([ ][^ ]{4})*")
        val multiMatches = rawMatches
                .filter { commandDefElement ->
                    if (!commandDefElement.variantsIntersect(variants))
                        return@filter false
                    val words = commandDefElement.commandWords.map { it.toLowerCase() }
                    if (words.size < 2)
                        return@filter false
                    matchesWords(words)
                }
                .map {
                    val index = it.commandWords.indexOf(name)
                    it.command.commandWordList.getOrNull(index) ?: it.command
                }
        if (multiMatches.isNotEmpty())
            return multiMatches
        return findCommandDefElements(caseName)
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.variants.intersect(variants).isNotEmpty()
                }.map {
                    it.command
                }
                .filter { it != myElement }
    }

    /**
     * Gets names around this command for use in matching
     */
    private val names by lazy {
        when (val parent = myElement.parent) {
            is CaosDefCommand,
            is CaosScriptIsCommandToken -> listOf(parent.text.commandFormatted)
            // If CaosDef code block, the word could be self, or include previous or next
            is CaosDefCodeBlock -> {
                // Get previous sibling for check in comment code block
                val previousSibling = myElement.getPreviousNonEmptySibling(false)

                // Get previous and previous-previous sibling for comment code block check
                val prevPrevSibling = previousSibling?.getPreviousNonEmptySibling(false)?.let {
                    it.text + " " + previousSibling.text + " " + name
                }.commandFormatted

                // Get next sibling for check in comment code block
                val nextSibling = myElement.getNextNonEmptySibling(false)

                // Get next and next's next sibling for check in code block
                val nextNextSibling = nextSibling?.getNextNonEmptySibling(false)?.let {
                    it.text + " " + nextSibling.text + " " + name
                }.commandFormatted

                // Get self in middle for 3 part command
                val inMiddle = nextSibling?.text?.let { next ->
                    previousSibling?.text?.let { prev ->
                        "$prev $name $next"
                    }
                }.commandFormatted

                // Create a list of possibilities involving this code block
                listOfNotNull(
                        name,
                        previousSibling?.text?.let { "$it $name" } .commandFormatted,
                        prevPrevSibling,
                        inMiddle,
                        nextSibling?.let { "$it $name" }.commandFormatted,
                        nextNextSibling
                )
            }
            else -> listOf(name)
        }
    }

    /**
     * Checks that a list of words matches this token and possibly the tokens before and after
     */
    private fun matchesWords(words: List<String>): Boolean {
        when (words.indexOf(name.toLowerCase())) {
            0 -> {
                var next: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words) {
                    if (next == null || !next.text.equalsIgnoreCase(word))
                        return false
                    next = next.getNextNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            1 -> {
                (myElement.getPreviousNonEmptySibling(false) as? CaosDefCommandWord)?.let {
                    if (!it.text.equalsIgnoreCase(words[0]))
                        return false
                }
                        ?: return false
                if (words.size == 2)
                    return true
                var next: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words.subList(1, words.size)) {
                    if (next == null || !next.text.equalsIgnoreCase(word))
                        return false
                    next = next.getNextNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            2 -> {
                var previous: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words.reversed()) {
                    if (previous == null || !previous.text.equalsIgnoreCase(word))
                        return false
                    previous = previous.getPreviousNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            else -> return false
        }
        return true
    }

    /**
     * Do not allow re-name of base commands
     */
    override fun handleElementRename(newElementName: String): PsiElement {
        //if (renameRegex.matches(newElementName))
        //  return myElement.setName(newElementName)
        return myElement
    }

    /**
     * Finds command def elements in both index and VFS
     */
    private fun findCommandDefElements(commandName: String): List<CaosDefCommandDefElement> {
        val project = myElement.project
        return CaosDefCommandElementsByNameIndex.Instance[commandName, project]
    }

    /**
     * Finds command def elements by pattern in both index and VFS
     */
    private fun findCommandDefElementsByPattern(pattern: String): List<CaosDefCommandDefElement> {
        val project = myElement.project
        return CaosDefCommandElementsByNameIndex.Instance.getByPatternFlat(pattern, project)
    }


    companion object {
        val EXTRA_SPACE_REGEX = "\\s\\s+".toRegex()
        private val VARx = "[Vv][Aa][Rr][0-9]".toRegex()
        private val VAxx = "[Vv][Aa][0-9][0-9]".toRegex()
        private val OBVx = "[Oo][Bb][Vv][0-9]".toRegex()
        private val OVxx = "[Oo][Vv][0-9][0-9]".toRegex()
        private val MVxx = "[Mm][Vv][0-9][0-9]".toRegex()

    }

}

private val String?.commandFormatted: String? get() = this?.replace(WHITESPACE, " ")