package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns

class CaosScriptCompletionContributor : CompletionContributor(), DumbAware {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), CaosScriptCompletionProvider)
    }

}