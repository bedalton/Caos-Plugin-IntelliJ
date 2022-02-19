package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptFixTooManySpaces
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType

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
        if (type != CaosScriptTypes.CaosScript_COMMA && type != TokenType.WHITE_SPACE && type != CaosScriptTypes.CaosScript_NEWLINE) {
            return
        }

        if (element.variant?.isOld != true)
            return

        if ((element.containingFile as? CaosScriptFile)?.hasCaos2Tags != false) {
            return
        }
        val previous = element.previous
        if (previous != null && previous !is CaosScriptCodeBlockLine && previous !is CaosScriptScriptElement && previous.tokenType != TokenType.WHITE_SPACE && previous.tokenType != CaosScriptTypes.CaosScript_NEWLINE) {
            return
        }
        // Get this elements text
        val text = element.text
        val next = element.next
        // Get text before and after this space
        val nextText = next?.text ?: ""
        val prevText = element.previous?.text ?: ""

        // Test if previous element is a comma or space
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)

        // Is a single space, and not followed by terminating comma or newline
        if (text.length == 1 && !(COMMA_NEW_LINE_REGEX.matches(nextText) || previousIsCommaOrSpace)) {
            if (next != null) {
                return
            }
        }


        // Find if previously on this line there is a newline
        var prev: PsiElement? = element.previous
        while (prev != null) {
            if (prev.tokenType != TokenType.WHITE_SPACE)
                break
            if (prev.textContains('\n'))
                break
            prev = prev.previous
        }

        // Trailing whitespace
        if (!(element.text.contains('\n') || prev?.text?.endsWith('\n') == true) || next == null || prev == null) {
            holder.registerProblem(
                element,
                CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-trailing-whitespace"),
                CaosScriptFixTooManySpaces(element)
            )
            return
        }
    }


    companion object {
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
        private val COMMA_NEW_LINE_REGEX = "([,]|\n)+".toRegex()

    }
}