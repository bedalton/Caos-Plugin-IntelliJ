package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.element
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.orFalse
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result.Continue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.suggested.endOffset

class CaosScriptEnterDelegate : EnterHandlerDelegate {

    override fun preprocessEnter(file: PsiFile, editor: Editor, ref1: Ref<Int>, ref2: Ref<Int>, context: DataContext, editorActionHandler: EditorActionHandler?): EnterHandlerDelegate.Result {

        return handle(file, editor)//Continue
    }

    override fun postProcessEnter(file: PsiFile, editor: Editor, context: DataContext): EnterHandlerDelegate.Result {
        return Continue
    }

    private fun handle(file: PsiFile, editor:Editor): EnterHandlerDelegate.Result {
        var offset = editor.caretModel.currentCaret.offset
        if (offset > 2) {
            offset -= 2
        } else if (offset > 1)
            offset -= 1
        val element = editor.element ?: file.findElementAt(offset)
        ?: return Continue

        val reformatRange: TextRange? = element.getSelfOrParentOfType(CaosScriptHasCodeBlock::class.java)?.let { block ->
            when {
                block.parent is CaosScriptDoifStatement -> onEnterInDoif(editor, block.parent as CaosScriptDoifStatement)
                block is CaosScriptEnumNextStatement -> onEnterInEnumNext(editor, block)
                block is CaosScriptEnumSceneryStatement -> onEnterInEscn(editor, block)
                block is CaosScriptRepeatStatement -> onEnterInRepeatStatement(editor, block)
                block is CaosScriptSubroutine -> onEnterInSubroutine(editor, block)
                else -> null
            }
        }?.let {
            it.getParentOfType(CaosScriptHasCodeBlock::class.java) ?: it.parent ?: it
        }?.textRange
        if (reformatRange != null) {
            CodeStyleManager.getInstance(file.project).reformatTextWithContext(file, listOf(reformatRange))
        }

        return Continue
    }

    private fun onEnterInDoif(editor: Editor, doif: CaosScriptDoifStatement): PsiElement? {
        if (doif.cEndi != null)
            return null
        val after = getEditorAfterCandidate(editor) {
            doif.elseStatement
                    ?: doif.elseIfStatementList.lastOrNull()
                    ?: doif.doifStatementStatement.equalityExpression
                    ?: doif.lastChild
        }.let {
            it.textRange.endOffset
        }
        EditorUtil.insertText(editor, "\nendi", after, false)
        return doif
    }

    private fun onEnterInEnumNext(editor: Editor, enumNext: CaosScriptEnumNextStatement): PsiElement? {
        if (enumNext.cNext != null || enumNext.cNscn != null)
            return null
        val after = getEditorAfterCandidate(editor) { enumNext.enumHeaderCommand }.endOffset
        EditorUtil.insertText(editor, "\nnext", after, false)
        return enumNext
    }

    private fun onEnterInEscn(editor: Editor, escn: CaosScriptEnumSceneryStatement): PsiElement? {
        if (escn.cNext != null || escn.cNscn != null)
            return null
        val after = getEditorAfterCandidate(editor) { escn.escnHeader }.textRange.endOffset
        EditorUtil.insertText(editor, "\nnscn", after, false)
        return escn
    }

    private fun onEnterInRepeatStatement(editor: Editor, reps: CaosScriptRepeatStatement): PsiElement? {
        if (reps.cRepe != null)
            return null
        val after = getEditorAfterCandidate(editor) { reps.repsHeader }.endOffset
        EditorUtil.insertText(editor, "\nrepe", after, false)
        return reps
    }

    private fun onEnterInSubroutine(editor: Editor, subroutine: CaosScriptSubroutine): PsiElement? {
        if (subroutine.cRetn != null)
            return null
        EditorUtil.insertText(editor, "\nretn", subroutine.endOffset, false)
        return subroutine
    }

    private fun getEditorAfterCandidate(editor: Editor, find: () -> PsiElement): PsiElement {
        var item = getEditorAfterCandidateRaw(editor, find)

        return item
    }

    private fun getEditorAfterCandidateRaw(editor: Editor, find: () -> PsiElement): PsiElement {
        val editorElement = editor.element
        if (editorElement == null) {
            return find()
        }
        editorElement.getPreviousNonEmptySibling(true)?.let { sibling ->
            sibling.getSelfOrParentOfType(CaosScriptCodeBlockLine::class.java)?.let { return it }
            sibling.lastChild.getSelfOrParentOfType(CaosScriptCodeBlockLine::class.java)?.let { return it }
        }
        return find()
    }
}