package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute


internal open class AbstractVariantFilePropertyPusher constructor(
    val key: Key<CaosVariant?>,
    val fileAttribute: FileAttribute
) : FilePropertyPusher<CaosVariant?> {

    override fun getDefaultValue(): CaosVariant = CaosVariant.UNKNOWN

    override fun getFileDataKey(): Key<CaosVariant?> = key

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): CaosVariant? {
        if (file == null)
            return null
        return (file as? CaosVirtualFile)?.variant
            ?: file.getUserData(key).nullIfUnknown()
            ?: readFromStorage(file, fileAttribute, key)
    }

    override fun getImmediateValue(module: Module): CaosVariant? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: CaosVariant) {
        writeToStorage(file, variant, fileAttribute)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file is CaosVirtualFile || (file.fileType as? LanguageFileType) is CaosScriptFileType
    }
}

private val VARIANT_EXPLICIT_FILE_ATTRIBUTE = FileAttribute("caos_script_variant_explicit", 0, true)
private val VARIANT_IMPLICIT_FILE_ATTRIBUTE = FileAttribute("caos_script_variant_implicit", 0, true)

internal class ExplicitVariantFilePropertyPusher : AbstractVariantFilePropertyPusher(
    key = CaosScriptFile.ExplicitVariantUserDataKey,
    fileAttribute = VARIANT_EXPLICIT_FILE_ATTRIBUTE
) {
    companion object {
        fun writeToStorage(file: VirtualFile, variantIn: CaosVariant?) {
            writeToStorage(file, variantIn, VARIANT_EXPLICIT_FILE_ATTRIBUTE)
        }
        fun readFromStorage(file: VirtualFile): CaosVariant? {
            return readFromStorage(file, VARIANT_EXPLICIT_FILE_ATTRIBUTE, CaosScriptFile.ExplicitVariantUserDataKey)
        }
    }
}

internal class ImplicitVariantFilePropertyPusher : AbstractVariantFilePropertyPusher(
    key = CaosScriptFile.ImplicitVariantUserDataKey,
    fileAttribute = VARIANT_IMPLICIT_FILE_ATTRIBUTE
) {
    companion object {
        fun writeToStorage(file: VirtualFile, variantIn: CaosVariant?) {
            writeToStorage(file, variantIn, VARIANT_IMPLICIT_FILE_ATTRIBUTE)
        }
        fun readFromStorage(file: VirtualFile): CaosVariant? {
            return readFromStorage(file, VARIANT_IMPLICIT_FILE_ATTRIBUTE, CaosScriptFile.ImplicitVariantUserDataKey)
        }
    }
}
private fun readFromStorage(file: VirtualFile, fileAttribute: FileAttribute, key: Key<CaosVariant?>): CaosVariant? {
    // Attempt to read variant from virtual file as CaosVirtualFile
    // It allows fall through in case CaosVirtualFile
    // Ever extends VirtualFileWithId
    (file as? CaosVirtualFile)?.variant?.let {
        return it
    }
    file.getUserData(key).nullIfUnknown()?.let {
        return it
    }
    // If file is not virtual file
    // Bail out as only VirtualFileWithId files
    // Have data that could be read through the stream.
    if (file is VirtualFileWithId) {
        return read(
            file,
            fileAttribute
        )
    }

    return null
}

private fun read(file: VirtualFile, attribute: FileAttribute): CaosVariant? {
    if (file is CaosVirtualFile)
        return file.variant

    if (file !is VirtualFileWithId) {
        LOGGER.info("${file.className} !is VirtualFileWithId")
        return null
    }

    val stream = attribute.readAttribute(file)
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
    return CaosVariant.fromVal(out.toString()).nullIfUnknown()
}



private fun writeToStorage(file: VirtualFile, variantIn: CaosVariant?, fileAttribute: FileAttribute) {
    if (file is CaosVirtualFile) {
        file.setVariant(variantIn, true)
        return
    }
    if (file !is VirtualFileWithId)
        return
    val stream = fileAttribute.writeAttribute(file)
    val variant = variantIn?.code ?: ""
    stream.writeInt(variant.length)
    stream.writeChars(variant)
    stream.close()
}