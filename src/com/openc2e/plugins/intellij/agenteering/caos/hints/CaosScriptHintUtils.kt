package com.openc2e.plugins.intellij.agenteering.caos.hints

import com.intellij.openapi.project.Project
import com.openc2e.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.lang.variant
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpectsValueOfType
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.argumentsLength
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.openc2e.plugins.intellij.agenteering.caos.utils.orElse


fun CaosScriptExpression.getTypeDefValue(): CaosDefTypeDefValueStruct? {
    // Lists values can only be for expressions of string or int
    if (!(isString || isInt)) {
        return null
    }
    (parent?.parent as? CaosScriptExpectsValueOfType)?.let {
        return getCommandParameterTypeDefValue(it, text)
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
        return null
    }
    val reference = token
            .reference
            .multiResolve(true)
            .firstOrNull()
            ?.element
            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
    if (reference == null) {
        return null
    }
    val typeDef = reference
            .docComment
            ?.returnTypeStruct
            ?.type
            ?.typedef
    if (typeDef == null) {
        return null
    }
    val variant = expression.containingCaosFile.variant
    return getListValue(variant, typeDef, expression.project, value)
}

private fun getListValue(variant: CaosVariant, listName: String, project: Project, key: String): CaosDefTypeDefValueStruct? {
    val list = CaosDefTypeDefinitionElementsByNameIndex
            .Instance[listName, project]
            .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
            ?: return null
    return list.keys.firstOrNull { it.key == key }
}

private fun getCommandParameterTypeDefValue(valueOfType: CaosScriptExpectsValueOfType, key: String): CaosDefTypeDefValueStruct? {
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
    val typeDef = parameterStruct
            .type
            .typedef
            ?: return null

    val variant = valueOfType.containingCaosFile.variant
    return getListValue(variant, typeDef, valueOfType.project, key)
}


internal fun getCommand(element: CaosScriptCommandElement): CaosDefCommandDefElement? {
    val commandTokens = try {
        element.commandToken?.reference?.multiResolve(true)?.mapNotNull {
            (it.element as? CaosDefCompositeElement)?.getParentOfType(CaosDefCommandDefElement::class.java)
        }
    } catch (e:Exception) { null } ?: return null
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
                return null
            }
        }
    } ?: return null
    val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
    if (token == null) {
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