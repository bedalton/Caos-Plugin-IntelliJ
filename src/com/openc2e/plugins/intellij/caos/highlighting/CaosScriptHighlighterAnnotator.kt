package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptAnimationString
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType

class CaosScriptHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element !is CaosScriptCompositeElement)
            return
        when {
            element is CaosScriptAnimationString || element.hasParentOfType(CaosScriptAnimationString::class.java)
                -> annotationHolder.colorize(element, CaosScriptSyntaxHighlighter.ANIMATION)
            element.text.toLowerCase() == "inst" -> annotationHolder.colorize(element, CaosScriptSyntaxHighlighter.KEYWORDS)
        }
    }


}

fun AnnotationHolder.colorize(element: PsiElement, attributes: TextAttributesKey) {
    val annotation = createInfoAnnotation(element, "")
    annotation.textAttributes = attributes
}

fun AnnotationHolder.colorize(range: TextRange, attributes: TextAttributesKey) {
    val annotation = createInfoAnnotation(range, "")
    annotation.textAttributes = attributes
}