package com.openc2e.plugins.intellij.caos.def.contributor

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern

class CaosDefCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, ElementPattern)
    }
}