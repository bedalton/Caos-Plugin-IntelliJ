package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEnumSceneryStatement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEqualityExpressionPlus

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if(DumbService.isDumb(element.project))
            return
        val file = element as? CaosScriptFile
                ?: return
        val variant = file.variant.toUpperCase()
        when (element) {
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationHolder)
            is PsiComment -> annotateComment(variant, element, annotationHolder)
        }
    }

    private fun annotateComment(variant:String, element: PsiComment, annotationHolder: AnnotationHolder) {
        if (variant != "C1" && variant != "C2")
            return
        annotationHolder.createErrorAnnotation(element, "Comments are not allowed in version less than CV")
    }

    private fun annotateEqualityExpressionPlus(variant:String, element: CaosScriptEqualityExpressionPlus, annotationHolder: AnnotationHolder) {
        val variant = element.containingCaosFile?.variant
        if (variant !in VARIANT_OLD )
            return
        annotationHolder.createErrorAnnotation(element, CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_plus_expression", variant?:"C1/C2"))
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