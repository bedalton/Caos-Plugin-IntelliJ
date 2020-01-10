package com.openc2e.plugins.intellij.caos.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.FileContentUtil
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptFileStub
import java.lang.StringBuilder
import com.intellij.openapi.roots.impl.FilePropertyPusher as FilePropertyPusher

class CaosScriptFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosScriptLanguage.instance) {

    var variant:String get () {
        val virtualFile = virtualFile
                ?: return ""
        return virtualFile.getUserData(VariantUserDataKey) ?:
                VariantFilePropertyPusher.readFromStorage(virtualFile)
                ?: ""
    } set(newVariant) {
        val virtualFile = virtualFile
                ?: return
        virtualFile.putUserData(VariantUserDataKey, newVariant)
        VariantFilePropertyPusher.writeToStorage(virtualFile, newVariant)
        FileContentUtil.reparseFiles(virtualFile)
    }

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub():CaosScriptFileStub? {
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
        val VariantUserDataKey = Key<String> ("com.openc2e.plugins.intellij.caos.SCRIPT_VARIANT_KEY")

    }
}

private class VariantFilePropertyPusher : FilePropertyPusher<String> {

    override fun getDefaultValue(): String = ""

    override fun getFileDataKey(): Key<String> {
        return CaosScriptFile.VariantUserDataKey
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) { }

    override fun getImmediateValue(project: Project, file: VirtualFile?): String? {
        if (file == null)
            return null;
        return file.getUserData(CaosScriptFile.VariantUserDataKey)
                ?: readFromStorage(file)
                ?: ""
    }
    override fun getImmediateValue(module: Module): String? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: String) {
        writeToStorage(file, variant)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return (file.fileType as? LanguageFileType) is CaosScriptFileType
    }

    companion object {
        private val VARIANT_FILE_ATTRIBUTE = FileAttribute("caos_script_variant", 0, true)

        internal fun readFromStorage(file:VirtualFile) : String? {
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

        internal fun writeToStorage(file:VirtualFile, variant:String) {
            val stream = VARIANT_FILE_ATTRIBUTE.writeAttribute(file)
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }
    }

}

