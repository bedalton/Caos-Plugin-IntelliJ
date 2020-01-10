package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile

class CaosScriptEnterDelegate : EnterHandlerDelegate {

    override fun postProcessEnter(file: PsiFile, editor: Editor, context: DataContext): EnterHandlerDelegate.Result {
        return EnterHandlerDelegate.Result.Continue
    }

    override fun preprocessEnter(file: PsiFile, editor: Editor, ref1: Ref<Int>, ref2: Ref<Int>, context: DataContext, editorActionHandler: EditorActionHandler?): EnterHandlerDelegate.Result {
        return EnterHandlerDelegate.Result.Continue
    }

}