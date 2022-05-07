package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.rd.util.string.print

/**
 * Object class for expanding commas and newline-like spaces into newlines
 */
object CaosScriptExpandCommasIntentionAction : PsiElementBaseIntentionAction(), IntentionAction, LocalQuickFix {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CAOSScript

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.containingFile is CaosScriptFile
    }

    override fun getText(): String = CaosBundle.message("caos.intentions.commands-on-new-line")

    /**
     * Invokes this fix, expanding newline-like elements into newlines
     */
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = element.containingFile as? CaosScriptFile
            ?: return
        invoke(project, file)
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
                runWriteAction {
                    runnable(project, fileIn)
                }
            } catch (e: Exception) {
                LOGGER.severe("Failed to run expandCommas on '${fileIn.name}' in existing transparent action with error: ${e.message}")
                e.printStackTrace()
            }
        } else {
            CommandProcessor.getInstance().runUndoTransparentAction {
                try {
                    runWriteAction {
                        runnable(project, fileIn)
                    }
                } catch (e: Exception) {
                    LOGGER.severe("Failed to run expandCommas on '${fileIn.name}' in runUndoTransparentAction with error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Method to expand commas and spaces into newlines
     */
    private fun runnable(project: Project, fileIn: PsiFile?): Boolean {
        val file = fileIn as? CaosScriptFile
            ?: return false
        val variant = fileIn.variant
            .nullIfUnknown()

        if (variant == null) {
            LOGGER.severe("Cannot quick format without variant")
            return false
        }
        // Check that there are more than two commands.
        // If not, no need to try to separate them by newlines
        if (shouldNotCollapse(file)) {
            return true
        }

        // Commit it so we can alter it
        fileIn.document?.let { document ->
            val manager = PsiDocumentManager.getInstance(project)
            manager.doPostponedOperationsAndUnblockDocument(document)
            manager.commitDocument(document)
        }

        // Get all possible newline elements
        PsiTreeUtil.collectElementsOfType(file, PsiWhiteSpace::class.java)
            .filter { whiteSpace ->
                if (whiteSpace.textContains('\n'))
                    return@filter false
                whiteSpace.next?.let { next ->
                    next is CaosScriptCodeBlockLine || next is CaosScriptCommandCallLike || next is CaosScriptCodeBlock || next is CaosScriptHasCodeBlock || next is CaosScriptScriptBodyElement
                } ?: false
            }
            .filterNot filter@{ whiteSpace ->
                var prev = whiteSpace.previous
                while (prev is PsiWhiteSpace && !prev.textContains('\n'))
                    prev = prev.previous
                if (prev != null && prev.textContains('\n'))
                    return@filter true
                var next = whiteSpace.next
                while (next is PsiWhiteSpace && !next.textContains('\n'))
                    next = next.previous
                next == null || next.textContains('\n')
            }
            .map {
                SmartPointerManager.createPointer(it)
            }
            .forEach { it.element?.replace(CaosScriptPsiElementFactory.newLine(project)) }


        val manager = PsiDocumentManager.getInstance(project)
        fileIn.document?.let { document ->
            manager.doPostponedOperationsAndUnblockDocument(document)
            manager.commitDocument(document)
        }

        PsiTreeUtil.collectElementsOfType(file, PsiWhiteSpace::class.java)
            .filter { it.textContains(',') }
            .map { SmartPointerManager.createPointer(it) }
            .forEach { it.element?.replace(CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(project)) }

        val readonly = (file.document?.isWritable == false)
        // Get the document again, and this time commit it
        file.document?.let { document ->
            document.setReadOnly(false)
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            manager.commitDocument(document)
        }
        if (!file.isValid || file.isInvalid || file.firstChild?.isValid != true) {
            return false
        }
        try {
            CodeStyleManager.getInstance(project).reformat(file)
            file.document?.setReadOnly(readonly)
        } catch (e: Exception) {
            LOGGER.severe("Reformat threw error: ${e.message}")
            e.printStackTrace()
            return false
        } catch (e: Error) {
            LOGGER.severe("Reformat threw ERROR not exception: ${e.message}")
            e.printStackTrace()
            return false
        }
        // Return success only if there are enough newlines to cover the elements, ignoring enum blocks
        return true
    }

    private fun shouldNotCollapse(file: PsiFile): Boolean {
        if (!file.isWritable) {
            LOGGER.warning("Cannot flatten readonly file: ${file.name}")
            (Exception()).printStackTrace()
            return false
        }
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