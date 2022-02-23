package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlockLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

object WhitespacePsiUtil {

    fun isNotWhitespace(previous: PsiElement?): Boolean {
        if (previous == null) {
            return false
        }
        return previous.tokenType != CaosScriptTypes.CaosScript_COMMA &&
            previous.tokenType != TokenType.WHITE_SPACE &&
            previous.tokenType != CaosScriptTypes.CaosScript_NEWLINE
    }

    fun isWhitespace(previous: PsiElement): Boolean {
        return previous.tokenType == CaosScriptTypes.CaosScript_COMMA ||
                previous.tokenType == TokenType.WHITE_SPACE ||
                previous.tokenType == CaosScriptTypes.CaosScript_NEWLINE
    }

    fun getElementBeforeSpaces(element: PsiElement): PsiElement? {
        var previous = element.previous
            ?: return null
        while (isWhitespace(previous) && previous.tokenType != CaosScriptTypes.CaosScript_NEWLINE && !previous.text.contains('\n')) {
            previous = previous.previous
                ?: return null
        }
        return previous
    }

    fun getElementAfterSpaces(element: PsiElement): PsiElement? {
        var next = element.next
            ?: return null
        while (isWhitespace(next) && next.tokenType != CaosScriptTypes.CaosScript_NEWLINE && !next.text.contains('\n')) {
            next = next.next
                ?: return null
        }
        return next
    }

    fun isNewline(element: PsiElement): Boolean {
        return element.tokenType == CaosScriptTypes.CaosScript_NEWLINE || element.text == "\n"
    }
}