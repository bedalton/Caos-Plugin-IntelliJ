package com.badahori.creatures.plugins.intellij.agenteering.caos.handlers

import bedalton.creatures.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.elementType

class CaosScriptEndWordIndenter(private val originalHandler: TypedActionHandler?)
    : TypedActionHandlerBase(originalHandler) {

    override fun execute(editor: Editor, c: Char, dataContext: DataContext) {
        originalHandler?.execute(editor, c, dataContext)

        val project = CommonDataKeys.PROJECT.getData(dataContext)
            ?: return
        if (PsiUtilBase.getLanguageInEditor(editor, project) != CaosScriptLanguage) {
            return
        }
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)
            ?: return
        if (file !is CaosScriptFile) {
            return
        }

        val blockToReformat = when (c) {
            'f', 'i' -> {
                getElementIfPrefixed(file, editor, ENDI, ELIF)
                    ?.getPreviousNonEmptySibling(true)
            }
            't' -> {
                getElementIfPrefixed(file, editor, NEXT)
                    ?.parent
            }
            'n' -> {
                getElementIfPrefixed(file, editor, RETN, NSCN)?.let {
                    if (it.elementType == CaosScriptTypes.CaosScript_C_KW_RETN) {
                        it.parent.parent
                            ?: return
                    } else {
                        it.parent
                    }
                }
            }
            'm' -> {
                getElementIfPrefixed(file, editor, ENDM)
                    ?.parent
                    ?.parent
            }
            'e' -> {
                getElementIfPrefixed(file, editor, REPE, ELSE)?.let {
                    if (it.elementType == CaosScriptTypes.CaosScript_C_ELSE) {
                        it.getPreviousNonEmptySibling(true)
                    } else {
                        it.parent
                    }
                }
            }
            'l' -> {
                getElementIfPrefixed(file, editor, UNTL)
                    ?.parent
                    ?.parent
            }
            'r' -> {
                getElementIfPrefixed(file, editor, EVER)
                    ?.parent
                    ?.parent
            }
            else -> null

        } ?: return

        try {
            CodeStyleManager.getInstance(project).reformatText(file, blockToReformat.startOffset, editor.caretModel.offset)
        } catch (e: Exception) {
            LOGGER.severe("Failed to format code block after ending word. ${e.className}: ${e.message ?: ""}")
        }
    }

    private fun commitAndUnblockDocument(file: PsiFile): Boolean {
        return try {
            val manager = PsiDocumentManager.getInstance(file.project)
            file.document?.let { document ->
                manager.doPostponedOperationsAndUnblockDocument(document)
                manager.commitDocument(document)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getElementIfPrefixed(file: PsiFile, editor: Editor, vararg words: Int): PsiElement? {
        if (editor.caretModel.offset < 3) {
            return null
        }
        if (!commitAndUnblockDocument(file)) {
            return null
        }
        val element = file.findElementAt(editor.caretModel.offset - 2)
            ?: return null
        val token = currentToken(element)
            ?: return null
        return if (token in words) {
            element.parent ?: element
        } else {
            null
        }
    }

    private fun currentToken(element: PsiElement): Int? {
        if (element.textLength != 4) {
            return null
        }
        val text = element.text
        return try {
            token(text)
        } catch (e: Exception) {
            null
        }
    }

    companion object {

        private val ELIF = token('e', 'l', 'i', 'f') //
        private val ELSE = token('e', 'l', 's', 'e') //
        private val ENDI = token('e', 'n', 'd', 'i') //
        private val REPE = token('r', 'e', 'p', 'e') //
        private val NEXT = token('n', 'e', 'x', 't') //
        private val NSCN = token('n', 's', 'c', 'n') //
        private val UNTL = token('u', 'n', 't', 'l')//
        private val EVER = token('e', 'v', 'e', 'r')//
        private val RETN = token('r', 'e', 't', 'n') //
        private val ENDM = token('e', 'n', 'd', 'm')//
    }

}