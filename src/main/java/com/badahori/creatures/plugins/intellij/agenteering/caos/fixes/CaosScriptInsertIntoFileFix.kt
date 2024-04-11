package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

class CaosScriptInsertIntoFileFix(
    psiFile: PsiFile,
    private val fixText: String,
    private val elementText: String,
    private val offsetInFile: Int = psiFile.endOffset,
    private val postInsert: ((editor:Editor) -> Unit)? = null,
) : LocalQuickFix, IntentionAction {

    private val filePointer = SmartPointerManager.createPointer(psiFile)

    override fun getFamilyName(): String = CAOSScript


    override fun getText(): String = fixText

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return filePointer.element != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null) {
            return
        }
        invoke(editor, file)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement?.containingFile
            ?: descriptor.psiElement?.originalElement?.containingFile
            ?: return
        invoke(element.editor, element)
    }

    private fun invoke(editor: Editor?, element:PsiFile) {
        if (editor == null)
            return
        EditorUtil.insertText(editor, "$elementText ", offsetInFile, true)
        postInsert?.invoke(editor)
    }
}