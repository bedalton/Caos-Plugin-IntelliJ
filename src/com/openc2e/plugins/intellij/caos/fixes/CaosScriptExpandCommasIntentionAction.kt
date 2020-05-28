package com.openc2e.plugins.intellij.caos.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.*

object CaosScriptExpandCommasIntentionAction : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")


    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null)
            return false
        val hasCommas = file.text.orEmpty().contains(",")
        val isSingleLine = file.text.length > 5 && file.lastChild.lineNumber == 0
        if (hasCommas)
            return true
        else if (isSingleLine) {
            return true
        }
        return false
    }

    override fun getText(): String = CaosBundle.message("caos.intentions.commands-on-new-line")

    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        val bodyElements = fileIn?.getChildrenOfType(CaosScriptScriptBodyElement::class.java)
                ?: return
        for (element in bodyElements) {
            if (element.eventScript != null) {
                element.eventScript!!.let { eventScript ->
                    eventScript.eventNumberElement?.next?.let { next ->
                        replaceIfBlankOrComma(next)
                    }
                    eventScript.codeBlock?.let { block ->
                        addLines(block)
                    }
                }
            }
            if (element.macro != null) {
                addLines(element.macro!!.codeBlock)
            }
        }
        CodeStyleManager.getInstance(project).reformat(fileIn)
    }

    private fun expandDoIf(doifStatement: CaosScriptDoifStatement) {
        doifStatement.doifStatementStatement
                .spaceLikeOrNewlineList
                .forEach {
                    replaceIfBlankOrComma(it)
                }
        doifStatement.doifStatementStatement.codeBlock?.let { addLines(it) }
        doifStatement.elseIfStatementList.forEach { elseif ->
            replaceIfBlankOrComma(elseif.spaceLikeOrNewline)
            elseif.codeBlock?.let { addLines(it) }
        }
        doifStatement.elseStatement?.let {elseElement ->
            replaceIfBlankOrComma(elseElement.spaceLikeOrNewline)
            elseElement.codeBlock?.let {
                addLines(it)
            }
        }
    }


    private fun addLines(block:CaosScriptCodeBlock) {
        for (line in block.codeBlockLineList) {
            line.doifStatement?.let {
                expandDoIf(it)
            }
            val next = line.lastChild.next
                    ?: continue
            replaceIfBlankOrComma(next)
            val hasCodeBlock = line.getChildOfType(CaosScriptHasCodeBlock::class.java)
                    ?: continue
            for (space in hasCodeBlock.getChildrenOfType(CaosScriptSpaceLikeOrNewline::class.java)) {
                replaceIfBlankOrComma(space)
            }
            val codeBlockChild = hasCodeBlock
                    .codeBlock
                    ?: continue
            codeBlockChild.previous?.let {
                replaceIfBlankOrComma(it)
            }
            addLines(codeBlockChild)
        }
        block.previous?.let { replaceIfBlankOrComma(it) }
        block.lastChild.next?.let{ replaceIfBlankOrComma(it) }
    }

    private fun replaceIfBlankOrComma(next:PsiElement?) {
        if (next == null)
            return
        if (next.text.trim(' ', '\n', ',').isEmpty() || next.text == ",")
            next.replace(CaosScriptPsiElementFactory.newLine(next.project))
    }

}