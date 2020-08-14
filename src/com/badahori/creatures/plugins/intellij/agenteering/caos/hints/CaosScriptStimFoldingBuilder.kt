package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.isNumeric
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpectsAgent
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpectsValueOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.abs

class CaosScriptStimFoldingBuilder : FoldingBuilderEx() {

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
        val variant = commandCall.containingCaosFile?.variant
                ?: return null
        val resolved = commandCall.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null

        return when (variant) {
            CaosVariant.C1, CaosVariant.C2 -> foldC1ChemCall(commandCall, resolved)
            CaosVariant.CV, CaosVariant.C3, CaosVariant.DS -> foldC3ChemCall(commandCall, resolved)
            else -> null
        }
    }

    private fun foldC1ChemCall(commandCall: CaosScriptCommandCall, caosDef: CaosDefCommandDefElement) = foldChemCallGeneric(commandCall, caosDef)

    private fun foldC3ChemCall(commandCall: CaosScriptCommandCall, caosDef: CaosDefCommandDefElement) = foldChemCallGeneric(commandCall, caosDef)

    private fun foldChemCallGeneric(commandCall: CaosScriptCommandCall, commandDef: CaosDefCommandDefElement): String? {
        val arguments = commandCall.arguments
        val parameters = commandDef.parameterStructs
        val firstIndex = parameters
                .sortedBy { it.parameterNumber }
                .firstOrNull { parameter ->
                    firstFoldParameterRegex.matches(parameter.name)
                }
                ?.parameterNumber
                ?: return null
        if (firstIndex >= arguments.size)
            return null
        val chemParameters = arguments.subList(firstIndex, arguments.size)
        val variant = commandCall.containingCaosFile?.variant
                ?: return null
        if (chemParameters.size % 2 != 0) {
            return null
        }
        val numParameters = chemParameters.size / 2
        val stringBuilder = StringBuilder()
        val isStim = numParameters == 1 && STIM.matches(commandCall.commandString)
        for (i in 0 until numParameters) {
            val pos = i * 2
            val argument = chemParameters[pos]
            val expression = (argument as? CaosScriptExpectsValueOfType)
                    ?.rvalue
                    ?.expression
            //Ignore blank input
            if (expression?.text == "255")
                continue
            // Get type def value.
            val value = expression?.getValuesListValue()?.value
                    ?: argument.text
                    ?: continue
            val amount = chemParameters[pos + 1].toCaosVar()
            formatChemAmount(variant, stringBuilder, value, amount, isStim)
            stringBuilder.append(value)
        }
        return stringBuilder.toString().trim(' ', ',').nullIfEmpty()
    }

    private fun formatChemAmount(variant: CaosVariant, stringBuilder: StringBuilder, value:String, amountVar:CaosVar, isStim:Boolean = false) {

        val amount = if (amountVar is CaosVar.CaosLiteral.CaosInt)
            amountVar.value * 1.0f
        else if (amountVar is CaosVar.CaosLiteral.CaosFloat)
            amountVar.value
        else
            0.0f

        if (isStim) {
            stringBuilder.append(value).append(" * ").append(amount)
            return
        }
        if (abs(amount) < 0.0003)
            return
        stringBuilder.append(", ")
        if (amount > 0)
            stringBuilder.append("+")
        stringBuilder.append(amount).append(" ").append(value)
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
                    ProgressIndicatorProvider.checkCanceled()
                    getCommandCallFoldingRegion(it, group)
                }
                .toTypedArray()
    }

    private fun getCommandCallFoldingRegion(commandCall: CaosScriptCommandCall, group: FoldingGroup): FoldingDescriptor? {
        if (!shouldFold(commandCall))
            return null
        return getFoldingDescriptor(commandCall, group)
    }

    private fun getFoldingDescriptor(commandCall: CaosScriptCommandCall, group: FoldingGroup): FoldingDescriptor? {
        if (shouldFoldAll.matches(commandCall.commandString))
            return FoldingDescriptor(commandCall.node, commandCall.textRange, group)
        val arguments = commandCall.arguments
        val firstArgument = if (arguments.firstOrNull() is CaosScriptExpectsAgent)
            arguments.getOrNull(1)
        else
            arguments.firstOrNull()
        val start = firstArgument
                ?.startOffset
                ?: return null
        val end = arguments.lastOrNull()?.endOffset
                ?: return null
        return FoldingDescriptor(commandCall.node, TextRange.create(start, end), group)
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

    private fun shouldFold(commandCall: CaosScriptCommandCall): Boolean {
        return shouldFold.matches(commandCall.commandString) && commandCall.arguments.filter { it.toCaosVar().isNumeric }.size > 1
    }

    companion object {
        private val STIM = "[Ss][Tt][Ii][Mm]([ ][^ ]{4})*".toRegex()
        private val shouldFoldAll = "([Dd][Rr][Ii][Vv]|[Cc][Hh][Ee][Mm])".toRegex()
        private val shouldFold = "([Ss][Tt][Ii][Mm]|[Dd][Rr][Ii][Vv]|[Ss][Ww][Aa][Yy]|[Cc][Hh][Ee][Mm])([ ][^ ]{4})*".toRegex()
        private val firstFoldParameterRegex = "([Dd][Rr][Ii][Vv]([Ee])?)|([Cc][Hh][Ee][Mm]([Ii][Cc][Aa][Ll][Ss]?)?)|([Ss][Tt][Ii][Mm]([Uu][Ll][Uu][Ss])?)[0-9]*".toRegex()
    }

}