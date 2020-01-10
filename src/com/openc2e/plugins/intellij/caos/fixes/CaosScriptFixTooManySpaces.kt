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

class CaosScriptFixTooManySpaces(private val spaces: PsiElement) : IntentionAction {
    private val spacesText = spaces.text
    private val SPACES_BEFORE_NEW_LINE = "[ \t]+\n".toRegex()
    private val TOO_MANY_SPACES = "\t|[ ][ \t]+".toRegex()
    private val BAD_SIBLING = "\\s+|\\s*,\\s*".toRegex()

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
        val previousSiblingText = spaces.previous?.text
        val nextSiblingText = spaces.next?.text
        val replacement = when {
            (previousSiblingText == null || BAD_SIBLING.matches(previousSiblingText)) -> {
                if (previousSiblingText == null)
                    ""
                else if (spacesText.contains(","))
                    ","
                else
                    ""
            }
            (nextSiblingText == null || BAD_SIBLING.matches(nextSiblingText))  -> {
                if (nextSiblingText == null)
                    ""
                else if (spacesText.contains(","))
                    ","
                else
                    ""
            }
            SPACES_BEFORE_NEW_LINE.matches(spacesText) -> "\n"
            TOO_MANY_SPACES.matches(spacesText) -> ""
            else -> return
        }
        val range = spaces.textRange
        file.document?.replaceString(range.startOffset, range.endOffset, replacement)
    }

}