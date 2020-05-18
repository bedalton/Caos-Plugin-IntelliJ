package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.annotators.AnnotationHolderWrapper
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType
import com.openc2e.plugins.intellij.caos.utils.isOrHasParentOfType

class CaosScriptHighlighterAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        if (element !is CaosScriptCompositeElement)
            return
        when {
            element is CaosScriptToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            element is CaosScriptIsRvalueKeywordToken || element.parent is CaosScriptIsRvalueKeywordToken-> colorize(element, wrapper, CaosScriptSyntaxHighlighter.RVALUE_TOKEN)
            element is CaosScriptIsLvalueKeywordToken || element.parent is CaosScriptIsLvalueKeywordToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.LVALUE_TOKEN)
            element is CaosScriptAnimationString -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptByteString && isAnimationByteString(element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            //isCommandKeyword(element) && element.text.toLowerCase() != "retn" -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.COMMAND_TOKEN)
            element is CaosScriptCGsub -> colorize(element.cKwGsub, wrapper, CaosScriptSyntaxHighlighter.KEYWORDS)
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

    private fun isAnimationByteString(element: CaosScriptByteString) : Boolean {
        if (!element.hasParentOfType(CaosScriptExpectsByteString::class.java))
            return false
        val argumentParent = element.getParentOfType(CaosScriptArgument::class.java)
                ?: return false
        val index = argumentParent.index
        val commandParent = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return false
        val command = commandParent.commandString
        val variant = element.containingCaosFile?.variant
        return CaosDefCommandElementsByNameIndex.Instance[command, element.project]
                .any {
                    if (!it.isRvalue)
                        return@any false
                    if (variant != null && !it.isVariant(variant))
                        return@any false
                    val parameter = it.parameterList
                            .getOrNull(index)
                            ?.parameterType
                            ?: return@any false
                    parameter == "[anim]"
                }
    }
}