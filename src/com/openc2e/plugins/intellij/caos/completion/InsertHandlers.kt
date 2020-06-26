package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.utils.CaosAgentClassUtils
import com.openc2e.plugins.intellij.caos.utils.EditorUtil
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*


object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        if (EditorUtil.isTextAtOffset(context, " ") || EditorUtil.isTextAtOffset(context, "\n") || EditorUtil.isTextAtOffset(context, "\t"))
            return
        EditorUtil.insertText(context, " ", true)
    }
}

object ReplaceTextWithValueInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupEl: LookupElement) {
        context.document.replaceString(context.startOffset, context.tailOffset, lookupEl.lookupString)
    }
}