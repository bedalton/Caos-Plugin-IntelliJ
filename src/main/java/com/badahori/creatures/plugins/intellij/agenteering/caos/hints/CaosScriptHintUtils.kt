package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.intellij.openapi.progress.ProgressIndicatorProvider


fun CaosScriptRvalue.getValuesListValue(): CaosValuesListValue? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    val variant = variant
            ?: return null
    (parent as? CaosScriptCommandLike)?.let {
        return getCommandParameterValuesListValue(variant, it, this, text)
    }

    getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.let {
        return getEqualityExpressionValuesListValue(variant, it, this)
    }
    return null
}

private fun getEqualityExpressionValuesListValue(variant:CaosVariant, equalityExpression: CaosScriptEqualityExpressionPrime, expression: CaosScriptRvalue): CaosValuesListValue? {
    val valuesListId = equalityExpression.getValuesList(variant, expression)
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
    val valuesListId = containingCommand.commandDefinition?.parameters?.getOrNull(index)?.valuesListIds?.get(variant.code)
            ?: return null
    val valuesList = CaosLibs.valuesList[valuesListId]
            ?: return null
    return valuesList[key]
}


internal fun getCommand(element: CaosScriptCommandElement): CaosDefCommandDefElement? {
    val commandTokens = try {
        element.commandToken?.reference?.multiResolve(true)?.mapNotNull {
            ProgressIndicatorProvider.checkCanceled()
            (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
        }
    } catch (e: Exception) {
        null
    } ?: return null
    val numParameters = element.argumentsLength
    if (commandTokens.isEmpty())
        return null
    return if (commandTokens.size == 1) {
        commandTokens[0]
    } else {
        commandTokens.filter { it.parameterStructs.size == numParameters }.ifEmpty { null }?.first()
                ?: commandTokens.filter { it.parameterStructs.size > numParameters }.ifEmpty { null }?.first()
                ?: return null
    }
}

internal fun getCommand(commandToken: CaosScriptIsCommandToken): CaosCommand? {
    return (commandToken.parent as? CaosScriptCommandLike)?.commandDefinition
}