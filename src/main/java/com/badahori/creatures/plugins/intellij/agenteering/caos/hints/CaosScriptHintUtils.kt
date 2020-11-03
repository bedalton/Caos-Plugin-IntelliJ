package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.ValuesListEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project


fun CaosScriptLiteral.getValuesListValue(): CaosDefValuesListValueStruct? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    (parent?.parent as? CaosScriptExpectsValueOfType)?.let {
        return getCommandParameterValuesListValue(it, text)
    }

    getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.let {
        return getEqualityExpressionValuesListValue(it, this)
    }
    return null
}

private fun getEqualityExpressionValuesListValue(equalityExpression: CaosScriptEqualityExpressionPrime, expression: CaosScriptLiteral): CaosDefValuesListValueStruct? {
    val value = expression.intValue?.let { "$it" }
            ?: expression.stringValue
            ?: return null
    val valuesList = equalityExpression.getValuesList(expression)
            ?: return null
    val variant = expression.variant
            ?: return null
    return getListValue(variant, valuesList, expression.project, value)
}

internal fun getListValue(variant: CaosVariant, listName: String, project: Project, key: String): CaosDefValuesListValueStruct? {
    return CaosDefValuesListElementsByNameIndex
            .Instance[listName, project]
            .filter { it.containingCaosDefFile.isVariant(variant, true) }
            .flatMap { it.valuesListValues }
            .firstOrNull {
                when (it.equality) {
                    ValuesListEq.EQUAL -> it.key == key
                    ValuesListEq.NOT_EQUAL -> it.key != key
                    ValuesListEq.GREATER_THAN -> try { key.toInt() > it.key.replace("[^0-9]".toRegex(), "").toInt() } catch (e:Exception) { false }
                }
            }
}

private fun getCommandParameterValuesListValue(valueOfType: CaosScriptExpectsValueOfType, key: String): CaosDefValuesListValueStruct? {
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
            ?: return null
    val valuesList = parameterStruct
            .type
            .valuesList
            ?: return null

    val variant = valueOfType.containingCaosFile?.variant
            ?: return null
    return getListValue(variant, valuesList, valueOfType.project, key)
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

internal fun getCommand(commandToken: CaosScriptIsCommandToken): CaosDefCommandDefElement? {
    val commandTokens = commandToken.reference.multiResolve(true).mapNotNull {
        ProgressIndicatorProvider.checkCanceled()
        (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
    }
    if (commandTokens.isEmpty())
        return null

    if (commandTokens.size == 1) {
        return commandTokens[0]
    }
    val numParameters = commandToken.getParentOfType(CaosScriptCommandElement::class.java)
            ?.argumentsLength
            .orElse(-1)

    return if (numParameters >= 0) {
        commandTokens.filter { it.parameterStructs.size == numParameters }.ifEmpty { null }?.first()
                ?: commandTokens.filter { it.parameterStructs.size > numParameters }.ifEmpty { null }?.first()
                ?: return null
    } else {
        commandTokens.maxBy { it.parameterStructs.size }
    }
}

fun CaosScriptLiteral.getCommand(): CaosDefCommandDefElement? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    (parent?.parent as? CaosScriptExpectsValueOfType)?.let {
        return getCommand(it)
    }

    getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.let {
        return getCommand(it, this)
    }
    return null
}

private fun getCommand(equalityExpression: CaosScriptEqualityExpressionPrime, expression: CaosScriptLiteral): CaosDefCommandDefElement? {
    val other = equalityExpression.expressionList.let {
        when (it.size) {
            0, 1 -> return null
            2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
            else -> {
                return null
            }
        }
    } ?: return null
    val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
            ?: return null
    return token
            .reference
            .multiResolve(true)
            .firstOrNull()
            ?.element
            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
}


private fun getCommand(valueOfType: CaosScriptExpectsValueOfType): CaosDefCommandDefElement? {
    val containingCommand = valueOfType.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return null
    return containingCommand
            .commandToken
            ?.reference
            ?.multiResolve(true)
            ?.firstOrNull()
            ?.element
            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
}