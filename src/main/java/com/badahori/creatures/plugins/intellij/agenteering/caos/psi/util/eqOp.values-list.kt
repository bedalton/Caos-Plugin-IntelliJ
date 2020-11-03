package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant

/**
 * Finds the value list id to use for the opposing expression in an equality comparison
 */
fun CaosScriptEqualityExpressionPrime.getValuesList(variant:CaosVariant, expression: CaosScriptRvalue): Int? {
    val other: CaosScriptRvalue = rvalueList.let let@{
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
    val listIds: Map<String, Int> = other.rvaluePrime?.commandDefinition?.returnValuesListIds
            ?: return null
    return listIds[variant.code]
}