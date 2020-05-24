package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
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
        (node.psi as? CaosScriptEventNumberElement)?.let {

        }
        return null
    }

    private fun getEventName(element:CaosScriptEventNumberElement):String? {
        val eventElement = element as? CaosScriptEventNumberElement
                ?: return null
        val variant = element.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        val typeList = CaosDefTypeDefinitionElementsByNameIndex
                .Instance["EventNumbers", element.project]
                .filter {
                    it.containingCaosDefFile.isVariant(variant)
                }
                .firstOrNull()
                ?: return null
        return typeList.getValueForKey(eventElement.text)?.value
    }

    private fun getExpressionFoldingText(expression: CaosScriptExpression): String? {
        return expression.getTypeDefValue()?.value
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