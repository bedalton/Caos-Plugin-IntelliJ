package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptFixTooManySpaces
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.CaosScript_NEWLINE
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.WhitespacePsiUtil.getElementAfterSpaces
import com.badahori.creatures.plugins.intellij.agenteering.utils.WhitespacePsiUtil.getElementBeforeSpaces
import com.badahori.creatures.plugins.intellij.agenteering.utils.WhitespacePsiUtil.isNewline
import com.badahori.creatures.plugins.intellij.agenteering.utils.WhitespacePsiUtil.isNotWhitespace
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

/**
 * Detects whitespace trailing after the end of line
 */
class CaosScriptTrailingWhitespaceInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.trailing-white-space.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.trailing-white-space.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                validate(element, holder)
            }
        }
    }

    /**
     * Annotates spacing errors, mostly multiple whitespaces within a command
     */
    private fun validate(element: PsiElement, holder: ProblemsHolder) {
        val type = element.tokenType

        if (type != CaosScriptTypes.CaosScript_COMMA && type != TokenType.WHITE_SPACE) {
            return
        }

        // Do not annotate newline
        if (isNewline(element)) {
            return
        }

        if (element.variant?.isOld != true)
            return

        val caosScriptFile = (element.containingFile as? CaosScriptFile)
                ?: return

        // Do not monitor spaces on CAOS2 or CAOS2 supplement
        if (caosScriptFile.hasCaos2Tags || caosScriptFile.isSupplement) {
            return
        }

        // Get where this spaces list ends
        val after = getElementAfterSpaces(element)

        // Make sure these spaces are followed by a newline
        // otherwise they are part of a command or leading spaces
        if (after != null && !isNewline(after)) {
            return
        }

        var textRange: TextRange? = null
        // Error element should be the one before
        var beforeElement = element.getPreviousNonEmptySibling(false)

        // If there is a before non-empty sibling
        if (beforeElement != null) {
            val parent = PsiTreeUtil.findCommonParent(element, beforeElement)
            if (parent != null) {
                val startOffset = beforeElement.endOffset - 1
                val endOffset = after?.startOffset?.minus(1) ?: element.endOffset
                if (startOffset >= parent.endOffset || endOffset <= parent.endOffset) {
                    beforeElement = parent
                    textRange = TextRange(startOffset, endOffset)
                }
            } else {
                beforeElement = null
                textRange = null
            }
        }

        // Error range was not properly found
        if (beforeElement != null && textRange != null) {
            if (beforeElement.endOffset < textRange.endOffset ||
                textRange.startOffset > beforeElement.startOffset) {
                beforeElement = null
                textRange = null
            }
        }
        if (textRange == null || beforeElement == null) {
            beforeElement = element
            textRange = element.textRange
        }

        textRange = beforeElement.textRange.let {
            val startOffsetInParent = textRange!!.startOffset - it.startOffset
            TextRange(startOffsetInParent, startOffsetInParent + textRange!!.length)
        }

        holder.registerProblem(
            beforeElement,
            textRange,
            CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-trailing-whitespace"),
            CaosScriptFixTooManySpaces(element)
        )
    }
}