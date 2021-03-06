package com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.AnnotationHolderWrapper
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType

class CaosScriptHighlighterAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        when {
            (element is CaosScriptIsRvalueKeywordToken || element.parent is CaosScriptIsRvalueKeywordToken) && element.firstChild?.tokenType != CaosScriptTypes.CaosScript_TOKEN -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.RVALUE_TOKEN)
            element is CaosScriptIsCommandKeywordToken || (element.parent is CaosScriptIsCommandKeywordToken && element.parent.firstChild != element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.COMMAND_TOKEN)
            element is CaosScriptIsLvalueKeywordToken || element.parent is CaosScriptIsLvalueKeywordToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.LVALUE_TOKEN)
            element is CaosScriptAnimationString -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptByteString && isAnimationByteString(element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ANIMATION)
            element is CaosScriptByteString && !isAnimationByteString(element) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.BYTE_STRING)
            element is CaosScriptIsPrefixToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.PREFIX_TOKEN)
            element is CaosScriptIsSuffixToken -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.SUFFIX_TOKEN)
            element is CaosScriptSubroutineName || element.parent is CaosScriptSubroutineName -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.SUBROUTINE_NAME)
            element.text.toLowerCase() == "inst" -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.KEYWORDS)
            element is CaosScriptTokenRvalue -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            element is CaosScriptAtDirectiveComment -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.COMMENT)
            element.tokenType == CaosScriptTypes.CaosScript_WORD -> when {
                element.hasParentOfType(CaosScriptErrorCommand::class.java) -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.ERROR_COMMAND_TOKEN)
                element.parent is CaosScriptCGsub -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.SUBROUTINE_NAME)
                else -> colorize(element, wrapper, CaosScriptSyntaxHighlighter.TOKEN)
            }
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
                .enforcedTextAttributes(attribute.defaultAttributes)
                .create()
    }

    private fun isAnimationByteString(element: PsiElement): Boolean {
        val argumentParent = element.getParentOfType(CaosScriptArgument::class.java)
                ?: return false
        val index = argumentParent.index
        val commandParent = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return false
        return commandParent.commandDefinition?.let { command ->
            val parameter = command.parameters.getOrNull(index)
            parameter?.type == CaosExpressionValueType.ANIMATION
        } ?: false

    }
}