@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptClassifier
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandElement


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
