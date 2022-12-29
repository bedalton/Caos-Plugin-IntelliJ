package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueLanguage
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItem
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

object CataloguePsiElementFactory {

    fun createItemNameElement(project: Project, name: String): CatalogueItemName? {
        val comment = """
            TAG "$name"
            "nothing"
        """.trimIndent().trimIndent()
        val file = createFileFromText(project, comment)
        val tag = file.getChildOfType(CatalogueTag::class.java)
            ?: return null
        return tag.itemName
    }

    fun createStringItem(project: Project, text:String) : CatalogueItem? {
        val comment = """
            TAG "temp"
            "$text"
        """.trimIndent().trimIndent()
        val file = createFileFromText(project, comment)
        val tag = file.getChildOfType(CatalogueTag::class.java)
            ?: return null
        return tag.itemList.firstOrNull()
    }

    private fun createFileFromText(project: Project, text: String): CatalogueFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.catalogue", CatalogueLanguage, text) as CatalogueFile
    }
}