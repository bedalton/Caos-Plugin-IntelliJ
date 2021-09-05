package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTagName
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext


class PrayCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), PrayCompletionProvider)
    }

}


object PrayCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position.getSelfOrParentOfType(CaosScriptCompositeElement::class.java)
        if (element is PrayTagName) {

        }
    }
}