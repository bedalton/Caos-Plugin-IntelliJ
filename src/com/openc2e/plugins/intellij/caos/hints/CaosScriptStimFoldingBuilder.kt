package com.openc2e.plugins.intellij.caos.hints

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandCall
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpectsValueOfType
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getParentOfType
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty

class CaosScriptStimFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun getPlaceholderText(node: ASTNode): String? {
        if (DumbService.isDumb(node.psi.project))
            return null
        (node.psi as? CaosScriptCommandCall)?.let {
            if (shouldFold(it))
                return getStimFold(it)
        }
        return null
    }

    private fun getStimFold(commandCall: CaosScriptCommandCall): String? {
        val variant = commandCall.containingCaosFile.variant
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
                .sortedBy { it.parameterNumber }
                .firstOrNull { parameter ->
                    parameter.name.toLowerCase().startsWith("chem")
                }
                ?.parameterNumber
                ?: return null
        val chemParameters = arguments.subList(firstIndex, arguments.size)
        if (chemParameters.size % 2 != 0) {
            LOGGER.info("Cannot fold STIM list, chemicals/amounts unequal paramterSize: ${chemParameters.joinToString { it.text }}")
            return null
        }
        val numParameters = chemParameters.size / 2
        val stringBuilder = StringBuilder()
        for (i in 0..numParameters) {
            val pos = i * 2
            val expression = (arguments as? CaosScriptExpectsValueOfType)
                    ?.rvalue
                    ?.expression
            //Ignore blank input
            if (expression?.text == "255")
                continue
            // Get type def value.
            val value = expression?.getTypeDefValue()?.value
                    ?: expression?.text?.let { "Chemical $it" }
            stringBuilder.append(", +")
            val amount = arguments[pos + 1].toCaosVar()
            if (amount is CaosVar.CaosLiteral.CaosInt)
                stringBuilder.append(amount.value).append(" ")
            else if (amount is CaosVar.CaosLiteral.CaosFloat)
                stringBuilder.append(amount.value).append(" ")
            stringBuilder.append(value)
        }
        return stringBuilder.toString().trim(' ',',').nullIfEmpty()
    }

    private fun foldC3ChemCall(commandCall: CaosScriptCommandCall, commandDef: CaosDefCommandDefElement): String? {
        // todo implement CV+ folding for STIM calls
        return null
    }

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_STIM_FOLDING")
        // Get a collection of the literal expressions in the document below root
        val children = PsiTreeUtil.findChildrenOfType(root, CaosScriptCommandCall::class.java)
        return children
                .filter {
                    shouldFold(it)
                }
                .mapNotNull {
                    getCommandCallFoldingRegion(it, group)
                }
                .toTypedArray()
    }

    private fun getCommandCallFoldingRegion(commandCall: CaosScriptCommandCall, group:FoldingGroup): FoldingDescriptor? {
        if (!shouldFold(commandCall))
            return null
        val variant = commandCall.containingCaosFile.variant
        return when (variant) {
            "C1", "C2" -> getC1StimFoldingDescriptor(commandCall, group)
            else -> {
                // TODO implement CV+ folding
                null
            }

        }
    }

    private fun getC1StimFoldingDescriptor(commandCall: CaosScriptCommandCall, group:FoldingGroup): FoldingDescriptor? {
        val resolved = commandCall.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null
        val arguments = commandCall.arguments
        val parameters = resolved.parameterStructs
        val firstIndex = parameters
                .sortedBy { it.parameterNumber }
                .firstOrNull {
                    it.name.toLowerCase().let {
                        it.startsWith("sign") || it.startsWith("chem")
                    }
                }
                ?.parameterNumber
                ?: return null
        val start = arguments.getOrNull(firstIndex)
                ?.startOffset
                ?: return null
        val end = arguments.lastOrNull()?.endOffset
                ?: return null
        return FoldingDescriptor(commandCall.node, TextRange.create(start, end), group)
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return getPlaceholderText(node).isNotNullOrBlank()
    }

    private fun shouldFold(commandCall: CaosScriptCommandCall): Boolean {
        return commandCall.commandString.toUpperCase().startsWith("STIM")
    }

}