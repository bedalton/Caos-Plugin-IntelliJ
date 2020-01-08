package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPatternBean
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage

class CaosScriptCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),CaosScriptCompletionProvider)
    }

}