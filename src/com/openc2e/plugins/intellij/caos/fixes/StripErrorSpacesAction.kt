package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.openc2e.plugins.intellij.caos.utils.CaosStringUtil

class StripErrorSpacesAction : EditorAction(Handler()) {
}

private class Handler : EditorWriteActionHandler() {
    override fun executeWriteAction(editor: Editor?, caret: Caret?, dataContext: DataContext?) {
        val document = editor?.document
                ?: return
        val text = document.text
        val trimmedText = CaosStringUtil.sanitizeCaosString(text)
        document.replaceString(0,text.length,trimmedText)
    }
}