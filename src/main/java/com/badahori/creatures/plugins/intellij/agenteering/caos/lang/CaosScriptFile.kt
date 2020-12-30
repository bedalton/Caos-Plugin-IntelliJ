package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.FileContentUtilCore
import java.util.concurrent.atomic.AtomicBoolean

class CaosScriptFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosScriptLanguage), HasVariant {
    private val didFormatInitial = AtomicBoolean(false)
    override var variant: CaosVariant?
        get() {
            val storedVariant = getUserData(VariantUserDataKey)
                    ?: this.virtualFile?.cachedVariant
                    ?: (module ?: originalFile.module)?.variant
            return if (storedVariant == CaosVariant.UNKNOWN)
                null
            else
                storedVariant
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
                        if (ApplicationManager.getApplication().isDispatchThread)
                            FileContentUtilCore.reparseFiles(it)
                    }
            if (ApplicationManager.getApplication().isDispatchThread) {
                DaemonCodeAnalyzer.getInstance(project).restart(this)
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

    fun quickFormat() {
        Companion.quickFormat(this)
    }

    companion object {
        @JvmStatic
        val VariantUserDataKey = Key<CaosVariant?>("com.badahori.creatures.plugins.intellij.agenteering.caos.SCRIPT_VARIANT_KEY")

        fun quickFormat(caosFile: CaosScriptFile) {
            if (caosFile.didFormatInitial.getAndSet(true))
                return
            val project = caosFile.project
            val application = ApplicationManager.getApplication()
            when {
                application.isWriteAccessAllowed -> {
                    runQuickFormatInWriteAction(caosFile)
                }
                DumbService.isDumb(project) -> {
                    if (!application.isDispatchThread) {
                        DumbService.getInstance(project).runWhenSmart {
                            application.runWriteAction {
                                runQuickFormatInWriteAction(caosFile)
                            }
                        }
                    } else {
                        DumbService.getInstance(project).runWhenSmart {
                            application.runWriteAction {
                                runQuickFormatInWriteAction(caosFile)
                            }
                        }
                    }
                }
                application.isDispatchThread -> {
                    application.runWriteAction(Computable {
                        runQuickFormatInWriteAction(caosFile)
                    })
                }
                else -> {
                    application.invokeLater {
                        application.runWriteAction {
                            runQuickFormatInWriteAction(caosFile)
                        }
                    }
                }
            }
        }

        private fun runQuickFormatInWriteAction(caosFile: CaosScriptFile) {
            if (caosFile.virtualFile?.apply { isWritable = true }?.isWritable.orFalse())
                CaosScriptExpandCommasIntentionAction.invoke(caosFile.project, caosFile)
        }

    }
}

val PsiFile.module: Module?
    get() {
        return virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it) }
    }

val RUN_INSPECTIONS_KEY = Key<Boolean?>("com.badahori.creatures.plugins.intellij.agenteering.caos.RUN_INSPECTIONS")

var PsiFile.runInspections: Boolean
    get() {
        return getUserData(RUN_INSPECTIONS_KEY) ?: true
    }
    set(value) {
        putUserData(RUN_INSPECTIONS_KEY, value)
    }

val VirtualFile.cachedVariant: CaosVariant?
    get() = (this as? CaosVirtualFile)?.variant
            ?: VariantFilePropertyPusher.readFromStorage(this)
            ?: this.getUserData(CaosScriptFile.VariantUserDataKey)