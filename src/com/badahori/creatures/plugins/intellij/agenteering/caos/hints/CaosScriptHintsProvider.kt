@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement

interface CaosScriptHintsProvider {

    fun isApplicable(element:PsiElement) : Boolean
    fun provideHints(element:PsiElement) : List<InlayInfo>
    fun getHintInfo(element:PsiElement) : HintInfo?
    val enabled:Boolean
    val option:Option
    val priority:Int


    companion object {
        fun resolve(element: PsiElement): CaosScriptHintsProvider? =
                values.sortedByDescending { it.priority }.find { it.isApplicable(element) }

        val values: Array<CaosScriptHintsProvider> =
                arrayOf(*CaosScriptInlayTypeHint.values(), *CaosScriptInlayParameterHintsProvider.values())
    }
}