package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.documentation.CaosScriptPresentationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.isOrHasParentOfType
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class CaosScriptUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                CaosScriptLexerAdapter(),
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
            element.isOrHasParentOfType(CaosScriptVarToken::class.java) -> "var ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_INT -> "int ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_FLOAT -> "float ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_FLOAT -> "number ${element.text}"
            element.elementType == CaosScriptTypes.CaosScript_QUOTE_STRING_LITERAL -> when {
                element.parent?.parent is CaosScriptNamedGameVar -> (element.parent?.parent as CaosScriptNamedGameVar).let {variable ->
                    "variable ${variable.varType.token} \"${variable.key}\""
                }
                    else -> "\"${element.text.substring(1 until element.textLength)}\""
            }
            else -> {
                element.getSelfOrParentOfType(CaosScriptIsCommandToken::class.java)?.let {
                    CaosScriptPresentationUtil.getDescriptiveText(it)
                } ?: element.text
            }
        }
    }

    override fun getType(element: PsiElement): String {

        return when {
            element is CaosScriptIsCommandToken || element is CaosScriptIsLvalueKeywordToken || element is CaosScriptIsRvalueKeywordToken -> "Command"
            element.isOrHasParentOfType(CaosScriptSubroutineName::class.java) -> "Subroutine Label"
            else -> when (element.elementType) {
                in CaosScriptTokenSets.STRING_LIKE -> (element.parent?.parent as? CaosScriptNamedGameVar)
                        ?.varType
                        ?.token
                        ?: "String"
                in CaosScriptTokenSets.KEYWORDS -> "Flow Control"
                in CaosScriptTokenSets.ALL_COMMANDS -> "Command"
                CaosScriptTypes.CaosScript_INT -> "Integer"
                CaosScriptTypes.CaosScript_FLOAT -> "Float"
                CaosScriptTypes.CaosScript_FLOAT -> "Number"
                CaosScriptTypes.CaosScript_VA_XX -> "SCRP variable"
                CaosScriptTypes.CaosScript_VAR_X -> "SCRP variable"
                CaosScriptTypes.CaosScript_OV_XX -> "TARG variable"
                CaosScriptTypes.CaosScript_OBV_X -> "TARG variable"
                CaosScriptTypes.CaosScript_MV_XX -> "OWNR variable"
                CaosScriptTypes.CaosScript_NAMED_GAME_VAR -> "variable"
                CaosScriptTypes.CaosScript_SUBROUTINE_NAME -> "SUBR"
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