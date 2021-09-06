package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptFixTooManySpaces
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertSpaceFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptTrimErrorSpaceBatchFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.CaosScript_COMMA
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.CaosScript_NEWLINE
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getNextNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.isDirectlyPrecededByNewline
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE

class CaosScriptSpaceAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val type = element.tokenType
        if (type != WHITE_SPACE && type != CaosScript_COMMA && type != CaosScript_NEWLINE)
            return
        val variant = element.variant.nullIfUnknown()
            ?: return
        if (element.textContains(',')) {
            if (variant.isNotOld)
                annotateC2eCommaError(element, holder)
            else
                annotateExtraSpaces(element, annotationHolder = holder)
        } else {
            // Spacing does not matter in CV+, so return
            if (variant.isOld)
                annotateExtraSpaces(element, annotationHolder = holder)
            else
                return
        }
    }

    private fun annotateC2eCommaError(element: PsiElement, holder: AnnotationHolder) {
        holder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-command-in-c2e"))
            .range(element)
            .create()
    }

    /**
     * Annotates spacing errors, mostly multiple whitespaces within a command
     */
    private fun annotateExtraSpaces(element: PsiElement, annotationHolder: AnnotationHolder) {
        if (element is PsiComment)
            return
        // Get this elements text
        val text = element.text
        // Get text before and after this space
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""
        if (text.contains(',')) {
            element.getNextNonEmptySibling(false)?.let { next ->
                if (!canFollowComma(next)) {
                    var annotation =
                        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.unexpected-comma"))
                            .range(element)

                    if (prevText.isNotBlank()) {
                        annotation = annotation
                            .withFix(CaosScriptReplaceElementFix(element, " ", "Replace comma with space", true))
                    }

                    annotation.create()
                    return
                }
            }
        }

        // Test if previous element is a comma or space
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)

        // Is a single space, and not followed by terminating comma or newline
        if (text.length == 1 && !(COMMA_NEW_LINE_REGEX.matches(nextText) || previousIsCommaOrSpace))
            return

        val next = element.next

        var prev: PsiElement? = element.previous
        while (prev != null) {
            if (prev.tokenType != WHITE_SPACE && prev.tokenType != CaosScript_NEWLINE)
                break
            if (!prev.textContains('\n'))
                prev = prev.previous
            else
                break
        }
        if (next == null || prev == null) {
            return
        }


        // Psi element is empty, denoting a missing space, possible after quote or byte-string or number
        if (text.isEmpty()) {
            val toMark = TextRange(element.startOffset - 1, next.startOffset + 1)
            annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.missing-space"))
                .range(toMark)
                .withFix(CaosScriptInsertSpaceFix(next))
                .create()
            return
        }
        // Did check for trailing comma, but this is assumed to be removed before injection
        // I think BoBCoB does this and CyberLife CAOS tool strips this as well.
        if (nextText.startsWith("\n") || element.node.isDirectlyPrecededByNewline()) {// && variant != CaosVariant.C1) {
            return
        }
        val errorTextRange = element.textRange.let {
            if (element.text.length > 1)
                TextRange(it.startOffset + 1, it.endOffset)
            else
                it
        }
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
            .range(errorTextRange)
            .withFix(CaosScriptFixTooManySpaces(element))
            .newFix(CaosScriptTrimErrorSpaceBatchFix())
            .range(element.containingFile.textRange)
            .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
            .registerFix()
            .create()
        return
    }

//
//    private fun annotateTrailingWhiteSpace(
//        element: PsiElement,
//        annotationHolder: AnnotationHolder
//    ) {
//        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-trailing-whitespace"))
//            .range(element)
//            .withFix(CaosScriptFixTooManySpaces(element))
//            .create()
//    }

    companion object {
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
        private val COMMA_NEW_LINE_REGEX = "([,]|\n)+".toRegex()

    }

}

private fun canFollowComma(element: PsiElement?): Boolean {
    if (element == null)
        return false
    return element !is CaosScriptArgument
//    if (element is CaosScriptArgument)
//        return false
////    if (element is CaosScriptEqualityExpression)
////        return true
//    return element is CaosScriptCodeBlockLine ||
//            element is CaosScriptCommandLike ||
//            element is CaosScriptHasCodeBlock ||
//            element is CaosScriptCommandElement ||
//            element is CaosScriptCodeBlock ||
//            element is CaosScriptHasCodeBlock ||
//            element.parent.let {
//                it is CaosScriptCommandLike ||
//                        it is CaosScriptIsCommandKeywordToken ||
//                        it is CaosScriptIsRvalueKeywordToken ||
//                        it is CaosScriptIsLvalueKeywordToken
//            }
////    return element is CaosScriptCRepe ||
////            element is CaosScriptCEndi ||
////            element is CaosScriptCEndm ||
////            element is CaosScriptCNext ||
////            element is CaosScriptCNscn ||
////            element is CaosScriptRetnKw ||
////            element is CaosScriptCRetn ||
////            element is CaosScriptEnumHeaderCommand ||
////            element is CaosScriptRepsHeader ||
////            element is CaosScriptSubroutineHeader ||
////            element is CaosScriptEscnHeader
}