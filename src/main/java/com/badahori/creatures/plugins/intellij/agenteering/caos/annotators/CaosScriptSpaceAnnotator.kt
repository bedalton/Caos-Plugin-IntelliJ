package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.startOffset

class CaosScriptSpaceAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
       try {
           annotateElement(element, holder)
       } catch (e: Exception) {
           LOGGER.severe("Failed to annotate space: ${e.message ?: ""}")
           e.printStackTrace();
       }
    }

    private fun annotateElement(element: PsiElement, holder: AnnotationHolder) {
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
                val endOffset = element.startOffset + 1
                var elementToAnnotate: PsiElement = element
                while (elementToAnnotate.endOffset < endOffset) {
                    elementToAnnotate = elementToAnnotate.parent
                        ?: return
                }
                if (element.endOffset < endOffset) {
                    return
                }
                holder.newErrorAnnotation("missing whitespace character")
                    .range(TextRange.create(element.startOffset, endOffset))
                    .withFix(CaosScriptInsertBeforeFix("Insert space", "", element, ' '))
                    .create()
            }
            return
        }
        val variant = element.variant.nullIfUnknown()
            ?: return

        if (variant.isNotOld) {
            if (element.textContains(',')) {
                annotateC2eCommaError(element, holder)
            }
            return
        }

        // Is CAOS2, too spaces will be stripped automatically
        if ((element.containingFile as? CaosScriptFile)?.hasCaos2Tags != false) {
            return
        }
        annotateExtraSpaces(element, annotationHolder = holder)
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


        if (element.elementType == CaosScript_COMMA) {
            // see if next non-empty sibling is an argument, if so, ask to replace comma with a space
            element.getNextNonEmptySibling(false)?.let { next ->
                if (!canFollowComma(next)) {
                    var annotation =
                        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.unexpected-comma"))
                            .range(element)
                    annotation = annotation
                        .withFix(CaosScriptReplaceElementFix(element, " ", "Replace comma with space", true))
                    annotation.create()
                    return
                }
            }
        }


        // Get previous non-space element to check if it is a newline
        val elementBeforeSpaces = WhitespacePsiUtil.getElementBeforeSpaces(element)
            ?: return

        // This preceding run of spaces is start of line
        if (elementBeforeSpaces.tokenType == CaosScript_NEWLINE || elementBeforeSpaces.text.contains('\n')) {
            return
        }

        if (element.textLength == 1) {
            return
        }
        // Only mark self if previous is space-like character
        // Is start of line, so cannot be a multiple whitespace that matters
        // Trailing whitespace is dealt with in another inspection
        val elementAfterSpaces = WhitespacePsiUtil.getElementAfterSpaces(element)
            ?: return
        // Check if this space is trailing
        if (elementAfterSpaces.tokenType == CaosScript_NEWLINE || elementAfterSpaces.text.contains('\n')) {
            return
        }

        val range = element.textRange.let {
            TextRange.create(it.startOffset + 1, it.endOffset)
        }

        // Error affects only one element at a time now
        // So only mark self
        annotationHolder.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
            .range(range)
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
