package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptFixTooManySpaces
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptTrimErrorSpaceBatchFix
import com.openc2e.plugins.intellij.caos.fixes.TransposeEqOp
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.next
import com.openc2e.plugins.intellij.caos.psi.util.previous
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType

class CaosScriptSyntaxErrorAnnotator : Annotator {



    override fun annotate(elementIn: PsiElement, holder: AnnotationHolder) {
        if(DumbService.isDumb(elementIn.project))
            return
        val annotationHolder:AnnotationHolderWrapper = AnnotationHolderWrapper(holder)
        val element = elementIn as? CaosScriptCompositeElement
                ?: return
        val variant = element.containingCaosFile?.variant?.toUpperCase()
                ?: ""
        when (element) {
            is CaosScriptTrailingSpace -> annotateExtraSpaces(element, annotationHolder)
            is CaosScriptSpaceLike -> annotateExtraSpaces(element, annotationHolder)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationHolder)
            is CaosScriptEnumNextStatement -> annotateBadEnumStatement(variant, element, annotationHolder)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, annotationHolder)
            is CaosScriptCRetn -> annotateRetnCommand(variant, element, annotationHolder)
            is PsiComment -> annotateComment(variant, element, annotationHolder)
        }
    }

    private fun annotateElseIfStatement(variant: String, element: CaosScriptElseIfStatement, annotationHolder: AnnotationHolderWrapper) {
        if (!(variant != "C1" || variant != "C2"))
            return
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.elif-not-available"))
                .range(element.cElif)
                .create()
    }

    private fun annotateExtraSpaces(element: CaosScriptSpaceLike, annotationHolder: AnnotationHolderWrapper) {
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
        val annotation = annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                .range(errorTextRange)
                .withFix(CaosScriptFixTooManySpaces(element))
                .newFix(CaosScriptTrimErrorSpaceBatchFix())
                .range(element.containingFile.textRange)
                .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                .registerFix()
                .create()
    }

    private fun annotateExtraSpaces(element: CaosScriptTrailingSpace, annotationHolder: AnnotationHolderWrapper) {
        var start = element.textRange.startOffset-1
        if (start < 0)
            start = 0
        val errorTextRange= TextRange.create(start, element.textRange.endOffset)
        val annotation = annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                .range(errorTextRange)
                .withFix(CaosScriptFixTooManySpaces(element))
                .newFix(CaosScriptTrimErrorSpaceBatchFix())
                .batch()
                .range(element.containingFile.textRange)
                .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                .registerFix()
                .create()
    }

    private fun annotateComment(variant:String, element: PsiComment, annotationHolder: AnnotationHolderWrapper) {
        if (variant != "C1" && variant != "C2")
            return
        annotationHolder.newErrorAnnotation("Comments are not allowed in version less than CV")
                .range(element)
                .create()
    }

    private fun annotateNewEqualityOps(variant:String, element: CaosScriptEqOpNew, annotationHolder: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .range(element)
                .withFix(TransposeEqOp(element))
                .create()
    }


    private fun annotateSceneryEnum(variant:String, element: CaosScriptEnumSceneryStatement, annotationHolder: AnnotationHolderWrapper) {
        if (variant == "C2")
            return
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
                .range(element.cEscn)
                .create()
    }
    private fun annotateRetnCommand(variant:String, element: CaosScriptCRetn, annotationHolder: AnnotationHolderWrapper) {
        if (!element.hasParentOfType(CaosScriptSubroutine::class.java)) {
            annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.retn-used-outside-of-subr"))
                    .range(element)
                    .create()
        }
        if (!element.hasParentOfType(CaosScriptNoJump::class.java) || variant != "C1")
            return
        annotationHolder.newAnnotation(HighlightSeverity.ERROR, CaosBundle.message("caos.annotator.command-annotator.loop-should-not-be-jumped-out-of"))
                .range(element.textRange)
                .create()
    }

    private fun annotateBadEnumStatement(variant:String, element: CaosScriptEnumNextStatement, annotationHolder: AnnotationHolderWrapper) {
        if (variant != "C1")
            return
        val badElement = element.cEnum
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", badElement.text.toUpperCase()))
                .range(badElement)
                .create()
    }

    companion object {
        private val VARIANT_OLD = listOf("C1", "C2")
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
    }

}