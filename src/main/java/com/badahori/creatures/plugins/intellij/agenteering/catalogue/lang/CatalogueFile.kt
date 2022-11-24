package com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.setVariantBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
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

    override fun toString(): String {
        return "CATALOGUE File"
    }
}