package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
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
    : PsiFileBase(viewProvider, CaosScriptLanguage), HasVariant {
    override var variant: CaosVariant?
        get() {
            getUserData(VariantUserDataKey)?.let {
                return it
            }
            return this.virtualFile
                    ?.let {
                        (it as? CaosVirtualFile)?.variant
                                ?: VariantFilePropertyPusher.readFromStorage(it)
                                ?: it.getUserData(VariantUserDataKey)
                    }
                    ?: getUserData(VariantUserDataKey)
                    ?: (module ?: originalFile.module)?.variant
        }
        set(newVariant) {
            putUserData(VariantUserDataKey, newVariant)
            this.virtualFile
                    ?.let {
                        (it as? CaosVirtualFile)?.variant = variant
                        if (it is com.intellij.openapi.vfs.VirtualFileWithId) {
                            VariantFilePropertyPusher.writeToStorage(it, newVariant ?: CaosVariant.UNKNOWN)
                        }
                        it.putUserData(VariantUserDataKey, newVariant)
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
        val VariantUserDataKey = Key<CaosVariant?>("com.badahori.creatures.plugins.intellij.agenteering.caos.SCRIPT_VARIANT_KEY")

    }
}

val PsiFile.module: Module?
    get() {
        return virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it) }
    }

val RUN_INSPECTIONS_KEY = Key<Boolean?>("com.badahori.creatures.plugins.intellij.agenteering.caos.RUN_INSPECTIONS")

var PsiFile.runInspections:Boolean get() {
    return getUserData(RUN_INSPECTIONS_KEY) ?: true
} set(value) {
    putUserData(RUN_INSPECTIONS_KEY, value)
}