@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalStdlibApi::class)

package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import bedalton.creatures.common.util.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.psiDirectory
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayCommand.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.commandStringUpper
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import icons.CaosScriptIcons

/**
 * Provides completions for CAOS script
 * Adds completions for commands, variables, subroutine names, and known values.
 */
@ExperimentalStdlibApi
object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>(), DumbAware {
    private val IS_NUMBER = "^\\d+".toRegex()
    private val WHITESPACE_ONLY = "^\\s+$".toRegex()

    private val SKIP_VAR_NAMES = listOf("VARX", "OBVX", "MVXX", "OVXX", "VAXX")

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet,
    ) {
        val element = parameters.position
        val nonEmpty = if (element is PsiWhiteSpace) {
            element.getPreviousNonEmptySibling(false) ?: element
        } else {
            element
        }
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
            ?: return
        val variant = caosFile.variant
            ?: return

        val project = element.project

        val text = element.textWithoutCompletionIdString
        ClassifierCompletion.completeClassifier(resultSet, parameters.isExtendedCompletion, element)
        if (text.isNotEmpty() && IS_NUMBER.matches(text) && !parameters.isExtendedCompletion) {
            resultSet.stopHere()
            return
        }

        CatalogueCompletionContributor.getCatalogueCompletions(resultSet, element)

        val caos2Block = nonEmpty.getParentOfType(CaosScriptCaos2Block::class.java)

        if (caos2Block != null) {
            // If in caos2 block, but is not CAOS2 child, return as no completion is needed
            val caos2Child = nonEmpty.getSelfOrParentOfType(PrayChildElement::class.java)

            if (caos2Child != null) {
                Caos2CompletionProvider.addCaos2Completion(
                    resultSet,
                    parameters.isExtendedCompletion,
                    variant,
                    caos2Block,
                    caos2Child
                )
            } else {
                val file = element.containingFile
                val case = text.stripSurroundingQuotes().replace(DUMMY_IDENTIFIER_TRIMMED, "").nullIfEmpty()?.case
                    ?: Case.CAPITAL_FIRST
                for (action in PrayCommand.getActionCommands()) {
                    val insertAction = LinkFilesInsertHandler(action, true)
                    if (!insertAction.isAvailable(file)) {
                        continue
                    }
                    val addMany = LookupElementBuilder
                        .create("${action.matchCase(case)} all files of type..")
                        .withInsertHandler(insertAction)
                        .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
                    resultSet.addElement(addMany)
                }
            }

            // No more completions should be added
            return
        }

        val previous = element.previous?.text

        if (
            element is CaosScriptStringText ||
            element.parent is CaosScriptStringText ||
            element.parent is CaosScriptQuoteStringLiteral ||
            element is CaosScriptQuoteStringLiteral ||
            element is CaosScriptRvalueLike
        ) {
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
        val previousTokenText = previousToken?.text?.uppercase()

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
        element: PsiElement,
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
        prefixIn: String? = null,
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
            commandIn.uppercase()
        else
            commandIn.lowercase()

        // If command has prefix then match case
        val prefix = when {
            prefixIn == null -> null
            case == Case.UPPER_CASE -> prefixIn.uppercase()
            else -> prefixIn.lowercase()
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
        val parentScript = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        val subroutines = PsiTreeUtil.collectElementsOfType(parentScript, CaosScriptSubroutineName::class.java)
            .mapNotNull { it.name.nullIfEmpty() }
            .toSet()
        for (subr in subroutines) {
            val builder = LookupElementBuilder.create(subr)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(builder)
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


@ExperimentalStdlibApi
private fun addStringCompletions(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    variant: CaosVariant,
    element: PsiElement,
) {

    val quoteStringElement = element as? CaosScriptQuoteStringLiteral
        ?: element.parent as? CaosScriptQuoteStringLiteral
        ?: element.parent?.parent as? CaosScriptQuoteStringLiteral
    val quoter = if (variant.isOld) {
        { text: String ->
            FileNameUtil.getFileNameWithoutExtension(text)
                ?: text
        }
    } else if (quoteStringElement != null) {
        quoter(quoteStringElement.text.replace(DUMMY_IDENTIFIER_TRIMMED, ""))
    } else {
        { aString ->
            aString
        }
    }
    val kind = StringStubKind.fromPsiElement(element)
        ?: return

    if (kind.isFile) {
        addFilenameStringCompletions(
            project,
            resultSet,
            expandedSearch,
            element,
            kind,
            quoter
        )
    } else if (variant.isNotOld) {
        // Only complete user strings on Quote string literals
        val quoteString = element as? CaosScriptQuoteStringLiteral
            ?: element.parent as? CaosScriptQuoteStringLiteral
            ?: element.parent?.parent as? CaosScriptQuoteStringLiteral
            ?: return
        addUserCreatedStringCompletions(
            project,
            resultSet,
            expandedSearch,
            variant,
            quoteStringElement ?: element,
            quoteString,
            kind,
            quoter
        )
    }

}


private fun addUserCreatedStringCompletions(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    variant: CaosVariant,
    element: PsiElement,
    quoteStringLiteral: CaosScriptQuoteStringLiteral,
    kind: StringStubKind,
    quoter: (String) -> String,
) {
    // Do not complete file names here
    if (kind.isFile) {
        return
    }

    if (variant.isOld) {
        return
    }

    val virtualFile = element.virtualFile
        ?: return
    val lock = if (expandedSearch && virtualFile.parent != null) {
        GlobalSearchScopes.directoryScope(project, virtualFile.parent, false)
    } else {
        GlobalSearchScope.fileScope(
            element.project,
            virtualFile
        )
    }
    val indexedStrings = if (!DumbService.isDumb(project)) {
        CaosScriptStringLiteralIndex
            .instance
            .getAllInScope(project, lock)
    } else if (expandedSearch) {
        // If index is dumb, just gather all sibling file's strings
        element.collectElementsOfTypeInParentChildren(
            CaosScriptQuoteStringLiteral::class.java,
            false
        )
    } else {
        emptyList()
    }


    // Get all strings within this file
    val inFileStrings = PsiTreeUtil.collectElementsOfType(
        element.containingFile,
        CaosScriptQuoteStringLiteral::class.java
    )

    val meta = quoteStringLiteral.meta
    val strings = (inFileStrings + indexedStrings)
        .distinct()
        .filter {
            if (it.stringStubKind == kind) {
                true
            } else if (kind != JOURNAL) {
                // If not journal, no meta to match
                true
            } else {
                // If kind is journal, it also needs to match location which is the meta
                val thisMeta = it.meta
                meta == -1 || thisMeta == -1 || meta == thisMeta
            }
        }
    val case = element.textWithoutCompletionIdString
        .case
    val lookupElements = strings.map {
        val completionText = it.stringValue
        LookupElementBuilder
            .createWithSmartPointer(quoter(completionText), element)
            .withLookupStrings(
                listOf(
                    completionText,
                    completionText.lowercase(),
                    completionText.matchCase(Case.CAPITAL_FIRST),
                    completionText.matchCase(case)
                )
            )
            .withPresentableText(completionText)
            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
    }
    resultSet.addAllElements(lookupElements)
}

private fun addFilenameStringCompletions(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    element: PsiElement,
    kind: StringStubKind,
    quoter: (String) -> String,
) {

    if (!kind.isFile) {
        return
    }

    val extensions = when (kind) {
        COS -> listOf("COS", "CAOS")
        S16 -> listOf("S16")
        C16 -> listOf("C16")
        C2E_SPRITE -> listOf("C16", "S16")
        WAV -> listOf("WAV")
        BLK -> listOf("BLK", "BACK")
        SPR -> listOf("SPR")
        MNG -> listOf("MNG", "MING")
        else -> return
    }

    val commandString = element.getParentOfType(CaosScriptCommandElement::class.java)
        ?.commandStringUpper

    val dropExtension = commandString != "JECT"

    addFileNameCompletionsForFileTypes(
        project,
        resultSet,
        expandedSearch,
        extensions,
        dropExtension,
        element,
        quoter
    )
}

private fun addFileNameCompletionsForFileTypes(
    project: Project,
    resultSet: CompletionResultSet,
    expandedSearch: Boolean,
    extensions: Collection<String>,
    dropExtension: Boolean,
    element: PsiElement,
    quoter: (String) -> String,
) {

    // Requires file index, so stop completing if index is not yet built
    if (project.isDisposed || DumbService.isDumb(project) || !element.isValid) {
        return
    }

    val containingFile = element.containingFile?.virtualFile
        ?: element.originalElement?.containingFile?.virtualFile
        ?: return

    // Get module of containing file
    val module = element.module

    // Get search scope
    // If expanded search is false, only get files in the parent directory
    val searchScope = if (!expandedSearch) {
        val parentDirectory = element.psiDirectory
        parentDirectory?.let {
            GlobalSearchScopes.directoryScope(it, true)
        }
    } else {
        module?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)
    }

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

    val roots = module?.rootManager?.contentRoots?.toList()
        ?: module?.myModuleFile?.toListOf()
        ?: project.projectFile?.toListOf()
    // Util method to get the relative path between a file and its parent module or directory
    val relativePath: (path: VirtualFile?) -> String? = path@{ path: VirtualFile? ->
        if (path == null) {
//            LOGGER.info("Path is null for virtual file")
            return@path null
        }
        val inModule = roots.isNullOrEmpty() || roots!!.any { parent -> VfsUtil.isAncestor(parent, path, true) }

        if (!inModule) {
//            LOGGER.info("File not in Roots: [${roots?.joinToString()}]")
            return@path null
        }
        VfsUtil.getRelativePath(path, containingFile)
    }

    // Get files as completion elements
    val files = extensions
        .flatMap { extension ->
            (search(extension) + search(extension.uppercase()))
                .distinctBy {
                    it.path
                }
        }
        .toSet()
        .map { file ->
            // Get the file name, dropping extension if necessary
            val completionText = if (dropExtension) {
                file.nameWithoutExtension
            } else {
                file.name
            }
            // Get location string for autocomplete tail-text
            val location = relativePath(file.parent)?.let { " in $it" } ?: "..."
            LookupElementBuilder
                .createWithSmartPointer(quoter(completionText), element)
                .withLookupString(completionText)
                .withPresentableText(file.name)
                .withTailText(location, true)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        }

    // Add all elements
    resultSet.addAllElements(files)

}

// TODO: See if the new generic string index function works just as well
//private fun addUserDefinedNamedVarsOfType(
//    resultSet: CompletionResultSet,
//    project: Project,
//    types: List<CaosScriptNamedGameVarType>,
//    expandedSearch: Boolean,
//    containerElement: PsiElement,
//    quoter: (String) -> String,
//) {
//
//    if (project.isDisposed || DumbService.isDumb(project))
//        return
//
//    val vars = getNamedGameVarNames(
//        project,
//        containerElement,
//        expandedSearch,
//        types,
//    )
//        .map {
//            LookupElementBuilder
//                .create(quoter(it))
//                .withPsiElement(containerElement)
//                .withLookupStrings(
//                    listOf(
//                        quoter(it),
//                        it,
//                        it.lowercase(),
//                        it.lowercase().replace(" ", "").replace("-", "").replace("_", "")
//                    )
//                )
//                .withPresentableText("\"$it\"")
//                .withTailText("  - unfiltered", true)
//                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
//        }
//    resultSet.addAllElements(vars)
//}


internal fun quoter(text: String, defaultQuote: Char = '"'): (text: String) -> String {
    val actualOpenQuote = text.trim().firstOrNull()?.let {
        if (it == '\'' || it == '"') {
            it
        } else {
            null
        }
    }
    val actualClosingQuote = text.trim().lastOrNull()?.let {
        if (it == '\'' || it == '"') {
            it
        } else {
            null
        }
    }
    val quote = actualOpenQuote ?: actualClosingQuote ?: defaultQuote
    val needsOpenQuote = actualOpenQuote != quote
    val needsCloseQuote = actualClosingQuote != quote
    return if (needsOpenQuote) {
        if (needsCloseQuote) {
            { aString ->
                quote + aString + quote
            }
        } else {
            { aString ->
                quote + aString
            }
        }
    } else if (needsCloseQuote) {
        { aString ->
            aString + quote
        }
    } else {
        { aString ->
            aString
        }
    }
}