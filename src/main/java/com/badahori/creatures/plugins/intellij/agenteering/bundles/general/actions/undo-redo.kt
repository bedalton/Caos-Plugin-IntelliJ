package com.badahori.creatures.plugins.intellij.agenteering.bundles.general.actions

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.common.util.className
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project

abstract class UndoableQuickFix : LocalQuickFix {

    override fun startInWriteAction(): Boolean {
        return true
    }

    abstract fun undoFix(project: Project, descriptor: ProblemDescriptor)

    abstract fun doFix(project: Project, descriptor: ProblemDescriptor)

    open fun getUndoRedoGroupId(): String? = null

    open fun canApply(project: Project, descriptor: ProblemDescriptor): Boolean {
        return true
    }

    open fun getUndoConfirmationPolicy(): UndoConfirmationPolicy {
        return UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
    }

    final override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        if (canApply(project, descriptor)) {
            run(project, descriptor)
        }
    }

    private fun run(project: Project, descriptor: ProblemDescriptor) {

        if (!ApplicationManager.getApplication().isDispatchThread) {
            LOGGER.severe("Is not Dispatch thread")
            return
        }

        val file = descriptor.psiElement?.containingFile?.virtualFile
            ?: descriptor.psiElement?.originalElement?.virtualFile
            ?: descriptor.startElement?.containingFile?.virtualFile
            ?: descriptor.startElement?.originalElement?.containingFile?.virtualFile

        if (file == null) {
            LOGGER.severe("VirtualFile is null in undoable fix in class: ${this.className}")
            return
        }
        if (project.isDisposed) {
            return
        }

        runWriteAction {
            val undoableAction = object : BasicUndoableAction(file) {

                override fun undo() {
                    if (project.isDisposed) {
                        return
                    }
                    runWriteAction {
                        undoFix(project, descriptor)
                    }
                }

                override fun redo() {
                    if (project.isDisposed) {
                        return
                    }
                    runWriteAction {
                        doFix(project, descriptor)
                    }
                }
            }
            val groupId = getUndoRedoGroupId()
            var action = WriteCommandAction.writeCommandAction(project)
                .withName(name)
                .withUndoConfirmationPolicy(getUndoConfirmationPolicy())
            if (groupId != null)
                action = action.withGroupId(groupId)
            runWriteAction {
                action.run<Exception> write@{
                    if (project.isDisposed) {
                        return@write
                    }
                    UndoManager.getInstance(project).undoableActionPerformed(undoableAction)
                }
            }

            undoableAction.redo()
        }
    }

}
