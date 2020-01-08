package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEnumSceneryStatement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEqualityExpressionPlus

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if(DumbService.isDumb(element.project))
            return
        when (element) {
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(element, annotationHolder)
        }
    }

    private fun annotateEqualityExpressionPlus(element: CaosScriptEqualityExpressionPlus, annotationHolder: AnnotationHolder) {
        val variant = element.containingCaosFile?.variant
        if (variant !in VARIANT_OLD )
            return
        annotationHolder.createErrorAnnotation(element, CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_plus_expression", variant?:"C1/C2"))
    }


    private fun annotateSceneryEnum(element: CaosScriptEnumSceneryStatement, annotationHolder: AnnotationHolder) {
        if (element.containingCaosFile?.variant == "C2")
            return
        annotationHolder.createErrorAnnotation(element.escn, CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
    }

    companion object {
        private val VARIANT_OLD = listOf("C1", "C2")
    }

}