package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueEntryElement
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CatalogueFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CatalogueLanguage) {

    override fun getFileType(): FileType {
        return CatalogueFileType
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    fun getEntryNames(): List<String> {
        return getChildrenOfType(CatalogueEntryElement::class.java).mapNotNull {
            it.name.nullIfEmpty()
        }
    }

    override fun toString(): String {
        return "CATALOGUE File"
    }
}