package com.badahori.creatures.plugins.intellij.agenteering.att.lang

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

class AttFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, AttLanguage), HasVariant {
    override val variant: CaosVariant?
        get() {
            val storedVariant =  getUserData(CaosScriptFile.ExplicitVariantUserDataKey)
                ?: getUserData(CaosScriptFile.ImplicitVariantUserDataKey)
                ?: this.virtualFile?.cachedVariantExplicitOrImplicit
                ?: (module ?: originalFile.module)?.variant
                ?: (project.settings.defaultVariant)
            return if (storedVariant == CaosVariant.UNKNOWN) {
                null
            } else {
                storedVariant
            }
        }

    override fun setVariant(variant: CaosVariant?, explicit: Boolean) {
        setVariantBase(virtualFile, variant, true)
        if (ApplicationManager.getApplication().isDispatchThread) {
            DaemonCodeAnalyzer.getInstance(project).restart(this)
        }
    }

    override fun getFileType(): FileType {
        return AttFileType
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "ATT File"
    }
}