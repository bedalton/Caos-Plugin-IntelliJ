package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.documentation.CaosScriptPresentationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isOrHasParentOfType

class CaosScriptUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosDefLexerAdapter(),
                CaosScriptTokenSets.ALL_FIND_USAGES_TOKENS,
                CaosScriptTokenSets.COMMENTS,
                CaosScriptTokenSets.LITERALS
        )
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String
            = element.text

    override fun getDescriptiveName(element: PsiElement): String {
        return when {
            element.isOrHasParentOfType(CaosScriptSubroutineName::class.java) -> "SUBR ${element.text}"
            element.isOrHasParentOfType(CaosScriptNamedVar::class.java) -> "var ${element.text}"
            element.isOrHasParentOfType(CaosScriptNamedConstant::class.java) -> "const ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_INT -> "int ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_FLOAT -> "float ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_DECIMAL -> "number ${element.text}"
            else -> {
                element.getSelfOrParentOfType(CaosScriptIsCommandToken::class.java)?.let {
                    CaosScriptPresentationUtil.getDescriptiveText(it)
                } ?: "element"
            }
        }
    }

    override fun getType(element: PsiElement): String {

        return when {
            element is CaosScriptIsCommandToken || element is CaosScriptIsLvalueKeywordToken || element is CaosScriptIsRvalueKeywordToken -> "Command"
            element.isOrHasParentOfType(CaosScriptSubroutineName::class.java) -> "Subroutine Label"
            else -> when (element.elementType) {
                in CaosScriptTokenSets.STRING_LIKE -> "String"
                in CaosScriptTokenSets.KEYWORDS -> "Flow Control"
                in CaosScriptTokenSets.ALL_COMMANDS -> "Command"
                CaosScriptTypes.CaosScript_INT -> "Integer"
                CaosScriptTypes.CaosScript_FLOAT -> "Float"
                CaosScriptTypes.CaosScript_DECIMAL -> "Number"
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