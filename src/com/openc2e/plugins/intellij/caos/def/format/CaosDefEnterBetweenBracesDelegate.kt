package com.openc2e.plugins.intellij.caos.def.format

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class CaosDefEnterBetweenBracesDelegate : EnterBetweenBracesDelegate() {
    override fun isBracePair(lBrace: Char, rBrace: Char): Boolean {
        return (lBrace == '(' && rBrace == ')') || (lBrace == '[' && rBrace == ']')
    }
}
