package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue

/**
 * Finds the value list id to use for the opposing expression in an equality comparison
 */
fun CaosScriptEqualityExpressionPrime.getValuesListId(variant:CaosVariant, expression: CaosScriptRvalue): Int? {
    return getValuesListId(variant, this, expression)
}

/**
 * Find the value list definition for the opposing expression
 */
fun CaosScriptEqualityExpressionPrime.getValuesList(
        variant:CaosVariant,
        expression: CaosScriptRvalue
) : CaosValuesList? {
    val valuesListId = getValuesListId(variant, this, expression)
            ?: return null
    return CaosLibs.valuesList[valuesListId]
}

/**
 * Find values list id for opposing expression
 */
private fun getValuesListId(
        variant: CaosVariant,
        equalityExpressionPrime: CaosScriptEqualityExpressionPrime,
        expression: CaosScriptRvalue
) : Int? {
    // Find the opposing expression in this equality expression
    val other: CaosScriptRvalue = equalityExpressionPrime.rvalueList.let let@{ rvaluesList ->
        when {
            // If there is no opposing expression yet, bail out
            rvaluesList.size < 2 -> null
            // If there are too many expressions, bail out
            // This should not happen
            rvaluesList.size > 2 -> {
                LOGGER.severe("Equality operator expects exactly TWO expressions. Found: ${rvaluesList.size}. Expressions ${rvaluesList.map { "'${it.text}'" }}")
                null
            }
            // Expression is left side expression, return right
            rvaluesList[0] == expression -> rvaluesList[1]
            // Expression is right side, return left
            else -> rvaluesList[0]
        }
    } ?: return null
    val listIds: Map<String, Int> = other
            // Only rvalue primes have command types as they are command calls
            .rvaluePrime
            // Get Command definition from rvalue prime command call
            ?.commandDefinition
            // Command definitions hold return value list types as a map value per variant
            ?.returnValuesListIds
            // Not return values map found, return empty handed
            ?: return null
    // Return the valuesListId for the given variant
    return listIds[variant.code]
}