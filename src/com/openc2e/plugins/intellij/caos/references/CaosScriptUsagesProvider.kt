package com.openc2e.plugins.intellij.caos.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefLexerAdapter
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.psi.util.elementType

class CaosScriptUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosDefLexerAdapter(),
                CaosScriptTokenSets.ALL_CAOS_KEYWORDS,
                CaosScriptTokenSets.COMMENTS,
                CaosScriptTokenSets.LITERALS
        )
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is CaosScriptIsCommandToken, is CaosScriptIsLvalueKeywordToken, is CaosScriptIsRvalueKeywordToken -> element.text
            else -> element.text
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is CaosScriptIsCommandToken, is CaosScriptIsLvalueKeywordToken, is CaosScriptIsRvalueKeywordToken -> "[" + element.text + "]"
            else -> "element"
        }
    }

    override fun getType(element: PsiElement): String {

        return when(element) {
            is CaosScriptIsCommandToken, is CaosScriptIsLvalueKeywordToken, is CaosScriptIsRvalueKeywordToken -> "Command"
            else -> when (element.elementType) {
                in CaosScriptTokenSets.STRING_LIKE -> "String"
                in CaosScriptTokenSets.ALL_CAOS_KEYWORDS -> "Flow Control"
                in CaosScriptTokenSets.ALL_COMMANDS -> "Command"
                else -> "element"
            }
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is CaosScriptCompositeElement || element is CaosDefCompositeElement
    }
}