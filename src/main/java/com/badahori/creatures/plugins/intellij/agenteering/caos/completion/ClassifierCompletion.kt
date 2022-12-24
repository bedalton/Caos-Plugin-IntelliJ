package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.ClassifierToAgentNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.PsiElement

object ClassifierCompletion {

    private val commandsWithClassifiers by lazy {
        listOf(
            token("cacl"),
            token("cati"),
            token("cls2"),
            token("dpas"),
            token("enum"),
            token("epas"),
            token("escn"),
            token("esee"),
            token("etch"),
            token("gpas"),
            token("ncls"),
            token("comp"),
            token("simp"),
            token("vhcl"),
            token("pcls"),
            token("rtar"),
            token("scrp"),
            token("scrx"),
            token("sorc"),
            token("sorq"),
            token("star"),
            token("totl"),
            token("ttar"),
            token("wild")
        )
    }

    private val commandsAllowingNonNumericFirstParameter = listOf(token("pcls"), token("ncls"))

    fun completeClassifier(
        resultSet: CompletionResultSet,
        extendedSearch: Boolean,
        element: PsiElement
    ) {
        val maxBack = 5
        var i = 0
        var previous: PsiElement = element
        var drop = -1
        val numericCharRange = '0'..'9'
        val numbers = Array<Boolean?>(4) { null }
        while (i < maxBack) {
            val previousTemp = previous.getPreviousNonEmptySibling(false)
                ?: return
            previous = previousTemp
            val token = getTokenSafe(previous)

            // Token is a token with classifier
            if (token in commandsWithClassifiers) {

                // Check if the command found allows non-numeric first parameters
                val allowsNonNumericFirstParameter = token in commandsAllowingNonNumericFirstParameter

                // There are non-numeric values which prevent completion
                if (!allowsNonNumericFirstParameter && numbers.count { it == false } > 0) {
                    return
                }
                drop = if (allowsNonNumericFirstParameter) {
                    i - 1
                } else {

                    i
                }
                break
            }

            // If value is non-numeric, then we should not try to complete
            if (previous.text.count { it !in numericCharRange } > 0) {
                // last value can be non-numeric, but no others can be for completion
                // This has to do with 2 commands having an agent first parameter
                if (numbers.count { it == false } > 0) {
                    return
                }
                if (i < numbers.size) {
                    numbers[i]
                }
            }
            i++
        }

        // If drop is less than 0, than no completion can be done
        if (drop < 0) {
            return
        }

        val classifiersToNames = ClassifierToAgentNameIndex.getAgentNames()

    }

    private fun getTokenSafe(element: PsiElement?): Int? {
        if (element?.textLength != 4) {
            return null
        }
        return try {
            token(element.text)
        } catch (_: Exception) {
            null
        }
    }

}