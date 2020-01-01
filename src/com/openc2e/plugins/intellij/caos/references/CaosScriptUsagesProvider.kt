package com.openc2e.plugins.intellij.caos.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefLexerAdapter
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

class CaosScriptUsagesProvider : FindUsagesProvider{

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosDefLexerAdapter(),
                TokenSet.create(CaosScript_COMMAND_TOKEN),
                CaosScriptTokenSets.COMMENTS,
                CaosScriptTokenSets.LITERALS
        )
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is CaosScriptCommandToken -> element.text
            else -> element.text
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is CaosScriptCommandToken -> "[" + element.text + "]"
            else -> "element"
        }
    }

    override fun getType(element: PsiElement): String {
        return when (element) {
            is CaosScriptCommandToken -> "Command Token"
            else -> "element"
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is CaosScriptCompositeElement || element is CaosDefCompositeElement
    }
}