package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.annotators.AnnotationHolderWrapper
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType

class CaosScriptHighlighterAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        when {
            element is CaosScriptToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            element is CaosScriptIsRvalueKeywordToken || element.parent is CaosScriptIsRvalueKeywordToken-> colorize(element, wrapper, CaosScriptSyntaxHighlighter.RVALUE_TOKEN)
            element is CaosScriptIsLvalueKeywordToken || element.parent is CaosScriptIsLvalueKeywordToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.LVALUE_TOKEN)
            isCommandKeyword(element) && element.text.toLowerCase() != "retn" -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.COMMAND_TOKEN)
            element is CaosScriptAnimationString || element.hasParentOfType(CaosScriptAnimationString::class.java) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptCGsub -> colorize(element.kGsub, wrapper, CaosScriptSyntaxHighlighter.KEYWORDS)
            element is CaosScriptSubroutineName -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.SUBROUTINE_NAME)
            element.text.toLowerCase() == "inst" -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.KEYWORDS)
        }
    }

    /**
     * Strips info annotations from a given element
     * Making it appear as regular text
     */
    private fun stripAnnotation(psiElement: PsiElement, annotationHolder: AnnotationHolderWrapper) {
        annotationHolder.newInfoAnnotation("")
                .range(psiElement)
                .enforcedTextAttributes(TextAttributes.ERASE_MARKER)
                .create()
    }

    private fun isCommandKeyword(element:PsiElement) : Boolean {
        return element is CaosScriptIsCommandKeywordToken
    }

    /**
     * Helper function to add color and style to a given element
     */
    private fun colorize(psiElement: PsiElement, annotationHolder: AnnotationHolderWrapper, attribute: TextAttributesKey, message: String? = null) {
        annotationHolder.newInfoAnnotation(message)
                .range(psiElement)
                .textAttributes(attribute)
                .create()
    }
}