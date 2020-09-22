package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

<<<<<<< Updated upstream
=======
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
>>>>>>> Stashed changes
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
<<<<<<< Updated upstream
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
=======
>>>>>>> Stashed changes

object CaosScriptExpandCommasIntentionAction : IntentionAction, LocalQuickFix {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")



    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile
    }

    override fun getText(): String = CaosBundle.message("caos.intentions.commands-on-new-line")

    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        invoke(project, fileIn)
    }

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val file = problemDescriptor.psiElement.containingFile
                ?: return
        invoke(project, file)
    }

<<<<<<< Updated upstream
    private fun invoke(project: Project, fileIn: PsiFile?) {
        val file = fileIn ?: return
        var document = PsiDocumentManager.getInstance(project).getCachedDocument(file) ?: fileIn.document
=======
    fun invoke(project: Project, fileIn: PsiFile?) {

        val file = fileIn as? CaosScriptFile ?: return
        val variant = file.variant
        var document = fileIn.document ?: PsiDocumentManager.getInstance(project).getCachedDocument(file)
>>>>>>> Stashed changes
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        val newLines = PsiTreeUtil.collectElementsOfType(file, com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline::class.java)
        for (newLine in newLines) {
            replaceIfBlankOrComma(variant, newLine)
        }
        document = file.document ?: PsiDocumentManager.getInstance(project).getCachedDocument(file)
        document?.let {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
        }
<<<<<<< Updated upstream
        CodeStyleManager.getInstance(project).reformat(fileIn, true)
    }

    private fun replaceIfBlankOrComma(next:PsiElement?) {
=======
        //CodeStyleManager.getInstance(project).reformat(file, true)
        //file.variant = variant
    }

    private fun replaceIfBlankOrComma(variant:CaosVariant?, next: PsiElement?) {
>>>>>>> Stashed changes
        if (next == null)
            return
        if (next.text.trim(' ', '\n', ',').isEmpty() || next.text == ",")
            next.replace(CaosScriptPsiElementFactory.newLine(next.project))
    }

}