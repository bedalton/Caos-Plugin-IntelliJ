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

class CaosScriptCollapseNewLineIntentionAction(private val collapseChar: CollapseChar) : IntentionAction {
    override fun startInWriteAction(): Boolean = true

    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")


    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (file == null)
            return false
        val hasNewlines = file.text.orEmpty().contains("\n")
        val isMultiline = file.text.length > 5 && file.lastChild.lineNumber != 0
        return hasNewlines || isMultiline
    }

    override fun getText(): String = collapseChar.text

    override fun invoke(project: Project, editor: Editor?, fileIn: PsiFile?) {
        val bodyElements = fileIn?.getChildrenOfType(CaosScriptScriptBodyElement::class.java)
                ?: return
        for (element in bodyElements) {
            if (element.eventScript != null) {
                element.eventScript!!.let { eventScript ->
                    eventScript.eventNumberElement?.next?.let { next ->
                        replaceWithSpaceOrComma(next)
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
                    replaceWithSpaceOrComma(it)
                }
        doifStatement.doifStatementStatement.codeBlock?.let { addLines(it) }
        doifStatement.elseIfStatementList.forEach { elseif ->
            replaceWithSpaceOrComma(elseif.spaceLikeOrNewline)
            elseif.codeBlock?.let { addLines(it) }
        }
        doifStatement.elseStatement?.let {elseElement ->
            replaceWithSpaceOrComma(elseElement.spaceLikeOrNewline)
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
            replaceWithSpaceOrComma(next)
            val hasCodeBlock = line.getChildOfType(CaosScriptHasCodeBlock::class.java)
                    ?: continue
            for (space in hasCodeBlock.getChildrenOfType(CaosScriptSpaceLikeOrNewline::class.java)) {
                replaceWithSpaceOrComma(space)
            }
            val codeBlockChild = hasCodeBlock
                    .codeBlock
                    ?: continue
            codeBlockChild.previous?.let {
                replaceWithSpaceOrComma(it)
            }
            addLines(codeBlockChild)
        }
        block.previous?.let { replaceWithSpaceOrComma(it) }
        block.lastChild.next?.let{ replaceWithSpaceOrComma(it) }
    }

    private fun replaceWithSpaceOrComma(next:PsiElement?) {
        if (next == null)
            return
        if (next.text != "\n" && next.text.contains('\n'))
            return
        val element = if (collapseChar == CollapseChar.SPACE)
            CaosScriptPsiElementFactory.spaceLikeOrNewlineSpace(next.project)
        else
            CaosScriptPsiElementFactory.comma(next.project)
        next.replace(element)

    }

}

enum class CollapseChar(internal val text:String) {
    SPACE(CaosBundle.message("caos.intentions.collapse-lines-with", "spaces")),
    COMMA(CaosBundle.message("caos.intentions.collapse-lines-with", "commas"))
}