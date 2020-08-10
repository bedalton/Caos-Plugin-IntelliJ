@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.hasParentOfType
import com.intellij.psi.util.PsiTreeUtil

class CaosDefHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element !is CaosDefCompositeElement)
            return
        when(element) {
            is CaosDefWordLink -> {
                val attributeKey = if (element.hasParentOfType(CaosDefDocComment::class.java))
                    CaosDefSyntaxHighlighter.WORD_LINK
                else
                    CaosDefSyntaxHighlighter.VALUES_LIST_WORD_LINK
                addColor(element, annotationHolder, attributeKey)
            }
            is CaosDefValuesListName -> {
                if (!element.hasParentOfType(CaosDefValuesListElement::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.TYPE_LINK)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_NAME)
            }
            is CaosDefValuesListValueKey ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_KEY)
            is CaosDefVariableLink
                -> addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_LINK)
            is CaosDefTypeLiteral -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT_VARIABLE_TYPE)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE)
            }
            is CaosDefVariableName -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_NAME)
            }
            is CaosDefCommand -> {
                if (element.parent is CaosDefCommandDefElement)
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMAND, true)
            }
            is CaosDefBracketString -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT)
                } else {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE)
                }
            }
            is CaosDefCodeBlock -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.CODE_BLOCK)
            }
            is CaosDefCommentLineItemMisc -> addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT)
            else -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    if (element is LeafPsiElement)
                        addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT_TAG)
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