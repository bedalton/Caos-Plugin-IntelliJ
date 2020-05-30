package com.openc2e.plugins.intellij.caos.hints

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.deducer.CaosNumber
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getParentOfType
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank

class CaosScriptFoldChemList : FoldingBuilderEx(), DumbAware {

    override fun getPlaceholderText(node: ASTNode): String? {
        (node.psi as? CaosScriptCommandCall)?.let {
            return getExpressionFoldingText(it)
        }
        return null
    }

    private fun getExpressionFoldingText(commandCall: CaosScriptCommandCall): String? {
        val command = commandCall.commandString.toUpperCase()
        val variant = commandCall.containingCaosFile.variant
        when {
            command.startsWith("STIM") -> foldStim(variant, commandCall)
        }
        return expression.getTypeDefValue()?.value
    }

    private fun foldStim(variant: String, commandCall: CaosScriptCommandCall): String? {
        val resolved = commandCall.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null

        return when (variant) {
            "C1", "C2" -> foldC1ChemCall(commandCall, resolved)
            "CV", "C3", "DS" -> foldC3ChemCall(commandCall, resolved)
            else -> null
        }
    }

    private fun foldC1ChemCall(commandCall: CaosScriptCommandCall, commandDef: CaosDefCommandDefElement): String? {
        val arguments = commandCall.arguments
        val parameters = commandDef.parameterStructs
        val firstIndex = parameters
                .firstOrNull {
                    it.name.toLowerCase().startsWith("chem")
                }
                ?.parameterNumber
                ?: return null
        val start = arguments.getOrNull(firstIndex)
                ?.startOffset
                ?: return null
        val end = arguments.lastOrNull()?.endOffset
                ?: return null
        val chemParameters = arguments.subList(firstIndex, arguments.lastIndex)
        if (chemParameters.size % 2 != 0) {
            LOGGER.info("Cannot fold STIM list, chemicals/amounts unequal")
            return null
        }
        val numParameters = chemParameters.size / 2
        val stringBuilder = StringBuilder()
        for(i in 0..numParameters) {
            stringBuilder.append("+")
            val amount = arguments[i+1].toCaosVar()
            if (amount is CaosVar.CaosLiteral.CaosInt)
                stringBuilder.
            if (amount.ca)
            arguments.
        }
    }

    private fun foldC3ChemCall(commandCall: CaosScriptCommandCall, commandDef: CaosDefCommandDefElement): String? {

    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_ListValue")
        // Get a collection of the literal expressions in the document below root
        val literalExpressions = PsiTreeUtil.findChildrenOfType(root, CaosScriptExpression::class.java)
        return literalExpressions.filter {
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