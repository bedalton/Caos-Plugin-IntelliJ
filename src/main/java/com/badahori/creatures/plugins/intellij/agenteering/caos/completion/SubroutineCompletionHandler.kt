package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCGsub
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.collectDescendantsOfType

internal object SubroutineCompletionHandler {

    fun completeSubroutineNamesForNewSubroutine(
        result: CompletionResultSet,
        element: PsiElement
    ) {
        if (!element.isOrHasParentOfType(CaosScriptSubroutineName::class.java)) {
            return
        }
        if (element.hasParentOfType(CaosScriptCGsub::class.java)) {
            return
        }
        val script = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        val gsubNames = script.collectElementsOfType(CaosScriptCGsub::class.java)
            .mapNotNull { it.subroutineName?.text }
        val subroutines = script.collectElementsOfType(CaosScriptSubroutine::class.java)
            .mapNotNull { it.name.nullIfEmpty()?.lowercase() }
        val needed = gsubNames.filter { it.lowercase() !in subroutines }
        for (newName in needed) {
            result.addElement(
                LookupElementBuilder.create(newName)
                    .withPresentableText("subr $newName")
                    .withCaseSensitivity(false)
                    .withLookupString(newName)
                    .withLookupStrings(listOf(
                        newName.uppercase(),
                        newName.lowercase(),
                        newName.matchCase(element.text.case)
                    ))
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            )
        }

    }
}