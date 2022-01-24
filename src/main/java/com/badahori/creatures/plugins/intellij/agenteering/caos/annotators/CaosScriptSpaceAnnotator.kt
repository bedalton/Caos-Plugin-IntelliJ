package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

class CaosScriptSpaceAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val type = element.tokenType
        if (type != WHITE_SPACE && type != CaosScript_COMMA && type != CaosScript_NEWLINE) {
            val previous = element.previous
                ?: return
            val previousType = previous.elementType
                ?: return

            // If is comment prefix, delete it
            if (previousType == CaosScript_CAOS_2_COMMENT_START || previousType == CaosScript_COMMENT_START)
                return
            if (isBracket(previousType, type) || isQuoted(previousType, type) || isQuoted(type, previousType))
                return
            if (previousType != WHITE_SPACE && previousType != CaosScript_COMMA && previousType != CaosScript_NEWLINE) {
                holder.newErrorAnnotation("missing whitespace character")
                    .range(TextRange.create(element.startOffset, element.startOffset + 1))
                    .withFix(CaosScriptInsertBeforeFix("Insert space", "", element, ' '))
                    .create()
            }
            return
        }
        val variant = element.variant.nullIfUnknown()
            ?: return
        if (element.textContains(',')) {
            if (variant.isNotOld)
                annotateC2eCommaError(element, holder)
            else if ((element.containingFile as? CaosScriptFile)?.caos2 == null)
                annotateExtraSpaces(element, annotationHolder = holder)
        } else {
            // Spacing does not matter in CV+, so return
            if (variant.isOld && (element.containingFile as? CaosScriptFile)?.caos2 == null)
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


private fun isBracket(t1: IElementType, t2: IElementType): Boolean {
    if (t1 == CaosScript_BYTE_STRING_R || t2 == CaosScript_BYTE_STRING_R || t1 == CaosScript_ANIM_R || t2 == CaosScript_ANIM_R)
        return true
    if (t1 == CaosScript_OPEN_BRACKET) {
        return t2 == CaosScript_INT ||
                t2 == CaosScript_BYTE_STRING_POSE_ELEMENT ||
                t2 == CaosScript_TEXT_LITERAL ||
                t2 == CaosScript_ANIM_R ||
                t2 == CaosScript_CLOSE_BRACKET
    }
    if (t2 == CaosScript_CLOSE_BRACKET) {
        return t1 == CaosScript_INT ||
                t1 == CaosScript_BYTE_STRING_POSE_ELEMENT ||
                t1 == CaosScript_TEXT_LITERAL ||
                t1 == CaosScript_ANIM_R ||
                t1 == CaosScript_ANIMATION_STRING ||
                t1 == CaosScript_OPEN_BRACKET
    }
    return false
}

private fun isQuoted(t1: IElementType, t2: IElementType): Boolean {

    if (t1 === CaosScript_DOUBLE_QUOTE || t1 === CaosScript_SINGLE_QUOTE) {
        if (t1 == t2)
            return true
        if (t2 === CaosScript_CHAR_CHAR || t2 === CaosScript_STRING_TEXT || t2 === CaosScript_STRING_CHAR || t2 === CaosScript_STRING_ESCAPE_CHAR) {
            return true
        }
    }
    return false
}