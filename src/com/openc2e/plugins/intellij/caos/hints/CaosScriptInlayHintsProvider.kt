@file:Suppress("UnstableApiUsage")

package com.openc2e.plugins.intellij.caos.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpression
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpressionList
import com.openc2e.plugins.intellij.caos.utils.now
import com.openc2e.plugins.intellij.caos.utils.orElse


class CaosScriptInlayHintsProvider : InlayParameterHintsProvider {


    override fun getParameterHints(element: PsiElement?): MutableList<InlayInfo> {
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

    override fun getHintInfo(p0: PsiElement?): HintInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

object CaosScriptInlayHintsUtil {
    private val PARAM_NAME_USER_DATA_KEY = Key<String>("com.openc2e.plugins.intellij.caos.hints.PARAM_NAME")
    private val PARAM_TYPE_USER_DATA_KEY = Key<String>("com.openc2e.plugins.intellij.caos.hints.PARAM_TYPE")
    private val PARAM_DATA_TIME = Key<Long>("com.openc2e.plugins.intellij.caos.hints.PARAM_TYPE")
    private const val CACHE_TIME = 3000
    fun getParamName(expression:CaosScriptExpression) : String? {
        val name = expression.getUserData(PARAM_NAME_USER_DATA_KEY)
        if (now - expression.getUserData(PARAM_DATA_TIME).orElse(0) > CACHE_TIME || name == null) {

        }
        return null
    }
}
