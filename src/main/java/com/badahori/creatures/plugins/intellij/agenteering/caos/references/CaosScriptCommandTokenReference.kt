package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptCommandTokenReference(element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val name: String by lazy {
        element.name?.replace("\\s+".toRegex(), " ") ?: "{{UNDEF}}"
    }
    private val variants: List<CaosVariant> by lazy {
        when (myElement) {
            is CaosDefCompositeElement -> myElement.variants
            is CaosScriptCompositeElement -> myElement.containingCaosFile?.variant?.let { listOf(it) } ?: emptyList()
            else -> emptyList()
        }
    }

    private val check:(element:PsiElement) -> Boolean by lazy {
        val innerCheck:(commandString:String) -> Boolean = when {
            myElement.parent.parent is CaosDefCodeBlock -> {
                { commandString:String ->
                    names.any { it like commandString }
                }
            }
            myElement is CaosScriptIsCommandToken -> {
                { commandString:String ->
                    myElement.text.commandFormatted like commandString
                }
            }
            else -> {
                { commandString:String ->
                    myElement.text.commandFormatted like commandString
                }
            }
        }
        check@{ element:PsiElement ->
            if (!isDeclaration(element))
                return@check false
            val elementText = (element.parent as? CaosDefCommand)?.text?.commandFormatted
                    ?: return@check false
            innerCheck(elementText)
        }
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosScriptVarToken && element.varGroup.value.toUpperCase() == name.toUpperCase()) {
            return true
        }
        return check(element)
    }

    private fun isDeclaration(element:PsiElement) : Boolean {
        return element is CaosDefCommandWord
                && element.parent is CaosDefCommand
                && element.parent?.parent is CaosDefCommandDefElement
    }

    /**
     * Main entry into resolving command references
     */
    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(myElement.project))
            return emptyArray()
        if (myElement.parent?.parent is CaosDefCommandDefElement) {
            return PsiElementResolveResult.EMPTY_ARRAY
        }
        val elements = if (myElement is CaosDefCompositeElement)
            findFromDefElement()
        else
            findFromScriptElement()
        return PsiElementResolveResult.createResults(elements)
    }

    /**
     * In charge of finding references within the command def files
     */
    private fun findFromDefElement(): List<PsiElement> {
        if (myElement.parent is CaosDefCodeBlock) {
            return getCommandFromCodeBlockCommand()
        }
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

                // Get next and next next sibling for check in code block
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
            else -> {
                LOGGER.info("Failed to understand WORD context: ${parent.elementType}; for type: ${myElement.elementType}")
                listOf(name)
            }
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
     * Do not allow rename of base commands
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

private val String?.commandFormatted: String? get() = this?.replace("\\s+".toRegex(), " ")