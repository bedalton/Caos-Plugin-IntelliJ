package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCLoop
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLoopStatement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager

class CaosScriptEmptyLoopInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.empty-loop.display-name")
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = CaosBundle.message("caos.inspection.empty-loop.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitLoopStatement(o: CaosScriptLoopStatement) {
                super.visitLoopStatement(o)
                validate(o, holder)
            }
        }
    }

    private fun validate(loop: CaosScriptLoopStatement, problemsHolder: ProblemsHolder) {
        if (loop.loopTerminator == null)
            return
        if (loop.variant?.isOld != true) {
            return
        }
        val isEmpty = loop.codeBlock
            ?.codeBlockLineList
            .orEmpty()
            .none { it.comment == null }
        if (!isEmpty)
            return
        val error = CaosBundle.message("caos.inspection.empty-loop.error-message")
        problemsHolder.registerProblem(
            loop.loopTerminator?.let { it.cEver ?: it.cUntl } ?: loop.lastChild,
            error,
            InsertWait1Fix(loop.cLoop)
        )
    }
}

private class InsertWait1Fix(loopKeyword: CaosScriptCLoop? = null) : LocalQuickFix, IntentionAction {

    val loopKeywordPointer = if (loopKeyword != null) SmartPointerManager.createPointer(loopKeyword) else null

    override fun getName(): String = "Insert 'wait 1' into empty loop"
    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = "Insert 'wait 1' into empty loop"

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return loopKeywordPointer
            ?.element
            ?.getParentOfType(CaosScriptLoopStatement::class.java)
            ?.codeBlock
            ?.codeBlockLineList
            .orEmpty()
            .none { it.comment == null }
            .orFalse()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val loopKeyword = loopKeywordPointer?.element
            ?: return
        val editorNonNull = editor ?: loopKeyword.editor
            ?: return
        applyFix(project, editorNonNull, loopKeyword)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val loop = descriptor.psiElement
            ?.getSelfOrParentOfType(CaosScriptLoopStatement::class.java)
            ?.cLoop
            ?: return

        val editor = loop.editor
            ?: return
        applyFix(project, editor, loop)
    }


    private fun applyFix(project: Project, editor: Editor, loop: CaosScriptCLoop) {
        val pointer = SmartPointerManager.createPointer(loop)
        val numLines = loop.parent?.text.orEmpty().split("[\n\r]+".toRegex()).size
        val start = if (numLines > 1)
            '\n'
        else
            ' '

        EditorUtil.insertText(editor, "${start}wait 1", loop.endOffset, true)
        val element = pointer.element
            ?: return
        CodeStyleManager.getInstance(project).reformat(element.parent, false)
    }

}