package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.bedalton.creatures.common.structs.GameVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
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
        writeToStorage(file, fileAttribute, key, variant)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
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
            writeToStorage(file, VARIANT_EXPLICIT_FILE_ATTRIBUTE, CaosScriptFile.ExplicitVariantUserDataKey, variantIn)
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
            writeToStorage(file, VARIANT_IMPLICIT_FILE_ATTRIBUTE, CaosScriptFile.ImplicitVariantUserDataKey, variantIn)
        }
        fun readFromStorage(file: VirtualFile): CaosVariant? {
            return readFromStorage(file, VARIANT_IMPLICIT_FILE_ATTRIBUTE, CaosScriptFile.ImplicitVariantUserDataKey).let {
                if (file.extension.orEmpty().lowercase() == "att"  && it == CaosVariant.CV) {
                    null
                } else {
                    it
                }
            }
        }
    }
}

private fun readFromStorage(file: VirtualFile, fileAttribute: FileAttribute, key: Key<CaosVariant?>): CaosVariant? {
    return com.badahori.creatures.plugins.intellij.agenteering.utils.readFromStorage(
        file,
        fileAttribute,
        key,
        CaosVariant::fromVal
    )
}



private fun writeToStorage(file: VirtualFile, fileAttribute: FileAttribute, key: Key<CaosVariant?>, variantIn: CaosVariant?) {
    com.badahori.creatures.plugins.intellij.agenteering.utils.writeToStorage(
        file,
        fileAttribute,
        key,
        variantIn
    ) {
        variantIn?.code
    }
}