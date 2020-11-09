package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefVariableLink
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefVariableName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement

/**
 * Establishes a find usages provider
 */
class CaosDefUsagesProvider : FindUsagesProvider{

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosDefLexerAdapter(),
                TokenSet.create(CaosDef_COMMAND_WORD, CaosDef_WORD, CaosDef_VARIABLE_LINK, CaosDef_VARIABLE_LINK_LITERAL, CaosDef_VARIABLE_NAME),
                TokenSet.create(CaosDef_LINE_COMMENT, CaosDef_DOC_COMMENT, CaosDef_COMMENT_TEXT_LITERAL, CaosDef_DOC_COMMENT_OPEN, CaosDef_COMMENT_TEXT_LITERAL),
                TokenSet.create(CaosDef_INT_LITERAL, CaosDef_INT)
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
        return element is CaosScriptCompositeElement || element is CaosDefCompositeElement
    }
}