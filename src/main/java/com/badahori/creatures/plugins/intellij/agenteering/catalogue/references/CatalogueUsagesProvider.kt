package com.badahori.creatures.plugins.intellij.agenteering.catalogue.references

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.types.CatalogueTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.bedalton.common.util.ensureNotEndsWith
import com.bedalton.common.util.ensureNotStartsWith
import com.bedalton.common.util.stripSurroundingQuotes
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

class CatalogueUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
                CatalogueLexerAdapter(),
                CatalogueTokenSets.ALL_FIND_USAGES_TOKENS,
                CatalogueTokenSets.COMMENTS,
                CatalogueTokenSets.LITERALS
        )
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return when (element) {
            is CatalogueItemName -> element.text.stripSurroundingQuotes(false)
            is CatalogueTag -> element.name
            is CatalogueArray -> element.name
            else -> {
                when (element.elementType) {
                    CatalogueTypes.CATALOGUE_STRING_LITERAL -> element
                        .text
                        .stripSurroundingQuotes(false)
                    in CatalogueTokenSets.STRINGS -> element.text
                        .ensureNotStartsWith('"')
                        .ensureNotEndsWith('"')
                    CatalogueTypes.CATALOGUE_WORD -> element.text
                    else -> null
                }
            }
        } ?: ""
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return element.getSelfOrParentOfType(CatalogueItemName::class.java)
            ?.name
            ?: return element.text
    }

    override fun getType(element: PsiElement): String {
        val item = element.getSelfOrParentOfType(CatalogueItemName::class.java)
            ?: return ""
        return when (item.parent) {
            is CatalogueTag -> "Tag"
            is CatalogueArray -> "Array"
            else -> ""
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element.elementType in CatalogueTokenSets.ALL_FIND_USAGES_TOKENS
    }
}