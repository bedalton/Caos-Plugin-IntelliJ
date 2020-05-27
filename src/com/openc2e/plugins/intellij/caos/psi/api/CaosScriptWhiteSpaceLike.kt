package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.CompositeElement
import com.openc2e.plugins.intellij.caos.psi.util.elementType

interface CaosScriptWhiteSpaceLike : CaosScriptCompositeElement

fun CaosScriptWhiteSpaceLike.absoluteParentWhitespace() : PsiElement {
    var current:PsiElement = this
    while (current.parent is CaosScriptWhiteSpaceLike || current.parent?.elementType == TokenType.WHITE_SPACE) {
        current = current.parent
    }
    return current
}