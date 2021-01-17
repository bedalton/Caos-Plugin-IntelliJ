package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpressionPrime
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement


object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        if (EditorUtil.isTextAtOffset(context, " ") || EditorUtil.isTextAtOffset(context, "\n") || EditorUtil.isTextAtOffset(context, "\t"))
            return
        if (lookupElement.psiElement?.getParentOfType(CaosScriptEqualityExpressionPrime::class.java)?.eqOp != null)
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
        if (EditorUtil.isTextAtOffset(context, closeQuote))
            return
        EditorUtil.insertText(context, closeQuote, true)
    }
}
object EqualSignInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        if (EditorUtil.isTextAtOffset(context, "=") || EditorUtil.isTextAtOffset(context, " ="))
            return
        val text = if (EditorUtil.isTextAtOffset(context, " "))
            "= "
        else
            " = "
        EditorUtil.insertText(context, text, true)
    }
}

class OffsetCursorInsertHandler(val offset:Int) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, lookupElement: LookupElement) {
        val editor = context.editor
        val caretPosition = context.editor.caretModel.currentCaret.offset
        EditorUtil.offsetCaret(editor, caretPosition + offset)
    }
}