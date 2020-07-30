package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosScriptLanguage.instance) {
    var variant: CaosVariant?
        get() {
            val module = module ?: originalFile.module
            return module?.variant ?: (this.virtualFile ?: this.originalFile.virtualFile)
                    ?.let {
                        VariantFilePropertyPusher.readFromStorage(it)
                                ?: it.getUserData(VariantUserDataKey)
                    }
                    ?: CaosScriptProjectSettings.variant
        }
        set(newVariant) {
            (this.virtualFile ?: this.originalFile.virtualFile)
                    ?.let {
                        VariantFilePropertyPusher.writeToStorage(it, newVariant ?: CaosVariant.UNKNOWN)
                        it.putUserData(VariantUserDataKey, newVariant ?: CaosVariant.UNKNOWN)
                    }
        }

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub(): CaosScriptFileStub? {
        return super.getStub() as? CaosScriptFileStub
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Script"
    }

    companion object {
        @JvmStatic
        val VariantUserDataKey = Key<CaosVariant>("com.badahori.creatures.plugins.intellij.agenteering.caos.SCRIPT_VARIANT_KEY")

    }
}

val PsiFile.module: Module?
    get() {
        return virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it) }
    }