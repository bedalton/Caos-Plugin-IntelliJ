package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.messages.MessageBus
import java.io.DataInputStream
import java.io.DataOutputStream

interface HasVariant {
    var variant: CaosVariant?
}

interface HasVariants {
    val variants:List<CaosVariant>
    companion object {
        val VARIANTS_KEY = Key<List<CaosVariant>?>("creatures.caos.variants.VARIANTS")
    }
}


class VariantsFilePropertyPusher private constructor() : FilePropertyPusher<List<CaosVariant>> {

    override fun getDefaultValue(): List<CaosVariant> = emptyList()

    override fun getFileDataKey(): Key<List<CaosVariant>?> {
        return HasVariants.VARIANTS_KEY
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): List<CaosVariant> {
        if (file == null)
            return emptyList()
        return (file as? CaosVirtualFile)?.variants
                ?: file.getUserData(HasVariants.VARIANTS_KEY)
                ?: readFromStorage(file).nullIfEmpty()
                ?: file.getModule(project)?.variant?.let { variant ->  listOf(variant) }
                ?: emptyList()
    }

    override fun getImmediateValue(module: Module): List<CaosVariant> {
        return emptyList()
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variants: List<CaosVariant>) {
        writeToStorage(file, variants)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file is CaosVirtualFile || (file.fileType as? LanguageFileType) is CaosScriptFileType
    }

    companion object {
        private val VARIANTS_FILE_ATTRIBUTE = FileAttribute("caos_script_variants", 0, true)

        internal fun readFromStorage(file: VirtualFile): List<CaosVariant> {
            if (file !is VirtualFileWithId)
                return emptyList()
            val stream = VARIANTS_FILE_ATTRIBUTE.readAttribute(file)
                    ?: return emptyList()
            return (0 until stream.readInt()).mapNotNull {
                readVariant(stream)
            }
        }

        private fun readVariant(stream:DataInputStream) : CaosVariant? {
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

        internal fun writeToStorage(file: VirtualFile, variants: List<CaosVariant>) {
            val stream = VARIANTS_FILE_ATTRIBUTE.writeAttribute(file)
            stream.write(variants.size)
            variants.forEach {variant->
                writeToStorage(stream, variant)
            }

        }

        private fun writeToStorage(stream: DataOutputStream, variantIn: CaosVariant) {
            val variant = variantIn.code
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }

        fun readFromStorageCatching(file:VirtualFile) : List<CaosVariant> {
            return try {
                readFromStorage(file)
            } catch (e:Exception) {
                emptyList()
            }
        }
    }

    override fun initExtra(project: Project, messageBus: MessageBus, engine: FilePropertyPusher.Engine) {

    }

}