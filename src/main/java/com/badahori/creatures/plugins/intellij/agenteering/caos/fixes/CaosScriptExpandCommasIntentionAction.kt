package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes


import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandCall
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacro
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orTrue
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Object class for expanding commas and newline-like spaces into newlines
 */
object CaosScriptExpandCommasIntentionAction : IntentionAction, LocalQuickFix {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is CaosScriptFile
    }

    override fun getText(): String = CaosBundle.message("caos.intentions.commands-on-new-line")

    /**
     * Invokes this fix, expanding newline-like elements into newlines
     */
    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        invoke(project, fileIn)
    }

    /**
     * Applies fix to a given element
     */
    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val file = problemDescriptor.psiElement.containingFile
                ?: return
        invoke(project, file)
    }

    /**
     * Expands all commas and new-line-like spaces into newline
     */
    fun invoke(project: Project, fileIn: PsiFile?) {
        if (fileIn == null)
            return
        if (CommandProcessor.getInstance().isUndoTransparentActionInProgress) {
            try {
                runnable(project, fileIn)
            } catch(e:Exception) {
                LOGGER.severe("Failed to run expandCommas on '${fileIn.name}' in existing transparent action with error: ${e.message}")
            }
        } else {
            CommandProcessor.getInstance().runUndoTransparentAction {
                try {
                    runnable(project, fileIn)
                } catch(e:Exception) {
                    LOGGER.severe("Failed to run expandCommas on '${fileIn.name}' in runUndoTransparentAction with error: ${e.message}")
                }
            }
        }
    }

    /**
     * Method to expand commas and spaces into newlines
     */
    private fun runnable(project: Project, fileIn: PsiFile?) : Boolean {
        val file = fileIn as? CaosScriptFile ?: return false
        val variant = file.variant

        // Check that there are more than two commands.
        // If not, no need to try to separate them by newlines
        val numCommands = PsiTreeUtil.collectElementsOfType(file, CaosScriptCommandCall::class.java).size
        if (shouldNotCollapse(file)) {
            return true
        }

        // Get this document
        var document = fileIn.document ?: PsiDocumentManager.getInstance(project).getCachedDocument(file)

        // If document is not null, commit it so we can alter it
        if (document != null) {
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        // Get all possible newline elements
        val newLines = PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java)
        // Replace newline like elements
        for (newLine in newLines) {
            replaceIfBlankOrComma(newLine)
        }
        // Get the document again, and this time commit it
        document = file.document ?: PsiDocumentManager.getInstance(project).getCachedDocument(file)
        document?.let {
            file.variant = variant
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it)
        }
        // Return success only if there are enough newlines to cover the elements, ignoring enum blocks
        return document?.text.orEmpty().count { it =='\n'} > numCommands - 1
    }

    private fun shouldNotCollapse(file: PsiFile) : Boolean {
        if (PsiTreeUtil.collectElementsOfType(file, CaosScriptCommandCall::class.java).size > 1)
            return false
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptHasCodeBlock::class.java)
                .nullIfEmpty()
                ?.none { it !is CaosScriptMacro }
                .orTrue()
    }

    /**
     * Replace a newline-like (comma, space or newline) element with an actual new line element
     */
    private fun replaceIfBlankOrComma(next: PsiElement?) {
        if (next == null)
            return
        val text = next.text
        if (NEEDS_NEWLINE.matches(text))
            next.replace(CaosScriptPsiElementFactory.newLine(next.project))
        else {
            // If psi element is not simply commas and spaces, it must have a newline
            // Count newlines so as not to replace existing ignoring commas
            val newLines = text.count { it == '\n' }
            if (newLines < 1)
                return
            // Actually replace this element with its commas
            next.replace(CaosScriptPsiElementFactory.newLines(next.project, newLines))
        }
    }

    private val NEEDS_NEWLINE = "^([ ,]+)$".toRegex()

}