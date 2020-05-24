package com.openc2e.plugins.intellij.caos.hints

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.caos.utils.orElse


fun CaosScriptExpression.getTypeDefValue(): CaosDefTypeDefValueStruct? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    (parent?.parent as? CaosScriptExpectsValueOfType)?.let {
        return getCommandParameterTypeDefValue(it)
    }

    getParentOfType(CaosScriptEqualityExpression::class.java)?.let {
        return getEqualityExpressionTypeDefValue(it, this)
    }
    return null
}

private fun getEqualityExpressionTypeDefValue(equalityExpression: CaosScriptEqualityExpression, expression: CaosScriptExpression): CaosDefTypeDefValueStruct? {
    val value = expression.intValue?.let { "$it" } ?: expression.stringValue ?: return null
    val other = equalityExpression.expressionList.let {
        when (it.size) {
            0, 1 -> return null
            2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
            else -> {
                LOGGER.severe("Equality operator expects exactly TWO expressions")
                return null
            }
        }
    } ?: return null
    val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
    if (token == null) {
        LOGGER.info("Failed to find rvaluePrime in equality expressions '${other.text}'")
        return null
    }
    val reference = token
            .reference
            .multiResolve(true)
            .firstOrNull()
            ?.element
            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
    if (reference == null) {
        LOGGER.info("Failed to resolve reference to ${token.text} in equality expression")
        return null
    }
    val typeDef = reference
            .docComment
            ?.returnTypeStruct
            ?.type
            ?.typedef
    if (typeDef == null) {
        LOGGER.info("Equality expressions type is null in typedef for command ${token.text}")
        return null
    }
    val variant = expression.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
    return getListValue(variant, typeDef, expression.project, value)
}

private fun getListValue(variant: String, listName: String, project: Project, key: String): CaosDefTypeDefValueStruct? {
    val list = CaosDefTypeDefinitionElementsByNameIndex
            .Instance[listName, project]
            .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
    if (list == null) {
        LOGGER.info("Failed to find list definition for name: $listName")
        return null
    }
    val value = list.keys.firstOrNull { it.key == key }
    if (value == null) {
        LOGGER.info("Failed to find value in list $listName for key $key")
    }
    return value
}

private fun getCommandParameterTypeDefValue(valueOfType: CaosScriptExpectsValueOfType): CaosDefTypeDefValueStruct? {
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
    if (parameterStruct == null) {
        LOGGER.info("Parameter struct is null for argument $index in command ${containingCommand.commandString}")
        return null
    }
    val typeDef = parameterStruct
            .type
            .typedef
    if (typeDef == null) {
        LOGGER.info("RValue type is null in typedef for command ${containingCommand.commandString}. Parameter Data: $parameterStruct")
        return null
    }
    val variant = valueOfType.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
    return getListValue(variant, typeDef, valueOfType.project, valueOfType.text)
}


internal fun getCommand(element: CaosScriptCommandElement): CaosDefCommandDefElement? {
    val commandTokens = element.commandToken?.reference?.multiResolve(true)?.mapNotNull {
        (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
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
        (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
    }
    if (commandTokens.isEmpty())
        return null

    if (commandTokens.size == 1) {
        return commandTokens[0]
    }
    val numParameters = commandToken.getParentOfType(CaosScriptCommandElement::class.java)?.let {
        it.argumentsLength
    }.orElse(-1)

    return if (numParameters >= 0){
        commandTokens.filter { it.parameterStructs.size == numParameters }.ifEmpty { null }?.first()
                ?: commandTokens.filter { it.parameterStructs.size > numParameters }.ifEmpty { null }?.first()
                ?: return null
    } else {
        commandTokens.maxBy { it.parameterStructs.size }
    }
}

fun CaosScriptExpression.getCommand(): CaosDefCommandDefElement? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    (parent?.parent as? CaosScriptExpectsValueOfType)?.let {
        return getCommand(it)
    }

    getParentOfType(CaosScriptEqualityExpression::class.java)?.let {
        return getCommand(it, this)
    }
    return null
}

private fun getCommand(equalityExpression: CaosScriptEqualityExpression, expression: CaosScriptExpression): CaosDefCommandDefElement? {
    val other = equalityExpression.expressionList.let {
        when (it.size) {
            0, 1 -> return null
            2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
            else -> {
                LOGGER.severe("Equality operator expects exactly TWO expressions")
                return null
            }
        }
    } ?: return null
    val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
    if (token == null) {
        LOGGER.info("Failed to find rvaluePrime in equality expressions '${other.text}'")
        return null
    }
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
            ?: return null
}