package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil


object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        context.editor.caretModel.currentCaret.offset
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

class CloseQuoteInsertHandler(private val closeQuote:String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        context.editor.caretModel.currentCaret.offset
        if (EditorUtil.isTextAtOffset(context, closeQuote))
            return
        EditorUtil.insertText(context, closeQuote, true)
    }
}