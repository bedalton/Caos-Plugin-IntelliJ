package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute

internal class VariantFilePropertyPusher private constructor() : FilePropertyPusher<CaosVariant?> {

    override fun getDefaultValue(): CaosVariant = CaosVariant.UNKNOWN

    override fun getFileDataKey(): Key<CaosVariant?> {
        return CaosScriptFile.VariantUserDataKey
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): CaosVariant? {
        if (file == null)
            return null
        return (file as? CaosVirtualFile)?.variant
            ?: file.getUserData(CaosScriptFile.VariantUserDataKey)
            ?: readFromStorage(file)
    }

    override fun getImmediateValue(module: Module): CaosVariant? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: CaosVariant) {
        writeToStorage(file, variant)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file is CaosVirtualFile || (file.fileType as? LanguageFileType) is CaosScriptFileType
    }

    companion object {
        private val VARIANT_FILE_ATTRIBUTE = FileAttribute("caos_script_variant", 0, true)

        internal fun readFromStorage(file: VirtualFile): CaosVariant? {
            // Attempt to read variant from virtual file as CaosVirtualFile
            // It allows fall through in case CaosVirtualFile
            // Ever extends VirtualFileWithId
            (file as? CaosVirtualFile)?.variant?.let {
                return it
            }
            // If file is not virtual file
            // Bail out as only VirtualFileWithId files
            // Have data that could be read through the stream.
            if (file !is VirtualFileWithId) {
                // Get possible session user data written on top of this file
                return file.getUserData(CaosScriptFile.VariantUserDataKey)
            }
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
            return CaosVariant.fromVal(out.toString())
        }

        internal fun writeToStorage(file: VirtualFile, variantIn: CaosVariant) {
            if (file is CaosVirtualFile) {
                file.variant = variantIn
                return
            }
            if (file !is VirtualFileWithId)
                return
            val stream = VARIANT_FILE_ATTRIBUTE.writeAttribute(file)
            val variant = variantIn.code
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }
    }
}