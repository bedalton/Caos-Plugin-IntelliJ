package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupItem
import com.openc2e.plugins.intellij.caos.utils.EditorUtil

object SpaceAfterInsertHandler : InsertHandler<LookupElement>{
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val position = context.editor.caretModel.currentCaret.offset
        if (EditorUtil.isTextAtOffset(context, " ") || EditorUtil.isTextAtOffset(context, "\n") || EditorUtil.isTextAtOffset(context, "\t"))
            EditorUtil.insertText(context, " ", true)
    }

}