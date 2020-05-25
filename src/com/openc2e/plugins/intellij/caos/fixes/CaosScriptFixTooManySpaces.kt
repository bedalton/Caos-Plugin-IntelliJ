package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.util.next
import com.openc2e.plugins.intellij.caos.psi.util.previous
import com.openc2e.plugins.intellij.caos.utils.document
import com.openc2e.plugins.intellij.caos.utils.orFalse

class CaosScriptFixTooManySpaces(private val spaces: PsiElement) : IntentionAction {
    private val spacesText = spaces.text
    private val WHITE_SPACE_OR_COMMAS = "[ ,\t]+".toRegex()

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName(): String {
        return "Caos Script"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile
    }

    override fun getText(): String {
        return "Remove extra spaces"
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null)
            return
        var hasComma = false
        var hasNewline = false
        val siblings = mutableListOf<PsiElement>(spaces)
        var sibling = spaces.previous
        while (sibling != null && WHITE_SPACE_OR_COMMAS.matches(sibling.text)) {
            if (sibling.text.contains(","))
                hasComma = true
            else if (sibling.text.contains("\n")) {
                hasNewline = true
                //break
            }
            siblings.add(0, sibling)
            sibling = sibling.previous
        }
        sibling = spaces.next
        while (sibling != null && WHITE_SPACE_OR_COMMAS.matches(sibling.text)) {
            if (sibling.text.contains(","))
                hasComma = true
            else if (sibling.text.contains("\n")) {
                hasNewline = true
                sibling = sibling.next
                break
            }
            siblings.add(sibling)
            sibling = sibling.next
        }
        // Needs this check, as WHITE_SPACE or comma will not catch new lines
        // Check should not match newlines, as it also matches the following tabs and spaces which are valid in c3+
        if (sibling?.text?.contains("\n").orFalse()) {
            hasNewline = true
        }
        if (siblings.isEmpty())
            return
        val hasNext = sibling?.text?.trim()?.isNotEmpty().orFalse()
        val replacement = if (hasNewline)
            ""
        else if (hasComma)
            ","
        else if (hasNext)
            " "
        else
            ""
        file.document?.replaceString(siblings.first().textRange.startOffset, siblings.last().textRange.endOffset, replacement)
    }

}