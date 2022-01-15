package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import bedalton.creatures.util.PathUtil
import bedalton.creatures.util.pathSeparator
import bedalton.creatures.util.pathSeparatorChar
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.psiDirectory
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayCommand.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.case
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.VALID_SPRITE_EXTENSIONS
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import icons.CaosScriptIcons
import stripSurroundingQuotes
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

/**
 * Provides completions for CAOS script
 * Adds completions for commands, variables, subroutine names, and known values.
 */
object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val IS_NUMBER = "^[0-9]+".toRegex()
    private val WHITESPACE_ONLY = "^\\s+$".toRegex()

    @Suppress("SpellCheckingInspection")
    private val SKIP_VAR_NAMES = listOf("VARX", "OBVX", "MVXX", "OVXX", "VAXX")

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet
    ) {
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
            ?: return
        val variant = caosFile.variant
            ?: return

        val project = element.project

        val text = element.textWithoutCompletionIdString
        if (text.isNotEmpty() && IS_NUMBER.matches(text) && !parameters.isExtendedCompletion) {
            resultSet.stopHere()
            return
        }
        val caos2Block = element.getParentOfType(CaosScriptCaos2Block::class.java)

        if (caos2Block != null) {
            // If in caos2 block, but is not CAOS2 child, return as no completion is needed
            val caos2Child = element.getSelfOrParentOfType(PrayChildElement::class.java)
                ?: return
            addCaos2Completion(
                resultSet,
                variant,
                caos2Block,
                caos2Child
            )

            // No more completions should be added
            return
        }

        val previous = element.previous?.text

        if (element is CaosScriptStringText || element.parent is CaosScriptStringText || element is CaosScriptQuoteStringLiteral) {
            addStringCompletions(project, resultSet, expandedSearch = parameters.isExtendedCompletion, variant, element)
            resultSet.stopHere()
            return
        }

        // If previous is not whitespace and not square or double quote symbol, return
        if (previous != "[" && previous != "\"" && previous.nullIfEmpty()?.let { !WHITESPACE_ONLY.matches(it) }
                .orFalse()) {
            resultSet.stopHere()
            return
        }

        val case = text.case

        // Add equality expression completions for known types
        val argument = element.getSelfOrParentOfType(CaosScriptArgument::class.java)
        (argument as? CaosScriptRvalue)?.let { expression ->
            val needsEverything = parameters.isExtendedCompletion ||
                    text.length > 4 ||
                    (element.parent?.getEnclosingCommandType()?.let { commandType ->
                        when (commandType) {
                            COMMAND -> CaosLibs[variant].commands
                            RVALUE -> CaosLibs[variant].rvalues
                            LVALUE -> CaosLibs[variant].lvalues
                            else -> null
                        }?.let { commands ->
                            commands.filter { command ->
                                command.command like text
                            }.size < 3
                        }
                    } ?: false)
            // If element's parent is a rvalue, should continue with normal completion
            CaosScriptValuesListValuesCompletionProvider.addParameterTypeDefValueCompletions(
                resultSet,
                variant,
                argument,
                needsEverything
            )
            // Else use special EQ expression completion
            (expression.parent as? CaosScriptEqualityExpressionPrime)?.let { equalityExpression ->
                CaosScriptValuesListValuesCompletionProvider.addEqualityExpressionCompletions(
                    variant,
                    resultSet,
                    case,
                    equalityExpression,
                    argument,
                    needsEverything
                )
                addCommandCompletions(resultSet, variant, RVALUE, argument)
                resultSet.stopHere()
                return
            }
        }


        // Get previous token, in case a special completion is in order
        val previousToken = element.getPreviousNonEmptySibling(false)
        val previousTokenText = previousToken?.text?.toUpperCase()

        // Previous token is CLAS, add class generator
        if (previousTokenText == "CLAS") {
            val builderElement = LookupElementBuilder
                .create("")
                .withPresentableText(GENERATE_CLAS_LOOKUP_STRING)
                .withInsertHandler(GenerateClasIntegerInsertHandler)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        //If previous is number, return
        if (IS_NUMBER.matches(previous ?: "")) {
            resultSet.stopHere()
            return
        }

        // Previous token is DDE: PICT, add PICT value generator
        if (previousTokenText == "DDE: PICT") {
            val builderElement = LookupElementBuilder
                .create("")
                .withPresentableText(GENERATE_DDE_PICT_LOOKUP_STRING)
                .withInsertHandler(GeneratePictDimensionsAction)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        if (
            (previousToken is PsiErrorElement && previousToken.parent is CaosScriptEventScript) ||
            element is CaosScriptEventNumberElement ||
            element.parent is CaosScriptEventNumberElement ||
            (previousToken is CaosScriptSpecies && previousToken.parent?.parent is CaosScriptEventScript) ||
            (previousToken?.lastChild is CaosScriptSpecies && previousToken.parent is CaosScriptEventScript)
        ) {
            CaosLibs[variant]
                .valuesList("EventNumbers")
                ?.values
                ?.forEach { item ->
                    resultSet.addElement(
                        LookupElementBuilder.create(item.value)
                            .withPresentableText("${item.value} - ${item.name}")
                            .withLookupString("${item.value} - ${item.name}")
                            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
                    )
                }
            return
        }

        // Requires a SUBR name
        if (element.hasParentOfType(CaosScriptCGsub::class.java)) {
            addSubroutineNames(element, resultSet)
            resultSet.stopHere()
            return
        }

        // Get parent Command element
        var parent: PsiElement? = element
        while (parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
            parent = parent.parent
        }
        // Element has no command parent
        if (parent == null) {
            resultSet.stopHere()
            return
        }
        // Get command type, ie RVALUE, LVALUE, COMMAND
        val commandType = parent.getEnclosingCommandType()

        // If token is expected, return
        // Token completions will have been added with CaosScriptValuesListValuesCompletionProvider#addParameterTypeDefValueCompletions,
        // called in the command previously
        if (element.hasParentOfType(CaosScriptTokenRvalue::class.java)) {
            resultSet.stopHere()
            return
        }

        addCommandCompletions(resultSet, variant, commandType, element)
        // If type is Lvalue or Rvalue, add variable name completions
        if (commandType != COMMAND)
            addVariableCompletions(variant, element, resultSet)
    }

    private fun addCommandCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        commandType: CaosCommandType,
        element: PsiElement
    ) {
        val case = element.case
        val allowUppercase = variant !in VARIANT_OLD
        val caosLib = CaosLibs[variant]
        val singleCommands = when (commandType) {
            COMMAND -> caosLib.commands
            RVALUE -> caosLib.rvalues
            LVALUE -> caosLib.lvalues
            else -> return
        }.filter { it.command likeNone SKIP_VAR_NAMES }.map {
            ProgressIndicatorProvider.checkCanceled()
            createCommandTokenLookupElement(
                allowUppercase,
                element,
                case,
                it.command,
                commandType,
                it.parameters,
                it.returnType
            )
        }
        resultSet.addAllElements(singleCommands)
    }

    /**
     * Adds variables to completions for a given variant.
     * i.e. VAR0 -> VAR9 on C1 or VA00-VA99 on C2+
     */
    private fun addVariableCompletions(variant: CaosVariant, element: PsiElement, resultSet: CompletionResultSet) {
        if (variant == CaosVariant.C1) {
            (0..2).forEach {
                resultSet.addElement(LookupElementBuilder.create("obv$it"))
            }
            (0..9).forEach {
                resultSet.addElement(LookupElementBuilder.create("var$it"))
            }
        } else if (variant == CaosVariant.C2) {
            (0..9).forEach {
                resultSet.addElement(LookupElementBuilder.create("obv$it"))
            }
            (0..9).forEach {
                resultSet.addElement(LookupElementBuilder.create("var$it"))
            }
        }
        if (variant != CaosVariant.C1 && element.text.substringBefore("zz").matches("[vom].*?".toRegex())) {
            val items = (0..99).map {
                "$it".padStart(2, '0')
            }
            items.map {
                resultSet.addElement(LookupElementBuilder.create("va$it"))
                resultSet.addElement(LookupElementBuilder.create("ov$it"))
            }
            if (variant != CaosVariant.C2) {
                items.map {
                    resultSet.addElement(LookupElementBuilder.create("mv$it"))
                }
            }
        }
    }

    /**
     * Creates a completion lookup element for a command
     */
    private fun createCommandTokenLookupElement(
        allowUppercase: Boolean,
        element: PsiElement,
        case: Case,
        commandIn: String,
        commandType: CaosCommandType,
        parameters: List<CaosParameter>,
        returnType: CaosExpressionValueType,
        prefixIn: String? = null
    ): LookupElement {
        // Tail text for command display element
        var tailText = returnType.simpleName
        if (!tailText.startsWith("["))
            tailText = "($tailText)"

        // If an icon is possible, use it here
        val icon = when (commandType) {
            COMMAND -> CaosScriptIcons.COMMAND
            RVALUE -> CaosScriptIcons.RVALUE
            LVALUE -> CaosScriptIcons.LVALUE
            else -> null
        }
        // Create completion for a given case
        val command = if (case == Case.UPPER_CASE && allowUppercase)
            commandIn.toUpperCase()
        else
            commandIn.toLowerCase()

        // If command has prefix then match case
        val prefix = when {
            prefixIn == null -> null
            case == Case.UPPER_CASE -> prefixIn.toUpperCase()
            else -> prefixIn.toLowerCase()
        }

        // Create actual element
        var builder = LookupElementBuilder
            .create(command)
            .withTailText(tailText)
            .withIcon(icon)
        // Check needs space requirement
        // Mostly just adds a space if there are additional parameters
        val needsSpace = needsSpaceAfter(element, command)
        // Add parameter insert handler if needed
        if (parameters.isNotEmpty()) {
            builder = builder.withInsertHandler(CommandInsertHandler(command, parameters, needsSpace))
        } else if (needsSpace)
            builder = builder.withInsertHandler(SpaceAfterInsertHandler)
        if (prefix.isNotNullOrBlank()) {
            builder = builder.withPresentableText("$prefix $command")
        }
        return builder
            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
    }

    /**
     * Checks if a space is needed after insert
     */
    private fun needsSpaceAfter(element: PsiElement, commandString: String): Boolean {
        var parent: PsiElement? = element.parent
        while (parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
            parent = parent.parent
        }
        if (parent == null)
            return false
        val variant = (element.containingFile as? CaosScriptFile)?.variant
            ?: return true
        val caosLib = CaosLibs[variant]
        val matches = when (parent) {
            is CaosScriptCommandCall -> caosLib.commands
            is CaosScriptRvalue -> caosLib.rvalues
            is CaosScriptLvalue -> caosLib.lvalues
            else -> return false
        }.filter { it.command like commandString }
        if (matches.isEmpty())
            return false
        return matches.all { it.parameters.isNotEmpty() }
    }

    /**
     * Adds completion for subroutine names in a given SCRP element
     */
    private fun addSubroutineNames(element: PsiElement, resultSet: CompletionResultSet) {
        val project = element.project
        val file = element.containingFile
        val scope = GlobalSearchScope.everythingScope(project)
        val subroutines = CaosScriptSubroutineIndex.instance.getAllInScope(project, scope)
            .filter {
                file.name == (it.originalElement?.containingFile ?: it.containingFile)?.name
            }
            .mapNotNull { it.name.nullIfEmpty() }
            .toSet()
        for (subr in subroutines) {
            val builder = LookupElementBuilder.create(subr)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(builder)
        }
    }

    private fun addCaos2Completion(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        caos2Block: CaosScriptCaos2Block,
        caos2Child: PrayChildElement
    ) {

        // Ensure element has caos file parent
        val caosFile = caos2Child.containingFile as? CaosScriptFile
            ?: return

        // Make sure parent is command or tag, as those should be the only parents
        val parent = caos2Child.parent as? CaosScriptCaos2Statement

        if (parent == null) {
            LOGGER.severe("Found CAOS2Child in unexpected context. Parent: ${caos2Child.parent?.elementType}")
            return
        }

        val caos2Cob = variant.isOld && caos2Block.isCaos2Cob
        val caos2Pray = variant.isNotOld && caos2Block.isCaos2Pray

        // Get case for Command name completion which is case flexible (at least in my plugin)
        val case = caos2Child.text.stripSurroundingQuotes().replace(DUMMY_IDENTIFIER_TRIMMED, "").nullIfEmpty()?.case
            ?: Case.CAPITAL_FIRST

        if (caos2Cob) {

            // Add tag completions here too, as tag is command before any other values are added
            addCaos2CobTagCompletions(
                resultSet,
                variant,
                caosFile,
                caos2Child
            )
        } else {
            addCaos2PrayTagCompletions(
                resultSet,
                caosFile.prayTags,
                caos2Child,
                case,
                isCaosScriptFile = true
            )
        }

        // Add completions for CAOS2 command names or tag names
        if (caos2Child is CaosScriptCaos2CommandName) {
            // Get all previous commands to prevent suggesting them again
            val previousCommands = caos2Block
                .commands
                .map { it.first }
                .toSet()
            if (caos2Cob) {
                addCaos2CobCommandCompletions(resultSet, variant, case, previousCommands)
            } else if (caos2Pray) {
                addCaos2PrayCommandCompletions(resultSet, case, previousCommands)
            }
            return
        } else if (caos2Child is CaosScriptCaos2TagName) {
            return
        }

        // Get child as pray tag value
        val value = caos2Child as? PrayTagValue

        // If CAOS2Child is not a pray tag value
        // This should not happen
        if (value == null) {
            LOGGER.severe("Reached CAOS 2 completion with an unhandled CAOS2 element. Type: ${caos2Child.elementType}")
            return
        }
        val directory = caos2Child.directory
            ?: return

        val text = value.text
        val openQuote: Char? = text.firstOrNull()?.let {
            if (it == '\"' || it == '\'')
                it
            else
                null
        }

        if (caos2Cob) {
            val cobTag = (parent as? CaosScriptCaos2Tag)?.let { tagElement ->
                CobTag.fromString(tagElement.tagName)
            }
            addCaos2CobFileNameCompletions(
                resultSet,
                directory,
                cobTag,
                value,
                openQuote,
            )
        }
        addCaos2PrayFileNameCompletions(variant, resultSet, directory, value, openQuote)
        resultSet.stopHere()
    }

    /**
     * Add CAOS2Cob TAG name completion
     */
    private fun addCaos2CobTagCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        caosFile: CaosScriptFile,
        tagNameElement: PrayChildElement,
    ) {
        val existingTags: List<CobTag> = caosFile.prayTags.map { it.tag }.mapNotNull { tagString ->
            CobTag.fromString(tagString)
        }
        val tagsRaw = CobTag.getTags(variant).filterNot { tag -> tag in existingTags }

        val tagTextRaw = tagNameElement.tagText
            ?: return
        val tagTextRawLength = tagTextRaw.length
        // Get tag text if any including previous siblings
        val textComponents = tagTextRaw
            .split("\\s+".toRegex())

        val case = tagTextRaw.nullIfEmpty()?.case ?: Case.CAPITAL_FIRST

        // Getting all possible tag names
        // Filter them down to the variant of the tag, that is closest to the string already types
        // In this plugin, there are name alternatives for tags in CAOS2Cob
        val tags: List<LookupElement> = tagsRaw.mapNotNull map@{ tag ->
            // Get tag keys
            val keys = tag.keys
            // If one starts with text, return it first
            keys.firstOrNull { key -> key.startsWith(tagTextRaw) }?.let { key ->
                return@map key
            }

            // Get the entered texts distance from all keys in this tag
            @Suppress("SimplifiableCallChain")
            val match = keys.mapNotNull keys@{ key ->
                val keyLength = key.length
                if (keyLength == 0)
                    return@keys null

                val mod = min(tagTextRawLength, keyLength) / max(tagTextRawLength, keyLength)
                val distance = tagTextRaw.levenshteinDistance(key) * mod
                Pair(distance, key)
            }.sortedBy { it.first }
                .firstOrNull()
                ?: return@map null
            // Decide on completion tag. If tags are too far out of
            (if (match.first > 0.8) {
                keys.first()
            } else {
                match.second
            }).matchCase(case)
        }.map { key ->
            val parts = key.split(' ')
            val skip = textComponents.size - 1
            val text = parts.drop(skip).joinToString(" ")
            return@map LookupElementBuilder.createWithSmartPointer(text, tagNameElement)
                .withLookupStrings(listOf(key, text))
                .withPresentableText(key)
                .withInsertHandler(EqualSignInsertHandler)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        }
        resultSet.addAllElements(tags)
    }


    /**
     * Add CAOS2Cob command name completion
     */
    private fun addCaos2CobCommandCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        case: Case,
        commandsUsed: Set<String>
    ) {
        CobCommand.getCommands(variant).forEach command@{ command ->
            if (command.singleton && commandsUsed.any { command.key.matches(it) })
                return@command
            resultSet.addElement(
                LookupElementBuilder.create(command.keyStrings.first().matchCase(case))
                    .withInsertHandler(SpaceAfterInsertHandler)
            )
        }
        resultSet.addElement(
            LookupElementBuilder.create("Link".matchCase(case))
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        )
    }

    /**
     * Add PRAY command completion for CAOS2Pray files
     */
    private fun addCaos2PrayCommandCompletions(
        resultSet: CompletionResultSet,
        case: Case,
        commandsUsed: Set<String>
    ) {
        PrayCommand.values().forEach command@{ command ->
            if (command.singleton && commandsUsed.any { command.key.matches(it) })
                return@command
            resultSet.addElement(
                LookupElementBuilder.create(command.keyStrings.first().matchCase(case))
                    .withInsertHandler(SpaceAfterInsertHandler)
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            )
        }
    }


    /**
     * Add Tag completion for CAOS2Cob files
     */
    internal fun addCaos2PrayTagCompletions(
        resultSet: CompletionResultSet,
        prayTags: List<PrayTagStruct<*>>,
        tagElement: PrayChildElement?,
        case: Case?,
        isCaosScriptFile: Boolean,
        eggs: Boolean = false
    ) {
        val existingTags: List<String> = prayTags.map { it.tag.toLowerCase() }
        val tagTextRaw = tagElement?.tagText ?: ""
        // Get tag text if any including previous siblings
        val textComponents = tagTextRaw
            .split("\\s+".toRegex())
        val skip = max(0, textComponents.size - 1)

        val tags = PrayTags.allLiterals
            .keys
            .filter {
                !(isCaosScriptFile && PrayTags.isAutogenerated(
                    it,
                    true
                )) && it.toLowerCase() !in existingTags && PrayTags.getShortTagReversed(it, true) !in existingTags
            } + (
                if (isCaosScriptFile) PrayTags.shortTagLiterals
                    .keys
                    .filter { tagName ->
                        tagName.toLowerCase() !in existingTags && PrayTags.normalize(tagName)?.toLowerCase()
                            ?.let { normalizedTag ->
                                !(isCaosScriptFile && PrayTags.isAutogenerated(
                                    normalizedTag,
                                    true
                                )) && normalizedTag !in existingTags
                            } == true
                    }.let { tags ->
                        if (case != null && isCaosScriptFile) {
                            tags.map { tag ->
                                tag.matchCase(case)
                            }
                        } else
                            tags
                    }
                else
                    emptyList()
                ) + (
                if (eggs) {
                    PrayTags.eggTagLiterals
                        .keys
                        .filter { it.toLowerCase() !in existingTags }
                } else
                    emptyList()
                )

        val hasTagElement = tagElement != null
        var elements = tags.map { tag ->
            val parts = tag.split(' ')
            val text = parts.drop(skip).joinToString(" ")
            (if (hasTagElement) {
                LookupElementBuilder.createWithSmartPointer(text, tagElement!!)
            } else {
                LookupElementBuilder.create(text)
            })
                .withLookupString(text)
                .withPresentableText(tag)
        }
        if (isCaosScriptFile) {
            elements = elements.map {
                it.withInsertHandler(EqualSignInsertHandler)
            }
        }
        resultSet.addAllElements(elements.map {
            it.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        })
    }


    /**
     * Add filename completion for CAOS2Cob tag
     */
    private fun addCaos2CobFileNameCompletions(
        resultSet: CompletionResultSet,
        directory: VirtualFile,
        tag: CobTag?,
        valueElement: PrayTagValue,
        openQuote: Char?
    ) {

        //
        if (tag != CobTag.THUMBNAIL)
            return


        // Get files of appropriate type
        val files = VirtualFileUtil.childrenWithExtensions(
            directory,
            true,
            "spr",
            "s16",
            "c16",
            "png",
            "jpg",
            "jpeg",
            "gif",
            "bmp"
        )

        // Ensure there are matching files
        if (files.isEmpty())
            return

        // Get parent path length for sizing down the relative child paths
        val parentPathLength = directory.path.length + 1

        val quoteFallback = openQuote?.toString() ?: "\""

        // Loop through files and add lookup elements
        for (file in files) {
            // Get relative path from parent
            val relativePath = file.path.substring(parentPathLength)

            // Get quotes to add if any
            val needsQuote = relativePath.contains(whiteSpaceOrQuote)
            val quote = if (!needsQuote) {
                openQuote?.toString().orEmpty()
            } else {
                quoteFallback
            }

            // Add quotes and also escape any quotes found inside this path
            val quotedPath = quote + relativePath.replace(quote, "\\" + quote) + quote


            val element = if (file.extension likeAny VALID_SPRITE_EXTENSIONS) {
                // Process filenames if is thumbnail
                val fileParts = quotedPath.split(".")
                if (fileParts.size < 2)
                    continue
                val basePath = fileParts.dropLast(1).joinToString(".")

                // Create insert handler for use by thumbnail tags to move the cursor between the [^] in sprite thumbnails
                val insertHandler = OffsetCursorInsertHandler(basePath.length + 1) // ^].spr"

                LookupElementBuilder.createWithSmartPointer(basePath + "[#]." + fileParts.last(), valueElement)
                    .withLookupString(basePath + "[]." + fileParts.last())
                    .withInsertHandler(insertHandler)
            } else {
                LookupElementBuilder.createWithSmartPointer(relativePath, valueElement)
                    .withLookupStrings(listOf(file.name, relativePath))
            }
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(element)
        }
    }

    /**
     * Add CAOS2Pray file name completion for known tags
     */
    internal fun addCaos2PrayFileNameCompletions(
        variant: CaosVariant,
        resultSet: CompletionResultSet,
        directory: VirtualFile,
        element: PrayTagValue,
        openQuote: Char?
    ) {
        val (requiredExtensions: List<String>?, dropExtension: Boolean) = (element.parent as? PrayTag)?.let { tag ->
            val tagName = tag.tagName
                .nullIfEmpty()
                ?: return
            tagRequiresFileOfType(tagName)
        } ?: (element.parent as? CaosScriptCaos2Command)?.let { command ->
            val extensions = if (element.containingCaosFile?.caos2 != CAOS2Cob) {
                val prayCommand = PrayCommand.fromString(command.commandName, variant)
                    ?: return
                when (prayCommand) {
                    LINK -> listOf("cos", "caos")
                    INLINE -> null
                    ATTACH -> null
                    DEPEND -> null
                    REMOVAL_SCRIPTS -> listOf("cos", "caos")
                    PRAY_FILE -> return
                }
            } else {
                val cobCommand = CobCommand.fromString(command.commandName, variant)
                    ?: return
                when (cobCommand) {
                    CobCommand.LINK -> listOf("cos", "caos")
                    CobCommand.COBFILE -> return
                    CobCommand.INSTALL_SCRIPTS -> listOf("cos", "caos")
                    CobCommand.REMOVAL_SCRIPTS -> listOf("cos", "caos")
                    CobCommand.ATTACH -> listOf("wav", "s16")
                    CobCommand.INLINE -> listOf("wav", "s16")
                    CobCommand.DEPEND -> listOf("wav", "s16")
                }
            }
            Pair(extensions, false)
        } ?: return

        val trueDirectory = element.text.stripSurroundingQuotes(2)
            .substringFromEnd(0, DUMMY_IDENTIFIER_TRIMMED.length).let { thePath: String ->
                val components: List<String> = if (thePath.contains('/')) {
                    thePath.split("/")
                } else if (thePath.contains('\\')) {
                    thePath.split("\\")
                } else {
                    emptyList()
                }
                val testPath = PathUtil.combine(directory.path, *components.toTypedArray())

                VfsUtil.findFile(Path.of(testPath), false)
                    ?: VfsUtil.findFile(
                        Path.of(PathUtil.combine(directory.path, *components.dropLast(1).toTypedArray())),
                        false
                    )
            } ?: directory


        // Get files of appropriate type
        val files = if (requiredExtensions != null) {
            VirtualFileUtil.childrenWithExtensions(trueDirectory, false, *requiredExtensions.toTypedArray())
        } else {
            VirtualFileUtil.collectChildFiles(trueDirectory, false)
        }

        // Ensure there are matching files
        if (files.isEmpty())
            return

//        val firstChar = element.text[0]
//        val quoteCharBase = if (element.containingFile is PrayFile)
//            '"'
//        else if (firstChar == '"' || firstChar == '\'')
//            firstChar
//        else
//            null

//        val insertHandler = InsertInsideQuoteHandler(quoteCharBase ?: '"', quoteCharBase ?: '"'
        // Loop through files and add lookup elements
        for (file in files) {
            val childPath = VfsUtil.findRelativePath(trueDirectory, file, pathSeparatorChar)
                ?: continue
            val relativePath = if (dropExtension) {
                val parentPath = childPath
                    .split(pathSeparator)
                    .dropLast(1)
                    .joinToString(pathSeparator)
                    .nullIfEmpty()
                    ?.let { it + pathSeparator }
                val baseName = FileNameUtils.getNameWithoutExtension(childPath)
                    ?: continue
                parentPath + baseName
            } else {
                childPath
            }

            val needsQuote = relativePath.contains(whiteSpaceOrQuote)
            val quote = if (needsQuote && openQuote == null) {
                "\""
            } else {
                openQuote?.toString() ?: ""
            }

            val builder = LookupElementBuilder
                .createWithSmartPointer("$quote$relativePath$quote", element)
                .withLookupStrings(listOf(file.name, childPath))
                resultSet.addElement(
                builder
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            )
        }
    }

}

/**
 * Gets element text minus the weird IntelliJ completion string appended to the end
 */
internal val PsiElement.textWithoutCompletionIdString: String
    get() = text.split(
        DUMMY_IDENTIFIER_TRIMMED
    ).first()

private val whiteSpaceOrQuote = "[ \t'\"]".toRegex()

private val PrayChildElement.tagText: String?
    get() {
        if (this is PrayTagName)
            return this.text
        if (this is CaosScriptCaos2CommandName)
            return this.text
        else if (this.firstChild is CaosScriptQuoteStringLiteral && this.prevSibling != null) {
            // This is a quoted string not at the beginning, so it has to a value inside a command
            return null
        }
        // Get parent caos2 statement
        val parent = this.parent as CaosScriptCaos2Statement

        // Get all non-whitespace children
        val children = parent.children.filter { it.tokenType != TokenType.WHITE_SPACE }
            .nullIfEmpty()
            ?: return ""

        // This element has child elements in quotes, so it cannot be a tag
        if (children.drop(1).any { child -> child.text.firstOrNull()?.let { it == '\'' || it == '"' } == true })
            return null

        val startOffset = this.startOffset
        val previousSiblings = children.filter { it.startOffset < startOffset }

        // This element comes after an equal sign
        if (previousSiblings.any { it.tokenType == CaosScriptTypes.CaosScript_EQUAL_SIGN })
            return null

        // This tag appears to be in the middle of a set of words
        val text = previousSiblings.joinToString(" ") { it.text }.trim() + ' ' + this.text

        return if (text.endsWith(DUMMY_IDENTIFIER_TRIMMED))
            text.substringFromEnd(0, DUMMY_IDENTIFIER_TRIMMED.length).trim()
        else
            text
    }


private fun addStringCompletions(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    variant: CaosVariant,
    element: PsiElement
) {
    val parentRvalue = element.parent.getParentOfType(CaosScriptRvalue::class.java)
        ?: return
    val parentCommandElement = (parentRvalue.parent as? CaosScriptCommandElement)
        ?: return

    val quote = if (element.text.startsWith('\"')) "\"" else ""

    val commandName = parentCommandElement.commandStringUpper
    when (commandName) {
        "GAME" -> {
            addGameNameCompletions(project, resultSet, variant, expandedSearch, element, quote)
            return
        }
        "NAME", "MAME" -> {
            addUserDefinedNamedVarsOfType(
                resultSet,
                project,
                listOf(NAME, MAME),
                expandedSearch,
                element,
                quote
            )
            return
        }
        "EAME" -> {
            addUserDefinedNamedVarsOfType(
                resultSet,
                project,
                listOf(EAME),
                expandedSearch,
                element,
                quote
            )
            return
        }
    }
    val parentCommand = parentCommandElement.commandDefinition
        ?: return


    val index = parentRvalue.index
    if (index < 0) {
        return
    }
    val parameter = parentCommand.parameters.getOrNull(index)
        ?: return
    val valuesList = parameter.valuesList[variant]
        ?: return

    if (valuesList.name.startsWith("File.", ignoreCase = true)) {
        val extensions = valuesList
            .name
            .toLowerCase()
            .substring(5)
            .trim()
            .split("/")
            .let {
                if (it.first() == "cos")
                    listOf("cos", "caos")
                else
                    it
            }
        val dropExtension = parentCommand.command != "JECT"
        addFileNameCompletionsForFileTypes(
            project,
            resultSet,
            expandedSearch,
            extensions,
            dropExtension,
            element,
            quote
        )
        return
    }

}


private fun addFileNameCompletionsForFileTypes(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    extensions: Collection<String>,
    dropExtension: Boolean,
    element: PsiElement,
    quote: String
) {

    // Requires file index, so stop completing if index is not yet built
    if (DumbService.isDumb(project))
        return

    // Get module of containing file
    val module = element.module

    // Get search scope
    // If expanded search is false, only get files in the parent directory
    val searchScope = if (!expandedSearch) {
        val parentDirectory = element.psiDirectory
        parentDirectory?.let {
            GlobalSearchScopes.directoryScope(it, true)
        } ?: parentDirectory?.resolveScope
    } else
        module?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)

    // Util function to search for files with an extension and scope if non-null
    val search: (extension: String) -> Collection<VirtualFile> = if (searchScope != null) {
        ({ extension: String ->
            FilenameIndex.getAllFilesByExt(project, extension, searchScope)
        })
    } else {
        ({ extension: String ->
            FilenameIndex.getAllFilesByExt(project, extension)
        })
    }

    // Get project path for location context
    val projectPath = project.basePath
    val projectPathLength = projectPath?.length?.let { it + 1 } ?: 0

    // Get module path for location context
    val modulePath = module?.moduleFilePath
    val modulePathLength = modulePath?.length?.let { it + 1 } ?: 0

    // Util method to get the relative path between a file and its parent module or directory
    val relativePath: (path: String?) -> String? = path@{ path: String? ->
        if (path == null)
            return@path null
        if (modulePath != null && path.startsWith(modulePath))
            path.substring(modulePathLength)
        else if (projectPath != null && path.startsWith(projectPath))
            path.substring(projectPathLength)
        else
            null
    }

    // Get files as completion elements
    val files = extensions
        .flatMap { extension ->
            (search(extension) + search(extension.toUpperCase()))
                .distinctBy {
                    it.path
                }
        }
        .toSet()
        .map { file ->
            // Get the file name, dropping extension if necessary
            val completionText = if (dropExtension)
                file.nameWithoutExtension
            else
                file.name
            // Get location string for autocomplete tail-text
            val location = relativePath(file.parent?.path)?.let { " in $it" } ?: "..."
            LookupElementBuilder
                .createWithSmartPointer("$quote$completionText$quote", element)
                .withLookupString(completionText)
                .withPresentableText(file.name)
                .withTailText(location, true)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        }

    // Add all elements
    resultSet.addAllElements(files)

}

/**
 * Adds completion strings for known game variables
 */
private fun addGameNameCompletions(
    project: Project,
    resultSet: CompletionResultSet,
    variant: CaosVariant,
    expandedSearch: Boolean,
    element: PsiElement,
    quote: String
) {
    val gameVariantName = "GameVariables"
    val values = CaosLibs[variant].valuesList(gameVariantName)
        ?.values
        .nullIfEmpty()
        .apply {
            if (this == null)
                LOGGER.severe("CAOS lib ${variant.code} has no list for game variables")
        }
        ?: return


    val elements = (values).map { value ->
        val completionText = value.value

        // Loads up the description, or fragment if description is too long
        val description = value.description
            .nullIfEmpty()
            ?.let { text ->
                " - " + (if (text.length > 47)
                    text.substring(0, 47) + "..."
                else
                    text)
            }
            ?: ""
        // Create lookup element for autocomplete
        LookupElementBuilder
            .createWithSmartPointer("$quote$completionText$quote", element)
            .withLookupStrings(
                listOf(
                    completionText,
                    completionText.toLowerCase(),
                    completionText.toLowerCase().replace(" ", "").replace("_", ""),
                    value.name.split("::").first(),
                    value.name.toLowerCase().split("::").first()
                )
            )
            .withPresentableText("\"$completionText\"")
            .withTailText(description, true)
            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
    }
    resultSet.addAllElements(elements)
    addUserDefinedNamedVarsOfType(
        resultSet,
        project,
        GAME,
        expandedSearch,
        element,
        quote
    )
}

private fun addUserDefinedNamedVarsOfType(
    resultSet: CompletionResultSet,
    project: Project,
    type: CaosScriptNamedGameVarType,
    expandedSearch: Boolean,
    element: PsiElement,
    quote: String
) {
    addUserDefinedNamedVarsOfType(
        resultSet,
        project,
        listOf(type),
        expandedSearch,
        element,
        quote
    )
}

private fun addUserDefinedNamedVarsOfType(
    resultSet: CompletionResultSet,
    project: Project,
    types: List<CaosScriptNamedGameVarType>,
    expandedSearch: Boolean,
    element: PsiElement,
    quote: String
) {

    if (DumbService.isDumb(project))
        return
    // If index is built, get user defined named variables
    // Get module of containing file
    val module = element.module

    // Get search scope
    // If expanded search is false, only get files in the parent directory
    val searchScope = if (!expandedSearch) {
        val parentDirectory = element.psiDirectory
        parentDirectory?.let {
            GlobalSearchScopes.directoryScope(it, true)
        } ?: parentDirectory?.resolveScope
    } else
        module?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)
    if (searchScope == null) {
        return
    }
    val vars = CaosScriptNamedGameVarIndex.instance.getAllInScope(project, searchScope)
        .filter { gameVar: CaosScriptNamedGameVar ->
            gameVar.parent is CaosScriptLvalue &&
                    gameVar.varType in types &&
                    gameVar.rvalue?.text?.startsWith('"') == true &&
                    !PsiTreeUtil.isAncestor(gameVar, element, false)
        }
        .map { gameVar ->
            gameVar.name
        }
        .distinct()
        .map {
            LookupElementBuilder
                .createWithSmartPointer("$quote$it$quote", element)
                .withLookupStrings(
                    listOf(
                        "$quote$it$quote",
                        it,
                        it.toLowerCase(),
                        it.toLowerCase().replace(" ", "").replace("-", "").replace("_", "")
                    )
                )
                .withPresentableText("\"$it\"")
                .withTailText("  - unfiltered", true)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        }
    resultSet.addAllElements(vars)
}