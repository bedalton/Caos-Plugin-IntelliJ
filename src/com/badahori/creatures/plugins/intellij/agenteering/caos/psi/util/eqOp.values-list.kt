package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken

/**
 * Finds the value list to use for the opposing expression in an equality comparison
 */
fun CaosScriptEqualityExpressionPrime.getValuesList(expression: CaosScriptLiteral) : String? {
    val other: CaosScriptLiteral = expressionList.let let@{
        when {
            it.size < 2 -> null
            it.size > 2 -> {
                LOGGER.severe("Equality operator expects exactly TWO expressions. Found: ${it.size}. Expressions ${it.map { "'${it.text}'" }}")
                null
            }
            it[0] == expression -> it[1]
            else -> it[0]
        }
    } ?: return null
    val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
            ?: return null
    val reference = token
            .reference
            .multiResolve(true)
            .firstOrNull()
            ?.element
            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
            ?: return null
    return reference
            .docComment
            ?.returnTypeStruct
            ?.type
            ?.valuesList
}