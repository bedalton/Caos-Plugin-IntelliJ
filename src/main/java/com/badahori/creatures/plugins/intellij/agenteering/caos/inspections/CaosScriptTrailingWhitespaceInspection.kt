package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptFixTooManySpaces
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType

class CaosScriptTrailingWhitespaceInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.trailing-white-space.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.trailing-white-space.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitElement(element: PsiElement?) {
                super.visitElement(element)
                if (element == null)
                    return
                validate(element, holder)
            }
        }
    }
    /**
     * Annotates spacing errors, mostly multiple whitespaces within a command
     */
    private fun validate(element: PsiElement, holder: ProblemsHolder) {
        val type = element.tokenType
        if (type != CaosScriptTypes.CaosScript_COMMA && type != TokenType.WHITE_SPACE && type != CaosScriptTypes.CaosScript_NEWLINE)
            return

        if (element.variant?.isOld != true)
            return

        if (element is PsiComment)
            return

        // Get this elements text
        val text = element.text
        // Get text before and after this space
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""
        if (text.contains(',')) {
            element.getPreviousNonEmptySibling(false)?.let { previous ->
                if ((previous as? CaosScriptCodeBlockLine)?.commandCall == null) {
                    holder.registerProblem(
                        element,
                        CaosBundle.message("caos.annotator.syntax-error-annotator.unexpected-comma"),
                        CaosScriptReplaceElementFix(element, " ", "Replace comma with space", true)
                    )
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

        // Find if previously on this line there is a newline
        var prev: PsiElement? = element.previous
        while(prev != null) {
            if (prev.tokenType != TokenType.WHITE_SPACE && prev.tokenType != CaosScriptTypes.CaosScript_NEWLINE)
                break
            if (!prev.textContains('\n'))
                prev = prev.previous
            else
                break
        }

        // Trailing whitespace
        if (!(element.text.contains('\n') || prev?.textContains('\n').orFalse()) || next == null || prev == null) {
            holder.registerProblem(
                element,
                CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-trailing-whitespace"),
                CaosScriptFixTooManySpaces(element)
            )
            return
        }
//
//
//        // Psi element is empty, denoting a missing space, possible after quote or byte-string or number
//        if (text.isEmpty()) {
//            val toMark = TextRange(element.startOffset - 1, next.startOffset + 1)
//            holder.registerProblem(
//                element,
//                toMark,
//                CaosBundle.message("caos.annotator.syntax-error-annotator.missing-space"),
//                CaosScriptInsertSpaceFix(next)
//            )
//            return
//        }
//        // Did check for trailing comma, but this is assumed to be removed before injection
//        // I think BoBCoB does this and CyberLife CAOS tool strips this as well.
//        if (nextText.startsWith("\n") || element.node.isDirectlyPrecededByNewline()) {// && variant != CaosVariant.C1) {
//            return
//        }
//        val errorTextRange = element.textRange.let {
//            if (element.text.length > 1)
//                TextRange(it.startOffset + 1, it.endOffset)
//            else
//                it
//        }
//        holder.registerProblem(
//            element,
//            errorTextRange,
//            CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"),
//            CaosScriptFixTooManySpaces(element)
//        )
    }


    companion object {
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
        private val COMMA_NEW_LINE_REGEX = "([,]|\n)+".toRegex()

    }
}