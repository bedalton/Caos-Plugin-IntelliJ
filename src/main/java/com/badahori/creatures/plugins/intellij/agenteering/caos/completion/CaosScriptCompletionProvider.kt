package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.VARIANT_OLD
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
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
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
                ?: return
        val variant = caosFile.variant
                ?: return

        val text = element.textWithoutCompletionIdString
        if (text.isNotEmpty() && text.replace(IS_NUMBER, "").isEmpty()) {
            resultSet.stopHere()
            return
        }
        val previous = element.previous?.text

        // If previous is not whitespace and not square or double quote symbol, return
        if (previous != "[" && previous != "\"" && previous.nullIfEmpty()?.let { !WHITESPACE.matches(it) }.orFalse()) {
            resultSet.stopHere()
            return
        }

        // Add equality expression completions for known types
        (element.getSelfOrParentOfType(CaosScriptRvalue::class.java))?.let { expression ->
            // If has parent RValue, should continue with normal completion
            // Else use special EQ expression completion
            expression.getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.let { equalityExpression ->
                CaosScriptValuesListValuesCompletionProvider.addEqualityExpressionCompletions(variant, resultSet, equalityExpression, expression)
                addCommandCompletions(resultSet, variant, CaosCommandType.RVALUE, element)
                resultSet.stopHere()
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
        val type = parent.getEnclosingCommandType()
        // If has parent of value, add values list completion if needed
        // ie. Chemicals by name, or file names
        (element.getSelfOrParentOfType(CaosScriptArgument::class.java))?.let {
            CaosScriptValuesListValuesCompletionProvider.addParameterTypeDefValueCompletions(resultSet, it)
        }

        // If token is expected, return
        // Token completions will have been added with CaosScriptValuesListValuesCompletionProvider#addParameterTypeDefValueCompletions,
        // called in the command previously
        if (element.hasParentOfType(CaosScriptTokenRvalue::class.java)) {
            resultSet.stopHere()
            return
        }

        addCommandCompletions(resultSet, variant, type, element)
        // If type is Lvalue or Rvalue, add variable name completions
        if (type != CaosCommandType.COMMAND)
            addVariableCompletions(variant, element, resultSet)
    }

    private fun addCommandCompletions(resultSet: CompletionResultSet, variant: CaosVariant, type: CaosCommandType, element: PsiElement) {
        val case = element.case
        val allowUppercase = variant !in VARIANT_OLD
        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project)
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    if (it.commandName.toUpperCase() in SKIP_VAR_NAMES)
                        return@filter false
                    if (!it.isVariant(variant))
                        return@filter false
                    when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.CONTROL_STATEMENT -> it.isCommand
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                }
                .map {
                    ProgressIndicatorProvider.checkCanceled()
                    createCommandTokenLookupElement(allowUppercase, element, case, it.commandName, it.isCommand, it.parameterStructs, it.returnTypeString)
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
    private fun createCommandTokenLookupElement(allowUppercase: Boolean, element: PsiElement, case: Case, commandIn: String, isCommand: Boolean, parameters: List<CaosDefParameterStruct>, returnType: String, prefixIn: String? = null): LookupElementBuilder {
        // Tail text for command display element
        val tailText = if (isCommand)
            "command"
        else
            returnType
        // If an icon is possible, use it here
        val icon = if (isCommand)
            CaosScriptIcons.COMMAND
        else
            CaosScriptIcons.RVALUE

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
                .withTailText("($tailText)")
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
        var matches = CaosDefCommandElementsByNameIndex.Instance[commandString, element.project]
                .filter { variant in it.variants }
        matches = when (parent) {
            is CaosScriptCommandCall -> matches.filter { it.isCommand }
            is CaosScriptRvalue -> matches.filter { it.isRvalue }
            is CaosScriptLvalue -> matches.filter { it.isLvalue }
            else -> return false
        }
        if (matches.isEmpty())
            return false
        return matches.all { it.parameterStructs.size > 0 }
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

}

@Suppress("SpellCheckingInspection")
private const val ideaRulezzTrimFromEndLength = CompletionUtilCore.DUMMY_IDENTIFIER.length - 1

/**
 * Gets element text minus the weird IntelliJ completion string appended to the end
 */
private val PsiElement.textWithoutCompletionIdString get() = text.substringFromEnd(0, ideaRulezzTrimFromEndLength)