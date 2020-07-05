package com.openc2e.plugins.intellij.agenteering.caos.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub

class CaosScriptFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosScriptLanguage.instance) {

        /*
        get() {

            val virtualFile = virtualFile
                    ?: return ServiceManager.getService(CaosScriptProjectSettingsService::class.java)?.state?.baseVariant
                            ?: CaosScriptProjectSettingsService.DEFAULT_VARIANT
            return virtualFile.getUserData(VariantUserDataKey) ?: VariantFilePropertyPusher.readFromStorage(virtualFile)
            ?: ""
        }
        set(newVariant) {
            val virtualFile = virtualFile
                    ?: return
            virtualFile.putUserData(VariantUserDataKey, newVariant)
            VariantFilePropertyPusher.writeToStorage(virtualFile, newVariant)
            FileContentUtil.reparseFiles(project, listOf(virtualFile), true)
        }*/

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
        val VariantUserDataKey = Key<CaosVariant>("com.openc2e.plugins.intellij.agenteering.caos.SCRIPT_VARIANT_KEY")

    }
}

private class VariantFilePropertyPusher : FilePropertyPusher<CaosVariant> {

    override fun getDefaultValue(): CaosVariant = CaosScriptProjectSettings.variant

    override fun getFileDataKey(): Key<CaosVariant> {
        return CaosScriptFile.VariantUserDataKey
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): CaosVariant? {
        if (file == null)
            return null
        return file.getUserData(CaosScriptFile.VariantUserDataKey)
                ?: readFromStorage(file)?.let { CaosVariant.fromVal(it) }
    }

    override fun getImmediateValue(module: Module): CaosVariant? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: CaosVariant) {
        writeToStorage(file, variant.code)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return (file.fileType as? LanguageFileType) is CaosScriptFileType
    }

    companion object {
        private val VARIANT_FILE_ATTRIBUTE = FileAttribute("caos_script_variant", 0, true)

        internal fun readFromStorage(file: VirtualFile): String? {
            val stream = VARIANT_FILE_ATTRIBUTE.readAttribute(file)
                    ?: return null
            val length = stream.readInt()
            val out = StringBuilder()
            if (length <= 0) {
                return null
            }
            (0 until length).forEach { _ ->
                out.append(stream.readChar())
            }
            stream.close()
            return out.toString()
        }

        internal fun writeToStorage(file: VirtualFile, variant: String) {
            val stream = VARIANT_FILE_ATTRIBUTE.writeAttribute(file)
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }
    }

}

val VirtualFile.variant: CaosVariant
    get() {
        return getUserData(CaosScriptFile.VariantUserDataKey)
                ?: VariantFilePropertyPusher.readFromStorage(this)?.let { CaosVariant.fromVal(it) }
                ?: CaosScriptProjectSettings.variant
    }


val CaosScriptFile?.variant: CaosVariant
    get() = CaosScriptProjectSettings.variant