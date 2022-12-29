package com.badahori.creatures.plugins.intellij.agenteering.catalogue.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.colorize
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class CatalogueSemanticHighlighter: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is CatalogueItemName -> annotateItemName(element, holder)
        }
    }

    private fun annotateItemName(itemName: CatalogueItemName, holder: AnnotationHolder) {
        val color = when (itemName.parent) {
            is CatalogueTag -> CatalogueSyntaxHighlighter.TAG_NAME
            is CatalogueArray -> CatalogueSyntaxHighlighter.ARRAY_NAME
            else -> CatalogueSyntaxHighlighter.NAME
        }
        holder.colorize(itemName, color)
    }
}