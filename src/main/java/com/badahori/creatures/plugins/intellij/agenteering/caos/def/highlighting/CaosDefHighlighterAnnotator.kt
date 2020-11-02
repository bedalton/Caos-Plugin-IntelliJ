@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.util.PsiTreeUtil

class CaosDefHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element !is CaosDefCompositeElement)
            return
        when {
            element is CaosDefComment -> CaosDefSyntaxHighlighter.DOC_COMMENT
            element is CaosDefWordLink -> {
                val attributeKey = if (element.hasParentOfType(CaosDefCodeBlock::class.java))
                    CaosDefSyntaxHighlighter.CODE_BLOCK
                else if (element.hasParentOfType(CaosDefDocComment::class.java))
                    CaosDefSyntaxHighlighter.DOC_COMMENT_WORD_LINK
                else
                    CaosDefSyntaxHighlighter.VALUES_LIST_WORD_LINK
                addColor(element, annotationHolder, attributeKey)
            }
            element is CaosDefValuesListName -> {
                if (!element.hasParentOfType(CaosDefValuesListElement::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.DOC_COMMENT_TYPE_LINK)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_NAME)
            }
            element is CaosDefValuesListValueKey ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_KEY)
            element is CaosDefValuesListValueName ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_NAME)
            element is CaosDefVariableLink
                -> addColor(element, annotationHolder, CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_LINK)
            element is CaosDefTypeLiteral -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_TYPE)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE)
            }
            element is CaosDefVariableName -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_NAME)
            }
            element is CaosDefCommand -> {
                if (element.parent is CaosDefCommandDefElement)
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMAND, true)
            }
            element is CaosDefBracketString -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.DOC_COMMENT)
                } else {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE)
                }
            }
            element is CaosDefCodeBlock -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.CODE_BLOCK)
            }
            element is CaosDefCommentLineItemMisc -> addColor(element, annotationHolder, CaosDefSyntaxHighlighter.DOC_COMMENT)
            element is CaosDefTypeNoteStatement && element.parent !is CaosDefDocCommentVariableType ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_TYPE)
            else -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    // If in doc comment, return. The following symbol highlighters can be found in comments
                    // but should not be colored
                    return
                }
                when(element.elementType ) {
                    CaosDefTypes.CaosDef_COMMA -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.DOT)
                    CaosDefTypes.CaosDef_OPEN_PAREN -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.PARENTHESES)
                    CaosDefTypes.CaosDef_CLOSE_PAREN -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.PARENTHESES)
                    CaosDefTypes.CaosDef_OPEN_BRACKET -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.BRACKETS)
                    CaosDefTypes.CaosDef_CLOSE_BRACKET -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.BRACKETS)
                    CaosDefTypes.CaosDef_OPEN_BRACE, CaosDefTypes.CaosDef_CLOSE_BRACE -> addColor(element, annotationHolder, DefaultLanguageHighlighterColors.BRACES)
                }
            }
        }
    }

    private fun addColor(element:PsiElement, annotationHolder: AnnotationHolder, color:TextAttributesKey, recursive:Boolean = false) {
        val elements = if (recursive) PsiTreeUtil.collectElementsOfType(element, PsiElement::class.java) + element else listOf(element)
        for(anElement in elements) {
            val annotation = annotationHolder.createInfoAnnotation(anElement, null)
            annotation.textAttributes = color
            annotation.enforcedTextAttributes = color.defaultAttributes
        }
    }
}