@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.agenteering.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptClassifier


class CaosScriptInlayHintsProvider : InlayParameterHintsProvider {
    override fun getSupportedOptions(): List<Option> =
            CaosScriptHintsProvider.values.map { it.option }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        val resolved = CaosScriptHintsProvider.resolve(element)
                ?: return mutableListOf()
        if (!resolved.enabled)
            return mutableListOf()
        return resolved.provideHints(element)
    }

    override fun getInlayPresentation(inlayText: String): String = inlayText

    override fun getDefaultBlackList(): Set<String> {
        return mutableSetOf()
    }

    override fun getHintInfo(element: PsiElement): HintInfo? {
        return CaosScriptHintsProvider.resolve(element)?.getHintInfo(element)
    }

}
