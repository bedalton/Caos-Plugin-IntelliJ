package com.openc2e.plugins.intellij.caos.def.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.openc2e.plugins.intellij.caos.def.psi.api.*
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType

class CaosDefHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element !is CaosDefCompositeElement)
            return;
        when(element) {
            is CaosDefWordLink->addColor(element, annotationHolder, CaosDefSyntaxHighlighter.WORD_LINK);
            is CaosDefTypeDefName -> {
                if (!element.hasParentOfType(CaosDefTypeDefinitionElement::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.TYPE_LINK)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.TYPE_DEF_NAME)
            }
            is CaosDefTypeDefinitionKey ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.TYPE_DEF_KEY)
            is CaosDefVariableLink
                -> addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_LINK)
            is CaosDefTypeLiteral -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java))
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT)
                else
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE);
            }
            is CaosDefVariableName -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_NAME);
            }
            is CaosDefCommand -> {
                if (element.parent is CaosDefCommandDefElement)
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMAND);
            }
            is CaosDefBracketString -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT);
                } else {
                    addColor(element, annotationHolder, CaosDefSyntaxHighlighter.VARIABLE_TYPE);
                }
            }
            is CaosDefCodeBlock -> {
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.CODE_BLOCK);
            }
            is CaosDefDocComment ->
                addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT);
            else -> {
                if (element.hasParentOfType(CaosDefDocComment::class.java)) {
                    if (element is LeafPsiElement)
                        addColor(element, annotationHolder, CaosDefSyntaxHighlighter.COMMENT_STATEMENT);
                }

            }
        }
    }

    private fun addColor(element:PsiElement, annotationHolder: AnnotationHolder, color:TextAttributesKey) {
        annotationHolder.createInfoAnnotation(element, "").textAttributes = color;
    }
}