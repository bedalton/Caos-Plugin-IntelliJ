package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets

class CaosScriptHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element !is CaosScriptCompositeElement)
            return
        when {
            element.elementType in CaosScriptTokenSets.ANIMATION_STRING -> {
                val annotation = annotationHolder.createInfoAnnotation(element, "")
                annotation.textAttributes = CaosScriptSyntaxHighlighter.ANIMATION
            }

        }
    }


}