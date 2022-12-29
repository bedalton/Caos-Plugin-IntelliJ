package com.badahori.creatures.plugins.intellij.agenteering.catalogue.support

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.lexer.PrayLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

/**
 * Establishes a find usages provider
 */
class CatalogueFindUsagesProvider : FindUsagesProvider{

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
                PrayLexerAdapter(),
                TokenSet.create(CATALOGUE_STRING_LITERAL),
                TokenSet.create(CATALOGUE_COMMENT_LITERAL),
                TokenSet.create(CATALOGUE_INT)
        )
    }
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return element.text
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is CatalogueErrorItem -> "invalid item"
            is CatalogueItemName -> getItemNameDescription(element)
            is CatalogueItem -> element.text
            else -> element.text
        }
    }

    private fun getItemNameDescription(element: CatalogueItemName): String {
        val parent = element.getParentOfType(CatalogueEntryElement::class.java)
            ?: return element.text
        if (parent is CatalogueTag) {
            return "TAG ${element.text}"
        } else if (parent is CatalogueArray) {
            return "${parent.arrayKw.text}${parent.override?.let {" OVERRIDE"} ?: ""} ${element.text}${parent.count?.let { " ${it.text}"} ?: ""}"
        }
        return element.text
    }

    override fun getType(element: PsiElement): String {
        return when (element) {
            is CatalogueErrorItem -> "invalid item"
            is CatalogueItemName -> if (element.parent is CatalogueArray) "ARRAY name" else "TAG name"
            is CatalogueItem -> "TAG element"
            else -> "element"
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is CatalogueCompositeElement
    }
}