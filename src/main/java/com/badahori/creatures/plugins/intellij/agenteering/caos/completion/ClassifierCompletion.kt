package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.ClassifierToAgentNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toTokenOrNull
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes

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
    ): Boolean {
        val project = element.project
        if (project.isDisposed || !element.isValid) {
            return false
        }
        val maxBack = 5
        var i = 0

        val elementText = element.textWithoutCompletionIdString
        val truePrevious = element.getPreviousNonEmptySibling(false)

        val quickComplete = truePrevious
            ?.toTokenOrNull(true) in commandsWithClassifiers ||
                (if (elementText.length == 4) token(elementText) else null) in commandsWithClassifiers

        var previous: PsiElement = element

        val numbers = Array<Int?>(4) { null }

        while (!quickComplete && i < maxBack) {
            val previousTemp = previous
                .getPreviousNonEmptySibling(false)
                ?: return false

            previous = previousTemp
            val token = getTokenSafe(previous)
            // Token is a token with classifier
            if (token in commandsWithClassifiers) {

                // Check if the command found allows non-numeric first parameters
                val allowsNonNumericFirstParameter = token in commandsAllowingNonNumericFirstParameter

                // There are non-numeric values which prevent completion
                if (!allowsNonNumericFirstParameter && (i > 0 && numbers.copyOfRange(0, i).count { it == null } > 0)) {
                    return false
                }
                if (allowsNonNumericFirstParameter) {
                    i--
                }
                break
            }

            // If value is non-numeric, then we should not try to complete
            val intValue = previous.text.toIntOrNull()
            if (intValue == null) {
                // last value can be non-numeric, but no others can be for completion
                // This has to do with 2 commands having an agent first parameter
                if (numbers.copyOfRange(0, i).count { it == null } > 0) {
                    return false
                }
            }
            if (i < numbers.size) {
                numbers[i] = intValue
            }
            i++
        }

        val typingName = quickComplete && elementText.toIntOrNull() == null

        val gonePastEndIndex = i >= numbers.size
        val classifierSoFar = if (typingName || gonePastEndIndex) {
            ""
        } else {
            numbers.copyOfRange(0, i).reversed().joinToString(" ")
        }
        val valuesToDrop = if (typingName || gonePastEndIndex) 0 else i

        val variantScope: GlobalSearchScope? = element.variant?.let {
            CaosVariantGlobalSearchScope(project, it, strict = false, searchLibraries = false)
        }

        var scope = variantScope

        if (!extendedSearch) {
            val directory = element.containingFile.directory
            if (directory != null) {
                val directoryScope = GlobalSearchScopes.directoriesScope(project, false, directory)
                scope = scope?.intersectWith(directoryScope) ?: directoryScope
            }
        }

        // Ensure there is at least
        if (!typingName && classifierSoFar.isBlank()) {
            return false
        }

        // Get classifiers or return false as no autocomplete items added
        val classifiers = getClassifiers(project, scope, typingName, classifierSoFar)
            ?: getClassifiers(project, variantScope, typingName, classifierSoFar)
            ?: return false

        // Get case for matching
        val case = elementText.case


        for ((classifier, name) in classifiers) {
            val value = classifier.split(' ').drop(valuesToDrop).joinToString(" ")
            val lookupElement = LookupElementBuilder.create(value)

                // Add primary lookup string
                .withLookupString(
                    if (typingName) {
                        // Match case is required as Intellij will not autocomplete with different starting case
                        name.matchCase(case)
                    } else {
                        classifier
                    }
                )

                // Add alternate lookup strings
                // TODO find out why these do not seem to be used
                .withLookupStrings(listOf(name, classifier, name.matchCase(case)).distinct())

                // Set presentable text with currently typed value first.
                // i.e. if typing text, show name first; if typing numbers, show classifier first
                .withPresentableText(
                    if (typingName) {
                        "$name - $classifier"
                    } else {
                        "$classifier - $name"
                    }
                )
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(lookupElement)
        }
        return true
    }

    private fun getClassifiers(
        project: Project,
        scope: GlobalSearchScope?,
        typingName: Boolean,
        classifierSoFar: String
    ): List<Pair<String, String>>? {
        return ClassifierToAgentNameIndex.getAllClassifiers(project, scope)
            .filter {
                typingName || it.first.startsWith(classifierSoFar)
            }
            .nullIfEmpty()
    }

    private fun getTokenSafe(element: PsiElement?): Int? {
        if (element?.textLength != 4) {
            return null
        }
        return try {
            token(element.text)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            null
        }
    }

}