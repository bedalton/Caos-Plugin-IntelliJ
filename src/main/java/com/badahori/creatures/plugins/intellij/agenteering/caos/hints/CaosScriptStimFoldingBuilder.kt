package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.isNumberType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
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
import kotlin.math.roundToInt

class CaosScriptStimFoldingBuilder : FoldingBuilderEx() {

    /**
     * Gets command call placeholder text for folding if it should be folded
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        if (DumbService.isDumb(node.psi.project))
            return null
        (node.psi as? CaosScriptCommandCall)?.let {
            if (shouldFold(it))
                return getStimFold(it)
        }
        return null
    }

    /**
     * Gets the folded text for this command call
     */
    private fun getStimFold(commandCall: CaosScriptCommandCall): String? {
        ProgressIndicatorProvider.checkCanceled()
        val variant = commandCall.containingCaosFile?.variant
            ?: return null
        val commandDefinition = commandCall.commandDefinition
            ?: return null
        return when (variant) {
            CaosVariant.C1, CaosVariant.C2 -> foldC1ChemCall(commandCall, commandDefinition)
            CaosVariant.CV, CaosVariant.C3, CaosVariant.DS -> foldC3ChemCall(commandCall, commandDefinition)
            else -> null
        }
    }

    /**
     * Folds a C1/C2 chemical call
     */
    private fun foldC1ChemCall(commandCall: CaosScriptCommandCall, commandDefinition: CaosCommand) =
        foldChemCallGeneric(commandCall, commandDefinition)

    /**
     * Folds a CV+ chemical call
     */
    private fun foldC3ChemCall(commandCall: CaosScriptCommandCall, commandDefinition: CaosCommand): String? {
        return when (commandDefinition.command) {
            "EMIT" -> formatEmit(commandCall, commandDefinition)
            else -> foldChemCallGeneric(commandCall, commandDefinition)
        }
    }

    private fun formatEmit(commandCall: CaosScriptCommandCall, commandDefinition: CaosCommand): String? {
        val variant = commandCall.variant
            ?: return null
        val arguments = commandCall.arguments
        if (arguments.size < 2)
            return null
        val caNumber = (arguments[0] as? CaosScriptRvalue)?.intValue
            ?: return null
        val ca = commandDefinition
            .parameters
            .getOrNull(0)
            ?.valuesList
            ?.get(variant)
            ?.get(caNumber)
            ?.name.nullIfEmpty()
            ?: return null

        val amount = (arguments[1] as? CaosScriptRvalue)?.floatValue
            ?: return "Emit CA: $ca"
        val amountString = if (amount < 0) "$amount" else "+$amount"
        return "Emit $amountString CA: $ca"
    }

    /**
     * Generic method for building a chemical folded string
     */
    private fun foldChemCallGeneric(commandCall: CaosScriptCommandCall, commandDefinition: CaosCommand): String? {
        ProgressIndicatorProvider.checkCanceled()
        // Pull arguments from call
        val arguments = commandCall.arguments

        // Get parameters from definition
        val parameters = commandDefinition.parameters

        // Find first parameter matching the name of the expected folding start
        val firstIndex = parameters
            .sortedBy { it.index }
            .firstOrNull { parameter ->
                firstFoldParameterRegex.matches(parameter.name)
            }
            ?.index
            ?: return null
        // If first index is greater than all arguments in list return
        if (firstIndex >= arguments.size)
            return null
        // Get arguments sublist starting at start index for folding
        val chemParameters = arguments.subList(firstIndex, arguments.size)

        // Ensure and get variant
        val variant = commandCall.containingCaosFile?.variant
            ?: return null

        // Ensure that chemicals and chem amounts are 1:1
        if (chemParameters.size % 2 != 0) {
            return null
        }
        // Determine number of chemical and amount pairs
        val numParameters = chemParameters.size / 2

        // Create string builder to hold folding text
        val stringBuilder = StringBuilder()
        val format = ValuesFormat.getFormat(variant, commandCall.commandString ?: "")

        var skipped = 0
        // Fold chemical amount pairs
        for (i in 0 until numParameters) {
            val pos = i * 2
            val argument = chemParameters[pos]
            val expression = argument as? CaosScriptRvalue
            //Ignore blank input
            if (expression == null || expression.text == "255")
                continue
            // Get type def value.
            val value = expression.getValuesListValue()?.name
                ?: getFallbackName(variant, parameters[pos], expression.text)
                ?: expression.text.apply {
                    skipped++
                }
                ?: continue
            val amount = chemParameters[pos + 1].text
            formatChemAmount(variant, stringBuilder, value, amount, format)
        }
        if (skipped == numParameters) {
            return null
        }
        return stringBuilder.toString().trim(' ', ',').nullIfEmpty()
        //?: "+ <<NOTHING>>"
    }

    private fun getFallbackName(variant: CaosVariant, parameter: CaosParameter, rawText: String): String? {
        val listName = parameter.valuesList[variant]?.name
            ?: return null
        val value = rawText.toFloatSafe()
            ?: return null
        return when {
            listName.startsWith("chem") -> "Chem $value"
            listName.startsWith("driv") -> "Drive $value"
            else -> null
        }
    }

    /**
     * Responsible for formatting a chemical with its amount
     */
    private fun formatChemAmount(
        variant: CaosVariant,
        stringBuilder: StringBuilder,
        value: String,
        amountVar: String,
        format: ValuesFormat = ValuesFormat.NORMAL,
    ) {

        // Get amount as float
        val amount = amountVar.toFloatSafe() ?: 0.0f
        val amountIsNumber = amountVar.toFloatSafe() != null

        // Check how this chemical should be folded
        if (format == ValuesFormat.STIM) {
            stringBuilder.append(value)
        }
        // If amount is roughly zero (with error tolerance) return without appending any data
        if (abs(amount) < 0.0005)
            return

        if (format == ValuesFormat.STIM) {
            stringBuilder.append(" * ").append(amount)
            return
        }

        // Format amount based on variant
        // C1/C2 use integers
        val amountString = if (variant.isOld)
            "${amount.roundToInt()}"
        // CV+ use floats, so use amount as is
        else
            "$amount"
        stringBuilder.append(", ")
        // If chemical name and amount need to be reversed
        if (format == ValuesFormat.REVERSED) {

            // example thirst +0.9
            stringBuilder.append(value).append(" ")
            if (amount > 0 && !amountIsNumber)
                stringBuilder.append("+")
            if (amountIsNumber)
                stringBuilder.append(amountString)
            // Chemical amounts should proceed chemical name
        } else {
            // ie. +3 Hunger Decrease
            if (amount > 0 || !amountIsNumber)
                stringBuilder.append("+")
            if (amountIsNumber)
                stringBuilder.append(amountString).append(" ").append(value)
            else
                stringBuilder.append(value)
        }
    }


    /**
     * Base method to build the regions that should be folded
     */
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

    // Helper function to get actual folding regions for command calls
    private fun getCommandCallFoldingRegion(
        commandCall: CaosScriptCommandCall,
        group: FoldingGroup,
    ): FoldingDescriptor? {
        if (!shouldFold(commandCall))
            return null
        return getFoldingDescriptor(commandCall, group)
    }

    // Gets the folding descriptor using the command call and the group
    private fun getFoldingDescriptor(commandCall: CaosScriptCommandCall, group: FoldingGroup): FoldingDescriptor? {
        if (shouldFoldAll.matches(commandCall.commandString ?: "")) {
            return FoldingDescriptor(commandCall.node, commandCall.textRange, group)
        }
        val arguments = commandCall.arguments
        val parameters = commandCall.commandDefinition?.parameters
            ?: return null

        val firstArgument = if (parameters.firstOrNull()?.type == CaosExpressionValueType.AGENT) {
            arguments.getOrNull(1)
        }  else {
            arguments.firstOrNull()
        }

        val start = firstArgument
            ?.startOffset
            ?: return null
        val end = arguments.lastOrNull()?.endOffset
            ?: return null
        return FoldingDescriptor(commandCall.node, TextRange.create(start, end), group)
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }

    private fun shouldFold(commandCall: CaosScriptCommandCall): Boolean {
        if (!shouldFold.matches(commandCall.commandString ?: ""))
            return false
        if (commandCall.arguments.none { types -> types.inferredType.any { type -> type.isNumberType } })
            return false
        val commandString = commandCall.commandStringUpper
            ?: return false
        val commandStringFirstWord = commandString.substring(0, 4)
        val commandCallText = commandCall.text.lowercase()
        val shouldFoldRaw = when (commandStringFirstWord) {
            "EMIT" -> EMIT_REGEX.matches(commandCallText)
            "CHEM", "DRIV" -> CHEM_REGEX.matches(commandCallText)
            "STIM" -> STIM_C1E_REGEX.matches(commandCallText) || STIM_C2E_REGEX.matches(commandCallText)
            "SWAY" -> SWAY_REGEX.matches(commandCallText)
            else -> false
        }
        if (!shouldFoldRaw) {
            return false
        }
        return getStimFold(commandCall) != null
    }

}

private val STIM_REGEX by lazy { "[Ss][Tt][Ii][Mm]([ ][^ ]{4})*".toRegex() }
private val shouldFoldAll by lazy {  "([Dd][Rr][Ii][Vv]|[Cc][Hh][Ee][Mm]|[Ee][Mm][Ii][Tt])".toRegex()}
private val shouldFold by lazy {
"([Ss][Tt][Ii][Mm]|[Dd][Rr][Ii][Vv]|[Ss][Ww][Aa][Yy]|[Cc][Hh][Ee][Mm]|[Ee][Mm][Ii][Tt])([ ][^ ]{4})*".toRegex()}
private const val STIMULUS = "[Ss][Tt][Ii][Mm]([Uu][Ll][Uu][Ss])?"
private const val DRIVE = "[Dd][Rr][Ii][Vv]([Ee])?"
private const val CHEMICAL = "[Cc][Hh][Ee][Mm]([Ii][Cc][Aa][Ll][Ss]?)?"
private const val CA = "[Cc][Aa][^0-9]*"
private const val NUMBER_REGEX = "[+-]?(\\.\\d+)?\\d+"
private val EMIT_REGEX by lazy {  "emit\\s+$NUMBER_REGEX.+".toRegex()}
private val CHEM_REGEX by lazy {  "(chem|driv)\\s+$NUMBER_REGEX\\s+[^ ]+$".toRegex()}
private val STIM_C1E_REGEX by lazy {
    "stim (shou|tact|sign|from|(writ\\s+[^ ]{1,4}))(\\s+[^ ]+){4}((\\s+$NUMBER_REGEX)(\\s+[^ ]+))+$".toRegex()
}
private val STIM_C2E_REGEX by lazy {
    "stim (shou|tact|sign|from|(writ\\s+[^ ]{1,4}))\\s+$NUMBER_REGEX\\s+[^ ]+$".toRegex()
}
private val SWAY_REGEX by lazy {
    "sway (shou|tact|sign|from|(writ\\s+[^ ]{1,4}))(\\s+$NUMBER_REGEX\\s+[^ ]+)+$".toRegex()
}
private val firstFoldParameterRegex by lazy {
    "($STIMULUS|$DRIVE|$CHEMICAL|$CA)[0-9]*".toRegex()
}

private enum class ValuesFormat {
    NORMAL,
    STIM,
    REVERSED;

    companion object {
        fun getFormat(variant: CaosVariant, commandString: String): ValuesFormat {
            return when {
                variant.isNotOld && STIM_REGEX.matches(commandString) -> STIM
                commandString.equalsIgnoreCase("emit") -> REVERSED
                else -> NORMAL
            }
        }
    }
}