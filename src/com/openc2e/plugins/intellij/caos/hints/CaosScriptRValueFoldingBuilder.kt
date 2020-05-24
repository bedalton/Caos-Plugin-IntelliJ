package com.openc2e.plugins.intellij.caos.hints

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank

class CaosScriptRValueFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun getPlaceholderText(node: ASTNode): String? {
        (node.psi as? CaosScriptExpression)?.let {
            return getExpressionFoldingText(it)
        }
        return null
    }

    private fun getExpressionFoldingText(expression: CaosScriptExpression): String? {
        // Lists values can only be for expressions of string or int
        if (!(expression.isString || expression.isInt)) {
            return null
        }
        (expression.parent?.parent as? CaosScriptExpectsValueOfType)?.let {
            return getCommandPlaceholderText(it)
        }

        expression.getParentOfType(CaosScriptEqualityExpression::class.java)?.let {
            return getEqualityExpressionFoldingText(it, expression)
        }
        return null
    }

    private fun getEqualityExpressionFoldingText(equalityExpression: CaosScriptEqualityExpression, expression: CaosScriptExpression): String? {
        val value = expression.intValue?.let { "$it" } ?: expression.stringValue ?: return null
        val other = equalityExpression.expressionList.let {
            when (it.size) {
                0, 1 -> return null
                2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
                else -> {
                    LOGGER.severe("Equality operator expects exactly TWO expressions")
                    return null
                }
            }
        } ?: return null
        val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
        if (token == null) {
            LOGGER.info("Failed to find rvaluePrime in equality expressions '${other.text}'")
            return null
        }
        val reference = token
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
        if (reference == null) {
            LOGGER.info("Failed to resolve reference to ${token.text} in equality expression")
            return null
        }
        val typeDef = reference
                .docComment
                ?.returnTypeStruct
                ?.type
                ?.typedef
        if (typeDef == null) {
            LOGGER.info("Equality expressions type is null in typedef for command ${token.text}")
            return null
        }
        val variant = expression.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        return getListValue(variant, typeDef, expression.project, value)
    }

    private fun getListValue(variant: String, listName: String, project: Project, key: String): String? {
        val list = CaosDefTypeDefinitionElementsByNameIndex
                .Instance[listName, project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
        if (list == null) {
            LOGGER.info("Failed to find list definition for name: $listName")
            return null
        }
        val value = list.keys.firstOrNull { it.key == key }
        if (value == null) {
            LOGGER.info("Failed to find value in list $listName for key $key")
        }
        return value?.value
    }

    private fun getCommandPlaceholderText(valueOfType: CaosScriptExpectsValueOfType): String? {
        val containingCommand = valueOfType.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return null
        val index = valueOfType.index
        val reference = containingCommand
                .commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null
        val parameterStruct = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(index)
        if (parameterStruct == null) {
            LOGGER.info("Parameter struct is null for argument $index in command ${containingCommand.commandString}")
            return null
        }
        val typeDef = parameterStruct
                .type
                .typedef
        if (typeDef == null) {
            LOGGER.info("RValue type is null in typedef for command ${containingCommand.commandString}. Parameter Data: $parameterStruct")
            return null
        }
        val variant = valueOfType.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        return getListValue(variant, typeDef, valueOfType.project, valueOfType.text)
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_ListValue")
        // Get a collection of the literal expressions in the document below root
        val literalExpressions = PsiTreeUtil.findChildrenOfType(root, CaosScriptExpression::class.java)
        return literalExpressions.filter {
            getExpressionFoldingText(it).isNotNullOrBlank()
        }.map {
            LOGGER.info("Creating folding region for ${it.text}")
            FoldingDescriptor(it.node, it.textRange, group)
        }.toTypedArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

    companion object {
        private const val DEFAULT_TEXT = "..."
    }

}