package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.VARIANT_OLD
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
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
import java.util.logging.Logger

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
        if (previous != "[" && previous != "\"" && previous.nullIfEmpty()?.let { !WHITESPACE.matches(it) }.orFalse()) {
            resultSet.stopHere()
            return
        }

        if (IS_NUMBER.matches( previous ?: "")) {
            resultSet.stopHere()
            return
        }

        val previousToken = element.getPreviousNonEmptySibling(false)?.text?.toUpperCase()
        if (previousToken == "CLAS") {
            val builderElement = LookupElementBuilder
                    .create("")
                    .withPresentableText(GENERATE_CLAS_LOOKUP_STRING)
                    .withInsertHandler(GenerateClasIntegerAction)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        if (previousToken == "DDE: PICT") {
            val builderElement = LookupElementBuilder
                    .create("")
                    .withPresentableText(GENERATE_DDE_PICT_LOOKUP_STRING)
                    .withInsertHandler(GeneratePictDimensionsAction)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        if (element.hasParentOfType(CaosScriptCGsub::class.java)) {
            addSubroutineNames(element, resultSet)
            resultSet.stopHere()
            return
        }

        var parent: PsiElement? = element
        while (parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
            parent = parent.parent
        }
        if (parent == null) {
            resultSet.stopHere()
            return
        }
        val type = parent.getEnclosingCommandType()
        val case = element.case
        val allowUppercase = variant !in VARIANT_OLD
        (element.getSelfOrParentOfType(CaosScriptExpectsValueOfType::class.java))?.let {
            CaosScriptTypeDefValueCompletionProvider.addParameterTypeDefValueCompletions(resultSet, it)
        }

        if (element.hasParentOfType(CaosScriptExpectsToken::class.java)) {
            resultSet.stopHere()
            return
        }
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
        if (type != CaosCommandType.COMMAND)
            addVariableCompletions(variant, element, resultSet)

        (element.getParentOfType(CaosScriptExpression::class.java))?.let { expression ->
            val equalityExpression = expression.getParentOfType(CaosScriptEqualityExpression::class.java)
            if (equalityExpression != null) {
                CaosScriptTypeDefValueCompletionProvider.addEqualityExpressionCompletions(resultSet, equalityExpression, expression)
            }
        }
    }

    private fun addVariableCompletions(variant: CaosVariant, element: PsiElement, resultSet: CompletionResultSet) {
        if (variant == CaosVariant.C1) {
            (0..2).forEach {
                resultSet.addElement(LookupElementBuilder.create("obv$it"))
            }
            (0..9).forEach {
                resultSet.addElement(LookupElementBuilder.create("var$it"))
            }
        }
        else if (variant == CaosVariant.C2) {
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

    private fun createCommandTokenLookupElement(allowUppercase: Boolean, element: PsiElement, case: Case, commandIn: String, isCommand: Boolean, parameters:List<CaosDefParameterStruct>, returnType: String, prefixIn: String? = null): LookupElementBuilder {
        val tailText = if (isCommand)
            "command"
        else
            returnType
        val icon = if (isCommand)
            CaosScriptIcons.COMMAND
        else
            CaosScriptIcons.RVALUE
        val command = if (case == Case.UPPER_CASE && allowUppercase)
            commandIn.toUpperCase()
        else
            commandIn.toLowerCase()
        val prefix = when {
            prefixIn == null -> null
            case == Case.UPPER_CASE -> prefixIn.toUpperCase()
            else -> prefixIn.toLowerCase()
        }
        var builder = LookupElementBuilder
                .create(command)
                .withTailText("($tailText)")
                .withIcon(icon)
        val needsSpace = needsSpaceAfter(element, command)
        if (parameters.size > 0) {
            builder = builder.withInsertHandler(CommandInsertHandler(command, parameters, needsSpace))
        } else if (needsSpace)
            builder = builder.withInsertHandler(SpaceAfterInsertHandler)
        if (prefix.isNotNullOrBlank()) {
            builder = builder.withPresentableText("$prefix $command")
        }
        return builder
    }

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

    private fun addSubroutineNames(element:PsiElement, resultSet: CompletionResultSet) {
        val project = element.project
        val file = element.containingFile
        val scope = GlobalSearchScope.everythingScope(project)
        val subroutines = CaosScriptSubroutineIndex.instance.getAllInScope(project, scope)
                .filter {
                    file.name == (it.originalElement?.containingFile ?: it.containingFile)?.name
                }
                .mapNotNull { it.name.nullIfEmpty() }
                .toSet()
        for(subr in subroutines) {
            val builder = LookupElementBuilder.create(subr)
            resultSet.addElement(builder)
        }
    }

}

private val PsiElement.textWithoutCompletionIdString get() = text.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER)
