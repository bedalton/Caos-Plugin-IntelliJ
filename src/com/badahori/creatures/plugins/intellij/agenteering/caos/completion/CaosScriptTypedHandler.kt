package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.element
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.tree.TokenSet

class CaosScriptTypedHandler(private val originalHandler:TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {

    private val handle = listOf('\'', '"')
    private val quoteTokenSet = TokenSet.create(
            CaosScriptTypes.CaosScript_DOUBLE_QUOTE,
            CaosScriptTypes.CaosScript_SINGLE_QUOTE,
            CaosScriptTypes.CaosScript_OPEN_BRACKET,
            CaosScriptTypes.CaosScript_CLOSE_BRACKET
    )
    private val blockEndLastChars = listOf(
            'm', 'M', // end[m]
            'i', 'I', // end[i]
            't', 'T', // next[t]
            'r', 'R', // eve[r]
            'l', 'L', // unt[l]
            'n', 'N', // esc[n] // retn
            'e', 'E' // rep[e]
    )


    override fun execute(editor: Editor, char: Char, context: DataContext) {
        originalHandler?.execute(editor, char, context)
        //if (handleQuote(char, editor)) {
          //  return
        //}
        //if (dedentIfBlockEnd(c, project, editor)) {
        //  return Result.CONTINUE
        // }
    }

    private fun handleQuote(c: Char, editor: Editor): Boolean {
        if (c != '"' && c != '\'')
            return false
        val element = editor.element
                ?: return false
        element.getSelfOrParentOfType(CaosScriptStringLike::class.java)?.let {
            if (it.isClosed)
                return true
        }
        val replacement = when (c) {
            '\'' -> "'"
            '"' -> "\""
            else -> return false
        }
        EditorUtil.insertText(editor, replacement, false)
        return true
    }

    private fun dedentIfBlockEnd(c: Char, project:Project, editor: Editor): Boolean {
        if (c !in blockEndLastChars) {
            return false
        }
        val element = editor.element
                ?: return false
        val blockEnd = when (c)  {
            'm', 'M' -> element.getSelfOrParentOfType(CaosScriptCEndm::class.java)
            'i', 'I' -> element.getSelfOrParentOfType(CaosScriptCEndi::class.java)
            'e', 'E' -> element.getSelfOrParentOfType(CaosScriptCRepe::class.java)
            'r', 'R' -> element.getSelfOrParentOfType(CaosScriptCEver::class.java)
            'l', 'L' -> element.getSelfOrParentOfType(CaosScriptCUntl::class.java)
            't', 'T' -> element.getSelfOrParentOfType(CaosScriptCNext::class.java)
            'n', 'N' -> element.getSelfOrParentOfType(CaosScriptCEscn::class.java)
                    ?: element.getSelfOrParentOfType(CaosScriptCRetn::class.java)
            else -> null
        } ?: return false
        val container = blockEnd.getParentOfType(CaosScriptHasCodeBlock::class.java)
                ?: return true
        runUndoTransparentWriteAction {
            CodeStyleManager.getInstance(project).reformat(container, false)
        }
        return true
    }
}