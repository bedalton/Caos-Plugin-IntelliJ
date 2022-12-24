package com.badahori.creatures.plugins.intellij.agenteering.caos.handlers

import bedalton.creatures.common.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotification
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.elementType

class CaosScriptEndWordIndenter(private val originalHandler: TypedActionHandler?) :
    TypedActionHandlerBase(originalHandler) {

    override fun execute(editor: Editor, c: Char, dataContext: DataContext) {
        try {
            originalHandler?.execute(editor, c, dataContext)
        } catch (e: Exception) {
            // logGER.severe("Failed to delegate TypedActionHandler to parent call (${originalHandler.className}). ${e.className}: ${e.message}\n${e.stackTraceToString()}")
        }
        handle(editor, c, dataContext)
    }

    private fun handle(editor: Editor, c: Char, dataContext: DataContext): Boolean {
        val project = CommonDataKeys.PROJECT.getData(dataContext)
            ?: return false

        if (PsiUtilBase.getLanguageInEditor(editor, project) != CaosScriptLanguage) {
            // log(editor.project!!) { "Not CaosScriptLanguage" }
            return false
        }
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)
            ?: return false.apply {
                // log(editor.project!!) { "Failed to get PSI file" }
            }

        if (file !is CaosScriptFile) {
            // log(editor.project!!) { "Is not CAOS Script File" }
            return false
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
                            ?: return false
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

            else -> null.apply {
                // log(editor.project!!) { "Not a possible block end char $c" }
            }

        } ?: return false.apply {
            // log(editor.project!!) { "Failed to match char $c to end of block" }
        }

        return try {
            // log(editor.project!!) { "Trying to format" }
            CodeStyleManager.getInstance(project)
                .reformatText(file, blockToReformat.startOffset, editor.caretModel.offset)
            // log(editor.project!!) { "Did reformat" }
            true
        } catch (e: Exception) {
            // log(editor.project!!) { "Failed to format code block after ending word. ${e.className}: ${e.message ?: ""}" }
            // logGER.severe("Failed to format code block after ending word. ${e.className}: ${e.message ?: ""}")
            false
        }
    }

    private fun commitAndUnblockDocument(file: PsiFile): Boolean {
        return try {
            val manager = PsiDocumentManager.getInstance(file.project)
            file.document?.let { document ->
                manager.doPostponedOperationsAndUnblockDocument(document)
                manager.commitDocument(document)
                // log(file.project) { "Committed document" }
                true
            } ?: false.apply {
                // log(file.project) { "Failed to locate document to commit" }
            }
        } catch (e: Exception) {
            // log(file.project) { "Failed to commit document ${e.className}: ${e.message}" }
            false
        }
    }

    private fun getElementIfPrefixed(file: PsiFile, editor: Editor, vararg words: Int): PsiElement? {
        if (editor.caretModel.offset < 3) {
            // log(editor.project!!) { "Caret too close to start" }
            return null
        }
        if (!commitAndUnblockDocument(file)) {
            // log(editor.project!!) { "Failed to commit and unblock document" }
            return null
        }
        val element = file.findElementAt(editor.caretModel.offset - 2)
            ?: return null.apply {
                // log(editor.project!!) { "Failed to find element at offset: ${editor.caretModel.offset - 2}" }
            }
        val token = currentToken(element)
            ?: return null.apply {
                // log(editor.project!!) { "Current token for element ${element.text} is null" }
            }
        return if (token in words) {
            // log(editor.project!!) { "Token ${element.text} is in words" }
            element.parent ?: element
        } else {
            // log(editor.project!!) { "Token ${element.text} is NOT in words" }
            null
        }
    }

    private fun currentToken(element: PsiElement): Int? {
        if (element.textLength != 4) {
            // log(element.project) { "PsiElement &lt;${element.text}> is not a word token" }
            return null
        }
        val text = element.text
        return try {
            token(text).apply {
                // log(element.project) { "Element ${element.text} == Token[$this]" }
            }
        } catch (e: Exception) {
            // log(element.project) { "Element ${element.text} caused error on token; ${e.className}: ${e.message}" }
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

//        private inline fun log(project: Project, text: () -> String) {
//            CaosNotifications.showInfo(project, "End Word handler", text())
//        }
    }

}