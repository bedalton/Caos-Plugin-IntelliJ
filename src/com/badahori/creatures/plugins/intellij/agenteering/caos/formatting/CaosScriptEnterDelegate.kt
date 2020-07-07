package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result.Continue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.suggested.endOffset

class CaosScriptEnterDelegate : EnterHandlerDelegate {

    override fun postProcessEnter(file: PsiFile, editor: Editor, context: DataContext): EnterHandlerDelegate.Result {
        return Continue
    }

    override fun preprocessEnter(file: PsiFile, editor: Editor, ref1: Ref<Int>, ref2: Ref<Int>, context: DataContext, editorActionHandler: EditorActionHandler?): EnterHandlerDelegate.Result {        LOGGER.info("Post process enter")
        var offset = editor.caretModel.currentCaret.offset
        if (offset > 2) {
            offset -= 2
        } else if (offset > 1)
            offset -= 1
        LOGGER.info("Getting element at offset: $offset")
        val element = file.findElementAt(offset)?.let { it }
                ?: return Continue
        LOGGER.info("Got element at selection start. Element is: ${element.elementType}")
        val reformatRange: TextRange? = element.getSelfOrParentOfType(CaosScriptHasCodeBlock::class.java)?.let { block ->
            when {
                block.parent is CaosScriptDoifStatement -> onEnterInDoif(editor, block.parent as CaosScriptDoifStatement)
                block is CaosScriptEnumNextStatement -> onEnterInEnumNext(editor, block)
                block is CaosScriptEnumSceneryStatement -> onEnterInEscn(editor, block)
                block is CaosScriptRepeatStatement -> onEnterInRepeatStatement(editor, block)
                block is CaosScriptSubroutine -> onEnterInSubroutine(editor, block)
                else -> {
                    LOGGER.info("Did not need completion on block of type: ${block.elementType} with parent of ${block.parent?.elementType}")
                    null
                }
            }
        }
        if (reformatRange != null) {
            CodeStyleManager.getInstance(file.project).reformatTextWithContext(file, listOf(reformatRange))
        } else {
            LOGGER.info("No reformat needed")
        }

        return Continue
    }

    private fun onEnterInDoif(editor: Editor, doif: CaosScriptDoifStatement): TextRange? {
        if (doif.cEndi != null)
            return null
        EditorUtil.insertText(editor, "\nendi", doif.endOffset, false)
        return doif.textRange
    }

    private fun onEnterInEnumNext(editor: Editor, enumNext: CaosScriptEnumNextStatement): TextRange? {
        if (enumNext.cNext != null || enumNext.cNscn != null)
            return null
        EditorUtil.insertText(editor, "\nnext", enumNext.endOffset, false)
        return enumNext.textRange
    }

    private fun onEnterInEscn(editor: Editor, enumNext: CaosScriptEnumSceneryStatement): TextRange? {
        if (enumNext.cNext != null || enumNext.cNscn != null)
            return null
        EditorUtil.insertText(editor, "\nnscn", enumNext.endOffset, false)
        return enumNext.textRange
    }

    private fun onEnterInRepeatStatement(editor: Editor, enumNext: CaosScriptRepeatStatement): TextRange? {
        if (enumNext.cRepe != null)
            return null
        EditorUtil.insertText(editor, "\nrepe", enumNext.endOffset, false)
        return enumNext.textRange
    }

    private fun onEnterInSubroutine(editor: Editor, enumNext: CaosScriptSubroutine): TextRange? {
        if (enumNext.cRetn != null)
            return null
        EditorUtil.insertText(editor, "\nretn", enumNext.endOffset, false)
        return enumNext.textRange
    }
}