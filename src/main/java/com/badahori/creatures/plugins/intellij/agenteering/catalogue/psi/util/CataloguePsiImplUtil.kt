package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.util

import bedalton.creatures.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueItemType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.psi.PsiElement


@Suppress("UNUSED_PARAMETER")
object CataloguePsiImplUtil {

    @JvmStatic
    fun getName(tag: CatalogueTag): String? {
        return tag.stub?.name
            ?: tag.text.stripSurroundingQuotes(1)
                .nullIfEmpty()
    }

    @JvmStatic
    fun setName(element: CatalogueTag, name: String): PsiElement {
        val newName = CataloguePsiElementFactory.createItemNameElement(element.project, name)
            ?: return element
        element.itemName?.let { itemName ->
            return itemName.replace(newName)
        }
        return element.addAfter(newName, element.tagKw)
    }

    @JvmStatic
    fun getNameIdentifier(element: CatalogueTag): PsiElement? {
        return element.itemName
    }

    @JvmStatic
    fun getItemCount(element: CatalogueTag): Int {
        return element.stub?.itemCount ?: element.itemList.size
    }

    @JvmStatic
    fun getType(element: CatalogueTag): CatalogueItemType {
        return CatalogueItemType.TAG
    }

    @JvmStatic
    fun getName(array: CatalogueArray): String? {
        return array.stub?.name
            ?: array.text.stripSurroundingQuotes(1)
                .nullIfEmpty()
    }

    @JvmStatic
    fun setName(element: CatalogueArray, name: String): PsiElement {
        val newName = CataloguePsiElementFactory.createItemNameElement(element.project, name)
            ?: return element
        element.itemName?.let { itemName ->
            return itemName.replace(newName)
        }
        return element.addAfter(newName, element.override ?: element.arrayKw)
    }

    @JvmStatic
    fun getItemCount(element: CatalogueArray): Int {
        return element.stub?.itemCount ?: element.itemList.size
    }


    @JvmStatic
    fun getExpectedValueCount(element: CatalogueArray): Int {
        return element.stub?.expectedValuesCount
            ?: element.count?.text?.toIntOrNull()
            ?: 0
    }

    @JvmStatic
    fun getType(element: CatalogueArray): CatalogueItemType {
        return CatalogueItemType.ARRAY
    }

    @JvmStatic
    fun getNameIdentifier(element: CatalogueArray): PsiElement? {
        return element.itemName
    }

    @JvmStatic
    fun isOverride(element: CatalogueArray): Boolean {
        return element.stub?.override ?: (element.override != null)
    }

    @JvmStatic
    fun getName(element: CatalogueItemName): String {
        return element.text.stripSurroundingQuotes(1)
    }

    @JvmStatic
    fun setName(element: CatalogueItemName, name: String): PsiElement {
        val newName = CataloguePsiElementFactory.createItemNameElement(element.project, name)
            ?: return element
        return element.replace(newName)
    }

    @JvmStatic
    fun getItemsAsStrings(tag: CatalogueTag): List<String> {
        return tag.stub?.items ?: tag.itemList.map { it.text.stripSurroundingQuotes(true) }
    }


    @JvmStatic
    fun getItemsAsStrings(tag: CatalogueArray): List<String> {
        return tag.stub?.items ?: tag.itemList.map { it.text.stripSurroundingQuotes(true) }
    }
}
