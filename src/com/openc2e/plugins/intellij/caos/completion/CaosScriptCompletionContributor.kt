package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER

class CaosScriptCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), CaosScriptCompletionProvider)
        LOGGER.info("Adding completion contributor")
    }

}