@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.common

import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotFolded
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement

interface InlayHintGenerator {

    fun isApplicable(element: PsiElement): Boolean
    fun provideHints(element: PsiElement): List<InlayInfo>
    fun getHintInfo(element: PsiElement): HintInfo?
    val enabled: Boolean
    val option: Option
    val priority: Int
}