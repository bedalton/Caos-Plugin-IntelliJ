package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isNotNullOrBlank
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptRValueFoldingBuilder : FoldingBuilderEx() {

    override fun getPlaceholderText(node: ASTNode): String? {
        (node.psi as? CaosScriptExpression)?.let {
            return getExpressionFoldingText(it)
        }
        (node.psi as? CaosScriptEventNumberElement)?.let {

        }
        return null
    }

    private fun getEventName(element: CaosScriptEventNumberElement):String? {
        val eventElement = element as? CaosScriptEventNumberElement
                ?: return null
        val variant = element.containingCaosFile?.variant
                ?: return null
        val typeList = CaosDefValuesListDefinitionElementsByNameIndex
                .Instance["EventNumbers", element.project]
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.containingCaosDefFile.isVariant(variant)
                }
                .firstOrNull()
                ?: return null
        return typeList.getValueForKey(eventElement.text)?.value
    }

    private fun getExpressionFoldingText(expression: CaosScriptExpression): String? {
        return expression.getValuesListValue()?.value
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_ListValue")
        // Get a collection of the literal expressions in the document below root
        val literalExpressions = PsiTreeUtil.findChildrenOfType(root, CaosScriptExpression::class.java)
        return literalExpressions.filter {
            ProgressIndicatorProvider.checkCanceled()
            getExpressionFoldingText(it).isNotNullOrBlank()
        }.map {
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