package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosStringUtil

@Suppress("unused", "ComponentNotRegistered")
class StripErrorSpacesAction : EditorAction(Handler())

private class Handler : EditorWriteActionHandler() {
    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        val document = editor.document
        val text = document.text
        val trimmedText = CaosStringUtil.sanitizeCaosString(text)
        document.replaceString(0,text.length,trimmedText)
    }
}