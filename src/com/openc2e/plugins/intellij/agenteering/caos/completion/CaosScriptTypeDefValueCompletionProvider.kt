package com.openc2e.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.AddSpaceInsertHandler
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.openc2e.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.TypeDefEq
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.*
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptyNode
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.agenteering.caos.utils.matchCase

object CaosScriptTypeDefValueCompletionProvider {

    fun addParameterTypeDefValueCompletions(resultSet:CompletionResultSet, valueOfType: CaosScriptExpectsValueOfType) {
        val containingCommand = valueOfType.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return
        val index = valueOfType.index
        val reference = containingCommand
                .commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return
        val parameterStruct = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(index)
                ?: return
        val typeDef = parameterStruct
                .type
                .typedef
                ?: return

        val variant = valueOfType.containingCaosFile.variant
        addListValues(resultSet, variant, typeDef, valueOfType.project, valueOfType.text, reference.parameterStructs.size - 1 > index)
    }

    fun addEqualityExpressionCompletions(resultSet:CompletionResultSet, equalityExpression: CaosScriptEqualityExpression, expression: CaosScriptExpression) {
        val other = equalityExpression.expressionList.let {
            when (it.size) {
                0, 1 -> return
                2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
                else -> {
                    LOGGER.severe("Equality operator expects exactly TWO expressions")
                    return
                }
            }
        } ?: return
        val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: return
        val reference = token
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return
        val typeDef = reference
                .docComment
                ?.returnTypeStruct
                ?.type
                ?.typedef
                ?: return
        val variant = expression.containingCaosFile.variant
        addListValues(resultSet, variant, typeDef, expression.project, expression.text, expression.getPreviousNonEmptyNode(false) !is CaosScriptEqOp)
    }

    private fun addListValues(resultSet:CompletionResultSet, variant:CaosVariant, listName: String, project: Project, string:String, addSpace:Boolean) {
        val values = CaosDefTypeDefinitionElementsByNameIndex
                .Instance[listName, project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
                ?.keys
                ?.filter { it.equality == TypeDefEq.EQUAL }
                ?: return
        for(value in values) {
            var lookupElement = LookupElementBuilder
                    .create(value.key)
                    .withLookupString(value.value.matchCase(string))
                    .withPresentableText(value.key + ": " + value.value)
                    .withTailText(value.description)
            if (addSpace) {
                lookupElement = lookupElement
                        .withInsertHandler(AddSpaceInsertHandler(true))
            }
            resultSet.addElement(lookupElement)
        }
    }

}