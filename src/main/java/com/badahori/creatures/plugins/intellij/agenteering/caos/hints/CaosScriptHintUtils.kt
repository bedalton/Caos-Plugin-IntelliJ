package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesListId


fun CaosScriptRvalue.getValuesListValue(): CaosValuesListValue? {
    // Lists values can only be for expressions of string or int
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

private fun getEqualityExpressionValuesListValue(variant: CaosVariant, equalityExpression: CaosScriptEqualityExpressionPrime, expression: CaosScriptRvalue): CaosValuesListValue? {
    val valuesListId = equalityExpression.getValuesListId(variant, expression)
            ?: return null
    val valuesList = CaosLibs.valuesList[valuesListId]
            ?: return null
    expression.intValue?.let {value ->
        return valuesList[value]
    }
    expression.stringValue?.let {value ->
        return valuesList[value]
    }
    return null
}

private fun getCommandParameterValuesListValue(variant: CaosVariant, containingCommand: CaosScriptCommandElement, expression: CaosScriptRvalue, key: String): CaosValuesListValue? {
    val index = expression.index
    val parameter = containingCommand.commandDefinition?.parameters?.getOrNull(index)
            ?: return null
    val valuesList = parameter.valuesList[variant]
            ?: return null
    return valuesList[key]
}


internal fun getCommand(element: CaosScriptCommandElement): CaosCommand? {
    val commandToken = element.commandString
            ?: return null
    val variant = element.variant
            ?: return null
    val commandType = element.getEnclosingCommandType()
    val command = CaosLibs[variant][commandType][commandToken]
    return command
}

internal fun getCommand(commandToken: CaosScriptIsCommandToken): CaosCommand? {
    return (commandToken.parent as? CaosScriptCommandElement)?.commandDefinition
}