package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.openc2e.plugins.intellij.caos.fixes.TransposeEqOp
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(elementIn: PsiElement, annotationHolder: AnnotationHolder) {
        if(DumbService.isDumb(elementIn.project))
            return
        val element = elementIn as? CaosScriptCompositeElement
                ?: return
        val variant = element.containingCaosFile?.variant?.toUpperCase()
                ?: ""
        when (element) {
            is CaosScriptSpaceLike -> annotateExtraSpaces(element, annotationHolder)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationHolder)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationHolder)
            is PsiComment -> annotateComment(variant, element, annotationHolder)
        }
    }

    private fun annotateExtraSpaces(element: CaosScriptSpaceLike, annotationHolder: AnnotationHolder) {
        if (element.text.length == 1)
            return
        val errorTextRange = if (element.text.contains("\n"))
            element.textRange
        else
            TextRange.create(element.textRange.startOffset + 1, element.textRange.endOffset)
        val annotation = annotationHolder.createErrorAnnotation(errorTextRange, CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
    }

    private fun annotateComment(variant:String, element: PsiComment, annotationHolder: AnnotationHolder) {
        if (variant != "C1" && variant != "C2")
            return
        annotationHolder.createErrorAnnotation(element, "Comments are not allowed in version less than CV")
    }

    private fun annotateEqualityExpressionPlus(variant:String, element: CaosScriptEqualityExpressionPlus, annotationHolder: AnnotationHolder) {
        if (variant !in VARIANT_OLD)
            return
        annotationHolder.createErrorAnnotation(element, CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_plus_expression", variant?:"C1/C2"))
    }

    private fun annotateNewEqualityOps(variant:String, element: CaosScriptEqOpNew, annotationHolder: AnnotationHolder) {
        if (variant !in VARIANT_OLD)
            return
        annotationHolder.createErrorAnnotation(element, CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .registerFix(TransposeEqOp(element))
    }


    private fun annotateSceneryEnum(variant:String, element: CaosScriptEnumSceneryStatement, annotationHolder: AnnotationHolder) {
        if (element.containingCaosFile?.variant == "C2")
            return
        annotationHolder.createErrorAnnotation(element.escn, CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
    }

    companion object {
        private val VARIANT_OLD = listOf("C1", "C2")
    }

}