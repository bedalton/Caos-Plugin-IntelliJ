package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.Case
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isNotNullOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import icons.CaosScriptIcons

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val IS_NUMBER = "[0-9]+".toRegex()
    private val WHITESPACE = "\\s+".toRegex()
    private val SKIP_VAR_NAMES = listOf("VARX", "OBVX", "MVXX", "OVXX", "VAXX")
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
                ?: return
        val variant = caosFile.variant

        val text = element.textWithoutCompletionIdString
        if (IS_NUMBER.matches(text)) {
            resultSet.stopHere()
            return
        }

        if (!WHITESPACE.matches(element.previous?.text ?: "")) {
            resultSet.stopHere()
            return
        }

        val previous = element.getPreviousNonEmptySibling(true)
        if (IS_NUMBER.matches( element.previous?.text ?:"")) {
            resultSet.stopHere()
            return
        }

        if (previous?.text?.toUpperCase() == "CLAS") {
            val builderElement = LookupElementBuilder
                    .create("")
                    .withPresentableText(GENERATE_CLAS_LOOKUP_STRING)
                    .withInsertHandler(GenerateClasIntegerAction)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        if (previous?.text?.toUpperCase() == "DDE: PICT") {
            val builderElement = LookupElementBuilder
                    .create("")
                    .withPresentableText(GENERATE_DDE_PICT_LOOKUP_STRING)
                    .withInsertHandler(GeneratePictDimensionsAction)
            resultSet.addElement(builderElement)
            resultSet.stopHere()
            return
        }

        if (element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCGsub::class.java)) {
            addSubroutineNames(element, resultSet)
            resultSet.stopHere()
            return
        }

        var parent: PsiElement? = element
        while (parent != null && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLvalue && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIncomplete) {
            parent = parent.parent
        }
        if (parent == null) {
            resultSet.stopHere()
            return
        }
        val type = parent.getEnclosingCommandType()
        val case = element.case
        val allowUppercase = variant !in listOf(CaosVariant.C1, CaosVariant.C2)
        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project)
                .filter {
                    if (it.commandName.toUpperCase() in SKIP_VAR_NAMES)
                        return@filter false
                    if (!it.isVariant(variant))
                        return@filter false
                    when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                }
                .map {
                    createCommandTokenLookupElement(allowUppercase, element, case, it.commandName, it.isCommand, it.returnTypeString)
                }
        resultSet.addAllElements(singleCommands)
        if (type != CaosCommandType.COMMAND)
            addVariableCompletions(variant, element, resultSet)
        (element.getParentOfType(CaosScriptExpectsValueOfType::class.java))?.let {
            CaosScriptTypeDefValueCompletionProvider.addParameterTypeDefValueCompletions(resultSet, it)
        }
        (element.getParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression::class.java))?.let { expression ->
            val equalityExpression = expression.getParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpression::class.java)
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

    private fun createCommandTokenLookupElement(allowUppercase: Boolean, element: PsiElement, case: Case, commandIn: String, isCommand: Boolean, returnType: String, prefixIn: String? = null): LookupElementBuilder {
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
        if (needsSpaceAfter(element, command))
            builder = builder.withInsertHandler(SpaceAfterInsertHandler)
        if (prefix.isNotNullOrBlank()) {
            builder = builder.withPresentableText("$prefix $command")
        }
        return builder
    }

    private fun needsSpaceAfter(element: PsiElement, commandString: String): Boolean {
        var parent: PsiElement? = element.parent
        while (parent != null && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLvalue && parent !is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall) {
            parent = parent.parent
        }
        if (parent == null)
            return false
        val variant = (element.containingFile as? CaosScriptFile).variant
        var matches = CaosDefCommandElementsByNameIndex.Instance[commandString, element.project]
                .filter { variant in it.variants }
        matches = when (parent) {
            is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall -> matches.filter { it.isCommand }
            is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue -> matches.filter { it.isRvalue }
            is com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLvalue -> matches.filter { it.isLvalue }
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
