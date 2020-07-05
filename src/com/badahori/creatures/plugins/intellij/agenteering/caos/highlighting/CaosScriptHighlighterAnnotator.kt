package com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.AnnotationHolderWrapper
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.hasParentOfType

class CaosScriptHighlighterAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        if (element !is CaosScriptCompositeElement)
            return
        when {
            element is CaosScriptToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            element is CaosScriptExpectsToken -> if (element.textLength == 4) colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            element is CaosScriptIsRvalueKeywordToken || element.parent is CaosScriptIsRvalueKeywordToken-> colorize(element, wrapper, CaosScriptSyntaxHighlighter.RVALUE_TOKEN)
            element is CaosScriptIsLvalueKeywordToken || element.parent is CaosScriptIsLvalueKeywordToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.LVALUE_TOKEN)
            element is CaosScriptAnimationString -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptByteString && isAnimationByteString(element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptByteString && !isAnimationByteString(element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.BYTE_STRING)
            //element is CaosScriptCGsub -> colorize(element.cKwGsub, wrapper, CaosScriptSyntaxHighlighter.KEYWORDS)
            element is CaosScriptSubroutineName || element.parent is CaosScriptSubroutineName -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.SUBROUTINE_NAME)
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

    /**
     * Helper function to add color and style to a given element
     */
    private fun colorize(psiElement: PsiElement, annotationHolder: AnnotationHolderWrapper, attribute: TextAttributesKey, message: String? = null) {
        annotationHolder.newInfoAnnotation(message)
                .range(psiElement)
                .textAttributes(attribute)
                .create()
    }

    private fun isAnimationByteString(element: PsiElement) : Boolean {
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpectsByteString::class.java))
            return false
        val argumentParent = element.getParentOfType(CaosScriptArgument::class.java)
                ?: return false
        val index = argumentParent.index
        val commandParent = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return false
        return commandParent
                .commandToken
                ?.reference
                ?.multiResolve(true)
                .orEmpty()
                .mapNotNull { it.element as? CaosDefCommandDefElement }
                .any {
                    val parameter = it.parameterList
                            .getOrNull(index)
                            ?.parameterType
                            ?: return@any false
                    parameter == "[anim]"
                }
    }
}