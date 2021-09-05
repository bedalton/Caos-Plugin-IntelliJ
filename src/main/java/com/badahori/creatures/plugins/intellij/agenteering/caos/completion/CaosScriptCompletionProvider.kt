package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import icons.CaosScriptIcons

/**
 * Provides completions for CAOS script
 * Adds completions for commands, variables, subroutine names, and known values.
 */
object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val IS_NUMBER = "^[0-9]+".toRegex()
    private val WHITESPACE = "^\\s+$".toRegex()
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

        val text = element.textWithoutCompletionIdString
        if (text.isNotEmpty() && IS_NUMBER.matches(text) && !parameters.isExtendedCompletion) {
            resultSet.stopHere()
            return
        }
        val caos2Block = element.getParentOfType(CaosScriptCaos2Block::class.java)
        val caosElement = element.getSelfOrParentOfType(CaosScriptCompositeElement::class.java) ?: element

        if (caos2Block != null) {
            val isCaos2Cob = caos2Block.isCaos2Cob
            val directory = caosFile.virtualFile.parent
            val previous = caosElement.getPreviousNonEmptySibling(false)
            val previousText = (previous as? CaosScriptCaos2CommandName)?.text
            if (previousText != null || previous?.text == "*#") {
                val case = text.nullIfEmpty()?.case ?: Case.CAPITAL_FIRST
                if (isCaos2Cob) {
                    addCaos2CobTagCompletions(resultSet, variant, caosFile, text, previousText)
                    resultSet.addElement(LookupElementBuilder.create("Link".matchCase(case)))
                }
                CobCommand.getCommands(variant).forEach {
                    resultSet.addElement(
                        LookupElementBuilder.create(it.keyStrings.first().matchCase(case))
                            .withInsertHandler(SpaceAfterInsertHandler)
                    )
                }
            }
            val stringValue = caosElement.getSelfOrParentOfType(CaosScriptCaos2Value::class.java)?.text?.trim() ?: ""
            val openChar = if (stringValue.startsWith("\"") || stringValue.startsWith("'")) "" else "\""
            val closeChar = if (stringValue.contains('\n') || stringValue.contains('\r'))
                if (stringValue.startsWith('\''))
                    "'"
                else
                    "\""
            else if (stringValue.endsWith("\"") && stringValue.startsWith("\"") && stringValue.length > 1)
                ""
            else if (stringValue.startsWith("'") && stringValue.endsWith("'") && stringValue.length > 1)
                ""
            else if (stringValue.startsWith("'"))
                "'"
            else
                "\""
            if (isCaos2Cob) {
                // Only need to add specific tags if in Caos2Cob, as Caos2Pray tags can be anything
                caosElement.getSelfOrParentOfType(CaosScriptCaos2Tag::class.java)?.let {
                    val tag = CobTag.fromString(it.tagName)
                    addCaos2CobFileNameCompletions(resultSet, directory, tag, openChar, closeChar)
                }
            }
            caosElement.getSelfOrParentOfType(CaosScriptCaos2Command::class.java)?.let {
                val command = CobCommand.fromString(it.commandName)
                addCaos2CobFileNameCompletions(resultSet, directory, command, openChar, closeChar, !isCaos2Cob)
            }
            resultSet.stopHere()
            return
        }
        val previous = element.previous?.text

        // If previous is not whitespace and not square or double quote symbol, return
        if (previous != "[" && previous != "\"" && previous.nullIfEmpty()?.let { !WHITESPACE.matches(it) }.orFalse()) {
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
            // If has parent RValue, should continue with normal completion
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
//        LOGGER.info("previousTokenText: <$previousTokenText>; TokenType: ${previousToken.elementType}")

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
     * ie. VAR0 -> VAR9 on C1 or VA00-VA99 on C2+
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
    ): LookupElementBuilder {
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

        // If prefix, match case
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
            resultSet.addElement(builder)
        }
    }

    private fun addCaos2CobFileNameCompletions(
        resultSet: CompletionResultSet,
        directory: VirtualFile,
        tag: CobCommand?,
        openChar: String,
        closeChar: String,
        anything: Boolean
    ) {
        val files = when {
            tag?.cosFiles.orFalse() -> {
                VirtualFileUtil.childrenWithExtensions(directory, true, "cos", "caos")
            }
            tag != null -> {
                if (anything)
                    VirtualFileUtil.collectChildFiles(directory, true)
                else
                    VirtualFileUtil.childrenWithExtensions(directory, true, "s16", "c16", "spr")
            }
            else -> return
        }
        val parentPathLength = directory.path.length + 1
        for (file in files) {
            val relativePath = file.path.substring(parentPathLength)
            resultSet.addElement(LookupElementBuilder.create(openChar + relativePath + closeChar))
        }

    }

    private fun addCaos2CobFileNameCompletions(
        resultSet: CompletionResultSet,
        directory: VirtualFile,
        tag: CobTag?,
        openChar: String,
        closeChar: String
    ) {

        // Get files of appropriate type
        val files = if (tag == CobTag.THUMBNAIL) {
            VirtualFileUtil.childrenWithExtensions(directory, true, "s16", "c16", "spr")
        } else {
            return
        }

        // Ensure there are matching files
        if (files.isEmpty())
            return

        // Get parent path length for sizing down the relative child paths
        val parentPathLength = directory.path.length + 1

        // Create insert handler for use by thumbnail tags to move the cursor between the [^] in sprite thumbnails
        val insertHandler = OffsetCursorInsertHandler(6) // ^].spr"

        // Loop through files and add lookup elements
        for (file in files) {
            val relativePath = file.path.substring(parentPathLength)
            resultSet.addElement(LookupElementBuilder.create(openChar + relativePath + closeChar))
            if (tag == CobTag.THUMBNAIL) {
                val fileParts = relativePath.split(".")
                if (fileParts.size < 2)
                    continue
                val basePath = fileParts.dropLast(1).joinToString(".")
                LookupElementBuilder.create(openChar + basePath + "[]." + fileParts.last() + closeChar)
                    .withLookupString(basePath + "[#]." + fileParts.last())
                    .withInsertHandler(insertHandler)
            }
        }
    }

    private fun addCaos2CobTagCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        caosFile: CaosScriptFile,
        textIn: String,
        previousText: String?
    ) {
        val existingTags: List<CobTag> = caosFile.prayTags.map { it.tag }.mapNotNull { tagString ->
            CobTag.fromString(tagString)
        }
        var tagsRaw = CobTag.getTags(variant).filterNot { tag -> tag in existingTags }
        var text = textIn
        if (previousText != null) {
            text = "$previousText $textIn"
            tagsRaw = tagsRaw.filter { it.keys.any { it.startsWith(previousText) } }
        }
        val case = previousText?.case ?: text.nullIfEmpty()?.case ?: Case.CAPITAL_FIRST
        val tags = tagsRaw.map { tag ->
            val keys = tag.keys
            keys.firstOrNull { it.startsWith(text) }?.let {
                return@map it
            }
            val sorted = keys.map { key ->
                Pair(text.levenshteinDistance(key), key)
            }.sortedBy { it.first }
            if (sorted.first().first > 8)
                keys.first()
            else
                sorted.first().second
        }.map { tagRaw ->
            val tag = if (previousText != null) {
                tagRaw.split(" ").last()
            } else {
                tagRaw
            }.matchCase(case)
            LookupElementBuilder.create(tag)
                .withInsertHandler(EqualSignInsertHandler)
        }
        resultSet.addAllElements(tags)
    }

}

@Suppress("SpellCheckingInspection")
private const val ideaRulezzTrimFromEndLength = CompletionUtilCore.DUMMY_IDENTIFIER.length - 1

/**
 * Gets element text minus the weird IntelliJ completion string appended to the end
 */
private val PsiElement.textWithoutCompletionIdString get() = text.substringFromEnd(0, ideaRulezzTrimFromEndLength)