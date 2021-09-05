package com.badahori.creatures.plugins.intellij.agenteering.att.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.FileContentUtilCore

class AttFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, AttLanguage), HasVariant {
    override var variant: CaosVariant?
        get() {
            val storedVariant = getUserData(CaosScriptFile.VariantUserDataKey)
                ?: this.virtualFile?.cachedVariant
                ?: (module ?: originalFile.module)?.variant
            return if (storedVariant == CaosVariant.UNKNOWN)
                null
            else
                storedVariant
        }
        set(newVariant) {
            putUserData(CaosScriptFile.VariantUserDataKey, newVariant)
            this.virtualFile
                ?.let {
                    (it as? CaosVirtualFile)?.variant = variant
                    if (it is com.intellij.openapi.vfs.VirtualFileWithId) {
                        VariantFilePropertyPusher.writeToStorage(it, newVariant ?: CaosVariant.UNKNOWN)
                    }
                    it.putUserData(CaosScriptFile.VariantUserDataKey, newVariant)
                    if (ApplicationManager.getApplication().isDispatchThread)
                        FileContentUtilCore.reparseFiles(it)
                }
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