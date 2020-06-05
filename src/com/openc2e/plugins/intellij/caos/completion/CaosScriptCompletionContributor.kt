package com.openc2e.plugins.intellij.caos.completion

import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns

class CaosScriptCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), CaosScriptCompletionProvider)
    }

}