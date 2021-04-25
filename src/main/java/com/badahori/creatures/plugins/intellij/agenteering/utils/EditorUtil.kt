@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object EditorUtil {

    fun runWriteAction(writeAction: Runnable, project: Project?) {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            application.runWriteAction { CommandProcessor.getInstance().executeCommand(project, writeAction, null, null, UndoConfirmationPolicy.DEFAULT) }
        } else {
            application.invokeLater { application.runWriteAction { CommandProcessor.getInstance().executeCommand(project, writeAction, null, null, UndoConfirmationPolicy.DEFAULT) } }
        }
    }
    fun runWriteAction(writeAction: Runnable, project: Project?, document: Document) {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            application.runWriteAction { CommandProcessor.getInstance().executeCommand(project, writeAction, null, null, UndoConfirmationPolicy.DEFAULT, document) }
        } else {
            application.invokeLater { application.runWriteAction { CommandProcessor.getInstance().executeCommand(project, writeAction, null, null, UndoConfirmationPolicy.DEFAULT, document) } }
        }
    }

    fun deleteText(document:Document, range: TextRange) {
        runWriteAction {
            document.deleteString(range.startOffset, range.endOffset)
        }
    }

    fun isTextAtOffset(context: InsertionContext, text: String): Boolean {
        if (context.selectionEndOffset == context.document.textLength) {
            return false
        }
        val range = TextRange.create(context.selectionEndOffset, context.selectionEndOffset + text.length)
        return isTextAtOffset(context.document, range, text)
    }

    fun isTextAtOffset(document:Document, startOffset:Int, text: String): Boolean {
        return isTextAtOffset(document, TextRange(startOffset, startOffset + text.length), text)
    }

    fun isTextAtOffset(document:Document, range:TextRange, text: String): Boolean {
        val textAtRange = document.getText(range)
        return textAtRange == text
    }

    fun insertText(insertionContext: InsertionContext, text: String, moveCaretToEnd: Boolean) {
        insertText(insertionContext.editor, text, moveCaretToEnd)
    }

    fun insertText(editor: Editor, text: String, moveCaretToEnd: Boolean) {
        insertText(editor, text, editor.selectionModel.selectionEnd, moveCaretToEnd)
    }

    fun insertText(editor: Editor, text: String, offset: Int, moveCaretToEnd: Boolean) {
        runWriteAction(Runnable {
            editor.document.insertString(offset, text)
            if (moveCaretToEnd) {
                offsetCaret(editor, text.length)
            }
        }, editor.project, editor.document)

    }

    fun insertText(project:Project, document: Document, text: String, offset: Int) {
        runWriteAction(Runnable {
            document.insertString(offset, text)
        }, project, document)
    }

    fun insertText(document: Document, text: String, offset: Int) {
        runWriteAction(Runnable {
            document.insertString(offset, text)
        }, null, document)
    }


    fun replaceText(document: Document, range:TextRange, text: String) {
        runWriteAction {
            document.replaceString(range.startOffset, range.endOffset, text)
        }
    }


    fun replaceText(editor: Editor, range:TextRange, text: String, moveCaretToEnd: Boolean = false) {
        WriteCommandAction.runWriteCommandAction(editor.project) {
            editor.document.replaceString(range.startOffset, range.endOffset, text)
            if (moveCaretToEnd) {
                editor.caretModel.moveToOffset(range.endOffset + text.length)
            }
        }
    }

    fun offsetCaret(insertionContext: InsertionContext, offset: Int) {
        offsetCaret(insertionContext.editor, offset)
    }

    fun offsetCaret(editor: Editor, offset: Int) {
        editor.caretModel.moveToOffset(editor.caretModel.offset + offset)
    }

    fun document(element: PsiElement) : Document? {
        val containingFile = element.containingFile ?: return null
        val psiDocumentManager = PsiDocumentManager.getInstance(element.project)
        return psiDocumentManager.getDocument(containingFile)
    }

    fun editor(element:PsiElement) : Editor?  {
        val document = document(element) ?: return null
        return EditorFactory.getInstance()
                .getEditors(document, element.project)
                .firstOrNull {editor ->
                    editor.psiFile == element.containingFile
                }
    }

    fun tabSize(psiElement: PsiElement) : Int? {
        val editor = psiElement.editor ?: return null
        return tabSize(editor)
    }

    fun tabSize(editor:Editor) : Int? {
        var tabSize: Int? = null
        val commonCodeStyleSettings = CommonCodeStyleSettings(CaosScriptLanguage)
        val indentOptions = commonCodeStyleSettings.indentOptions

        if (indentOptions != null) {
            tabSize = indentOptions.TAB_SIZE
        }

        if (tabSize == null || tabSize == 0) {
            tabSize = editor.settings.getTabSize(editor.project)
        }
        return tabSize
    }

}

val PsiElement.document : Document? get() {
    return EditorUtil.document(this)
}

val PsiElement.editor : Editor? get() {
    return EditorUtil.editor(this)
}

val Editor.primaryCursorElement:PsiElement? get() {
    val offset = caretModel.primaryCaret.offset
    return psiFile?.findElementAt(offset)
}

fun Editor.cursorElementInside(range:TextRange):PsiElement? {
    var offset = caretModel.primaryCaret.offset
    if (offset !in range.startOffset .. range.endOffset) {
        offset = caretModel.allCarets.firstOrNull{
            it.offset !in range.startOffset .. range.endOffset
        }?.offset
            ?: return null
    }
    return psiFile?.findElementAt(offset)
}

val Editor.psiFile : PsiFile? get() {
    val file = virtualFile
            ?: return null
    val project = this.project
            ?: return null
    return PsiManager.getInstance(project).findFile(file)
}

val Editor.element: PsiElement? get() {
    val file = psiFile
            ?: return null
    val position = this.caretModel.offset
    return file.findElementAt(position)
}

val Editor.module: Module? get() {
    val file = virtualFile
            ?: return null
    val project = this.project
            ?: return null
    return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(file)
}