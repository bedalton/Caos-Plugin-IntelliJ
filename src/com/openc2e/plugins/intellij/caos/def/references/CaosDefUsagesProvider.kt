package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefLexerAdapter
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes
import com.openc2e.plugins.intellij.caos.def.psi.api.*

class CaosDefUsagesProvider : FindUsagesProvider{

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosDefLexerAdapter(),
                TokenSet.create(CaosDefTypes.CaosDef_COMMAND_WORD, CaosDefTypes.CaosDef_VARIABLE_LINK, CaosDefTypes.CaosDef_VARIABLE_NAME),
                TokenSet.create(CaosDefTypes.CaosDef_LINE_COMMENT),
                TokenSet.create(CaosDefTypes.CaosDef_INT_LITERAL, CaosDefTypes.CaosDef_INT)
        )
    }
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is CaosDefVariableLink -> "link ${element.text}"
            is CaosDefVariableName -> "@param {${element.text}}"
            is CaosDefCommandWord -> element.text
            else -> element.text
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is CaosDefVariableLink -> element.variableName
            is CaosDefVariableName -> element.text
            is CaosDefCommandWord -> element.text.toUpperCase()
            else -> "element"
        }
    }

    override fun getType(element: PsiElement): String {
        return when (element) {
            is CaosDefVariableLink -> "parameter link"
            is CaosDefVariableName -> "parameter"
            else -> "element"
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is CaosDefCompositeElement
    }
}