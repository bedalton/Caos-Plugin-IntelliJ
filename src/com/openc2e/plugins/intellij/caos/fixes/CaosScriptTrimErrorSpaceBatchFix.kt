package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.utils.CaosStringUtil
import com.openc2e.plugins.intellij.caos.utils.document

class CaosScriptTrimErrorSpaceBatchFix : IntentionAction, LocalQuickFix{

    override fun getFamilyName(): String = CaosBundle.message("caos.fixes.family-name")
    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file !is CaosScriptFile)
            return false
        val text = file.text
        return text != CaosStringUtil.sanitizeCaosString(text)
    }

    override fun getText(): String = CaosBundle.message("caos.fixes.stip-error-spaces")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null)
            return
        stripSpacesAndApply(file)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement as? CaosScriptFile
                ?: descriptor.psiElement?.containingFile as? CaosScriptFile
                ?: return
        stripSpacesAndApply(file)
    }

    private fun stripSpacesAndApply(file:PsiFile) {
        val text = file.text
        val trimmedText = CaosStringUtil.sanitizeCaosString(text)
        if (text == trimmedText)
            return
        file.document?.replaceString(0, file.textLength, trimmedText)
    }

    companion object {
        val HIGHLIGHT_DISPLAY_KEY = HighlightDisplayKey.findOrRegister("TrimErrorSpaces", CaosBundle.message("caos.fixes.stip-error-spaces"))
    }
}