package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesListId
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.projectDisposed
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import kotlin.math.abs


fun CaosScriptRvalue.getValuesListValueString(): String? {
    // List values can only be for expressions of string or int
    if (!(isString || isInt || isFloat)) {
        return null
    }
    val variant = variant
        ?: return null

    getCommandParameterValuesListValueString(this)?.let { value ->
        return value
    }

    (parent as? CaosScriptEqualityExpressionPrime)?.let {
        return getEqualityExpressionValuesListValueString(variant, it, this)
    }

    return null
}

fun CaosScriptRvalue.getValuesListValue(): CaosValuesListValue? {
    // List values can only be for expressions of string or int
    if (!(isString || isInt || isFloat)) {
        return null
    }
    val variant = variant
        ?: return null


    (parent as? CaosScriptCommandElement)?.let { commandElement ->
        return getCommandParameterValuesListValue(variant, commandElement, this, text)
    }

    (parent as? CaosScriptEqualityExpressionPrime)?.let {
        return getEqualityExpressionValuesListValue(variant, it, this)
    }
    return null
}

private fun getEqualityExpressionValuesListValue(
    variant: CaosVariant,
    equalityExpression: CaosScriptEqualityExpressionPrime,
    expression: CaosScriptRvalue,
): CaosValuesListValue? {
    val valuesListId = equalityExpression.getValuesListId(variant, expression)
        ?: return null
    val valuesList = CaosLibs.valuesList[valuesListId]
        ?: return null
    expression.intValue?.let { value ->
        return valuesList[value]
    }
    expression.stringValue?.let { value ->
        return valuesList[value]
    }
    return null
}

private fun getEqualityExpressionValuesListValueString(
    variant: CaosVariant,
    equalityExpression: CaosScriptEqualityExpressionPrime,
    expression: CaosScriptRvalue,
): String? {
    val valuesListId = equalityExpression.getValuesListId(variant, expression)
        ?: return null
    val valuesList = CaosLibs.valuesList[valuesListId]
        ?: return null

    expression.intValue?.let { value ->
        return if (valuesList.bitflag) {
            getBitFlagText(valuesList, value, ",")
        } else {
            valuesList[value]?.name
        }
    }
    expression.stringValue?.let { value ->
        return valuesList[value]?.name
    }
    return null
}

private fun getCommandParameterValuesListValue(
    variant: CaosVariant,
    containingCommand: CaosScriptCommandElement,
    expression: CaosScriptRvalue,
    key: String,
): CaosValuesListValue? {
    val index = expression.index
    val parameter = containingCommand.commandDefinition?.parameters?.getOrNull(index)
        ?: return null
    val valuesList = parameter.valuesList[variant]
        ?: return null
    return valuesList[key]
}


private fun getCommandParameterValuesListValueString(
    rvalue: CaosScriptRvalue,
): String? {
    val valuesList = getValuesList(rvalue)
        ?: return null

    if (valuesList.bitflag && rvalue.isInt) {
        return getBitFlagText(valuesList, rvalue.intValue!!, ",")
    }
    return valuesList[rvalue.text]?.name
}


internal fun getCommand(element: CaosScriptCommandElement): CaosCommand? {
    val commandToken = element.commandString
        ?: return null
    val variant = element.variant
        ?: return null
    val commandType = element.getEnclosingCommandType()
    return CaosLibs[variant][commandType][commandToken]
}

internal fun getCommand(commandToken: CaosScriptIsCommandToken): CaosCommand? {
    return (commandToken.parent as? CaosScriptCommandElement)?.commandDefinition
}


internal fun formatRvalue(rvalue: CaosScriptRvalue?): Pair<String, TextRange>? {
    if (rvalue == null || rvalue.projectDisposed || DumbService.isDumb(rvalue.project)) {
        return null
    }

    formatRoomPropRvalue(rvalue)?.let {
        return it
    }

    formatRoomRvalue(rvalue)?.let {
        return Pair(it, rvalue.textRange)
    }

    val rvalueToken = rvalue.commandTokenElementType

    if (rvalueToken == CaosScriptTypes.CaosScript_K_DRV_EXC) {
        return Pair("Highest Drive", rvalue.textRange)
    }

    if (rvalueToken == CaosScriptTypes.CaosScript_K_CHEM) {
        val index = (rvalue.arguments.getOrNull(1) as? CaosScriptRvalue)?.intValue
            ?: return null
        return (rvalue.variant?.lib?.valuesList("Chemicals")?.values?.getOrNull(index)?.name)?.let {
            Pair(it, rvalue.textRange)
        }
    }

    if (rvalueToken == CaosScriptTypes.CaosScript_K_DRIV) {
        val index = (rvalue.arguments.getOrNull(1) as? CaosScriptRvalue)?.intValue
            ?: return null
        return rvalue.variant?.lib?.valuesList("Drives")?.values?.getOrNull(index)?.name?.let {
            Pair(it, rvalue.textRange)
        }
    }

    (getC1ClasText(rvalue))?.let {
        return Pair(it, rvalue.textRange)
    }

    rvalue.getValuesListValueString()?.let {
        return Pair(it, rvalue.textRange)
    }
    return null
}

private fun formatRoomRvalue(rvalue: CaosScriptRvalue): String? {
    (rvalue.parent as? CaosScriptRvalue)?.let { parent ->
        // Do not fold in PROP command
        if (parent.commandTokenElementType === CaosScriptTypes.CaosScript_K_PROP) {
            return null
        }
    }
    val commandToken = rvalue.commandTokenElementType
    return if (commandToken == CaosScriptTypes.CaosScript_K_ROOM && rvalue.arguments.size == 1) {
        val agent = rvalue.arguments[0].text.nullIfEmpty()
            ?: return null
        if (agent.contains(' '))
            null
        else
            "$agent's room"
    } else if (commandToken == CaosScriptTypes.CaosScript_K_GRAP && rvalue.arguments.size == 2) {
        val arguments = rvalue.arguments
        val xArgument = arguments[0] as? CaosScriptRvalue
            ?: return null
        val xValue = xArgument.varToken?.text
            ?: xArgument.intValue
            ?: return null
        val yArgument = arguments[1] as? CaosScriptRvalue
        val yValue = yArgument?.varToken?.text
            ?: yArgument?.intValue
            ?: return null
        "room at ($xValue, $yValue)"
    } else {
        null
    }
}

private fun formatRoomPropRvalue(rvalue: CaosScriptRvalue): Pair<String, TextRange>? {
    val command = (rvalue.parent as? CaosScriptCommandElement)
        ?: return null

    val token = command.commandTokenElementType
        ?: return null
    if (token != CaosScriptTypes.CaosScript_K_PROP && token != CaosScriptTypes.CaosScript_K_ALTR) {
        return null
    }
    // Only room itself should be folded. CA should be skipped
    if (rvalue.index != 0) {
        return null
    }
    val commandArguments = command.arguments
    if (commandArguments.size != 2 && commandArguments.size != 3) {
        return null
    }
    val roomValue = commandArguments[0] as? CaosScriptRvalue
        ?: return null
    val room = formatRoomRvalue(roomValue)
        ?: roomValue.text?.let { roomString ->
            if (roomString == "-1")
                "targ's Room"
            else
                null
        }
        ?: return null
    val caRvalue = (commandArguments[1] as? CaosScriptRvalue)
        ?: return null
    val value = caRvalue.parameterValuesListValue
        ?: return null
    val amount = commandArguments.getOrNull(2)?.text?.trim()
    val baseText = "${value.name} in $room"
    val text = if (amount != null) {
        if (command == CaosScriptTypes.CaosScript_K_PROP) {
            "set $baseText to $amount"
        } else {
            val mod = amount.toFloatOrNull()?.let {
                if (it >= 0) {
                    "+= $it"
                } else {
                    "-= ${abs(it)}"
                }
            } ?: "+= $amount"
            "$baseText $mod"
        }
    } else {
        baseText
    }
    return Pair(text, command.textRange)
}

//private fun formatDriveOrChemical(thisCommandToken:IElementType, otherValue:CaosScriptRvalue, ) {
//    val hasDriveOrChemical = thisCommandToken in CaosScriptDoifFoldingBuilder.foldableChemicals
//    val otherValueInt = otherValue
//    if (otherValueInt == null && !hasDriveOrChemical && otherValue.commandDefinition?.returnType != CaosExpressionValueType.AGENT)
//        return null
//
//    // Formats the primary value as a drive or chemical name as needed
//    val thisValueAsDriveOrChemical: String? =
//        if (hasDriveOrChemical && thisCommandToken != CaosScriptTypes.CaosScript_K_DRV_EXC)
//            thisValue.rvaluePrime?.let { rvaluePrime ->
//                CaosScriptDoifFoldingBuilder.formatChemicalValue(variant, rvaluePrime)
//            }
//        else
//            null
//}


internal fun getC1ClasText(element: PsiElement): String? {
    val rvalue = element as? CaosScriptRvalue
        ?: return null
    if ((element.parent as? CaosScriptCAssignment)?.lvalue?.text?.uppercase() != "CLAS") {
        return null
    }
    val clasValue = rvalue.intValue
        ?: return null
    val agentClass = CaosAgentClassUtils.parseClas(clasValue)
        ?: return null
    return "family:${agentClass.family} genus:${agentClass.genus} species:${agentClass.species}"
}