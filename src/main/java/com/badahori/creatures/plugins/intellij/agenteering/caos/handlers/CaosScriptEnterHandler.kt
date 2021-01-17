package com.badahori.creatures.plugins.intellij.agenteering.caos.handlers

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2BlockComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result.Continue
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptEnterHandler : EnterHandlerDelegate {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        return Continue
    }

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): EnterHandlerDelegate.Result {
        val caret = dataContext.getData(CommonDataKeys.CARET)?.selectionStart
            ?: return Continue
        val document = editor.document
        PsiDocumentManager.getInstance(file.project).commitDocument(document)
        var caretElement = file.findElementAt(maxOf(caret - 1, 0))
        if (caretElement == null) {
            return Continue
        }
        if (caretElement.elementType in CaosScriptTokenSets.WHITESPACES) {
            caretElement = caretElement.previous
        }
        if (caretElement is CaosScriptCaos2BlockComment || caretElement is CaosScriptCaos2Block) {
            EditorUtil.insertText(editor, "*# ", true)
            return EnterHandlerDelegate.Result.Stop
        }
        return Continue
    }
}