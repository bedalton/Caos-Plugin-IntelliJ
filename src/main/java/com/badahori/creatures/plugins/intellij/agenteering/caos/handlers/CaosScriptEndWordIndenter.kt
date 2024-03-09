package com.badahori.creatures.plugins.intellij.agenteering.caos.handlers

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.className
import com.bedalton.common.util.formatted
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.elementType

class CaosScriptEndWordIndenter : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {

        if (PsiUtilBase.getLanguageInEditor(editor, project) != CaosScriptLanguage) {
//            LOGGER.info("CaosScriptEndWordIndenter not called on NON-CAOS language")
            return Result.CONTINUE
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
                            ?: return Result.CONTINUE
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

        } ?: return Result.CONTINUE/*.also {
            LOGGER.info("Is not a CAOS de-dent token; Char: $c; Element: ${editor.element?.let { it.className +  "[${it.text}]" } }")
        }*/



        try {
//            LOGGER.info("IS CAOS de-dent token; \n\tChar: $c;\n\tElement: ${blockToReformat.className}[${blockToReformat.text}]\nReformatting...")
            CodeStyleManager.getInstance(project)
                .reformatText(file, blockToReformat.startOffset, blockToReformat.endOffset)
        } catch (e: Exception) {
            LOGGER.severe("Failed to de-dent at token;\n\tChar: $c;\n\tElement: ${blockToReformat.className}[${blockToReformat.text}]\n${e.formatted(true)}")
        }
        return Result.CONTINUE
    }

    private fun commitAndUnblockDocument(file: PsiFile): Boolean {
        return try {
            val manager = PsiDocumentManager.getInstance(file.project)
            file.document?.let { document ->
                manager.doPostponedOperationsAndUnblockDocument(document)
                manager.commitDocument(document)
                // log(file.project) { "Committed document" }
                true
            } ?: false
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

}

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