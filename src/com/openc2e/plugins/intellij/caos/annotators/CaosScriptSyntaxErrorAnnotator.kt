package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptFixTooManySpaces
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptTrimErrorSpaceBatchFix
import com.openc2e.plugins.intellij.caos.fixes.TransposeEqOp
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.next
import com.openc2e.plugins.intellij.caos.psi.util.previous
import com.openc2e.plugins.intellij.caos.utils.orFalse

class CaosScriptSyntaxErrorAnnotator : Annotator {

    private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()


    override fun annotate(elementIn: PsiElement, annotationHolder: AnnotationHolder) {
        if(DumbService.isDumb(elementIn.project))
            return
        val element = elementIn as? CaosScriptCompositeElement
                ?: return
        val variant = element.containingCaosFile?.variant?.toUpperCase()
                ?: ""
        when (element) {
            is CaosScriptTrailingSpace -> annotateExtraSpaces(element, annotationHolder)
            is CaosScriptSpaceLike -> annotateExtraSpaces(element, annotationHolder)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationHolder)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationHolder)
            is PsiComment -> annotateComment(variant, element, annotationHolder)
        }
    }

    private fun annotateExtraSpaces(element: CaosScriptSpaceLike, annotationHolder: AnnotationHolder) {
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""
        val nextIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(nextText)
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)
        if (element.text.length == 1 && !nextIsCommaOrSpace)
            return
        val errorTextRange = if (element.text.contains("\n") || previousIsCommaOrSpace)
            element.textRange
        else
            TextRange.create(element.textRange.startOffset, element.textRange.endOffset)
        val annotation = annotationHolder.createErrorAnnotation(errorTextRange, CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
        annotation.registerFix(CaosScriptFixTooManySpaces(element))
        annotation.registerBatchFix(CaosScriptTrimErrorSpaceBatchFix(), element.containingFile.textRange, CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
    }

    private fun annotateExtraSpaces(element: CaosScriptTrailingSpace, annotationHolder: AnnotationHolder) {
        var start = element.textRange.startOffset-1
        if (start < 0)
            start = 0
        val errorTextRange= TextRange.create(start, element.textRange.endOffset)
        val annotation = annotationHolder.createErrorAnnotation(errorTextRange, CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
        annotation.registerFix(CaosScriptFixTooManySpaces(element))
        annotation.registerBatchFix(CaosScriptTrimErrorSpaceBatchFix(), element.containingFile.textRange, CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
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