@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpressionList


class CaosScriptInlayHintsProvider : InlayParameterHintsProvider {


    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        if (element == null)
            return mutableListOf()
        val project = element.project;
        if (DumbService.isDumb(project))
            return mutableListOf()
        if (element is CaosScriptExpressionList) {
            //return getExpressionInlayHints(element)
        }
        return mutableListOf()
    }

    override fun getDefaultBlackList(): MutableSet<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHintInfo(p0: PsiElement): HintInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
