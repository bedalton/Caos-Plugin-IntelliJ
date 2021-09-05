@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotFolded
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

interface CaosScriptHintsProvider {

    fun isApplicable(element: PsiElement): Boolean
    fun provideHints(element: PsiElement): List<InlayInfo>
    fun getHintInfo(element: PsiElement): HintInfo?
    val enabled: Boolean
    val option: Option
    val priority: Int


    companion object {
        // When resolving hint providers, ensure element is not folded first
        fun resolve(element: PsiElement): CaosScriptHintsProvider? = if (element.isNotFolded)
            values.sortedByDescending { it.priority }
                    .firstOrNull { it.enabled && it.isApplicable(element) }
        else
            null

        val values: Array<CaosScriptHintsProvider> =
                arrayOf(*CaosScriptInlayTypeHint.values(), *CaosScriptInlayParameterHintsProvider.values())
    }
}