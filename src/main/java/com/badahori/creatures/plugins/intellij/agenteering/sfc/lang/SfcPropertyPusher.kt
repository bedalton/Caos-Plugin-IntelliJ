@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFileDataHolder
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.google.gson.Gson
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute

class SfcDecompiledFilePropertyPusher private constructor() : FilePropertyPusher<SfcFileDataHolder?> {

    override fun getDefaultValue(): SfcFileDataHolder = SfcFileDataHolder()

    override fun getFileDataKey(): Key<SfcFileDataHolder?> {
        return SFC_DECOMPILED_DATA_KEY
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(project: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): SfcFileDataHolder? {
        if (file == null)
            return null
        return file.getUserData(SFC_DECOMPILED_DATA_KEY)
                ?: try {
                    readFromStorage(file)
                } catch (e:Exception) {
                    e.rethrowAnyCancellationException()
                    LOGGER.severe("Failed to deserialize SFC data with error: ${e.message}")
                    e.printStackTrace()
                    null
                }
    }

    override fun getImmediateValue(module: Module): SfcFileDataHolder? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, data: SfcFileDataHolder) {
        try {
            writeToStorage(file, data)
        } catch (e:Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to write SFC data holder with exception: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return false
    }

    companion object {
        private val VARIANT_FILE_ATTRIBUTE = FileAttribute("caos.sfc.decompiler.SFC_DECOMPILED_DATA", 0, true)

        internal fun readFromStorage(file: VirtualFile): SfcFileDataHolder? {
            if (file is SfcVirtualFile) {
                SfcFileDataHolder(file.sfcData)
            }
            if (file !is VirtualFileWithId)
                return null
            val stream = VARIANT_FILE_ATTRIBUTE.readFileAttribute(file)
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
            try {
                return Gson().fromJson(out.toString(), SfcFileDataHolder::class.java)
            } catch (e:Exception) {
                e.rethrowAnyCancellationException()
                return null
            }
        }

        internal fun writeToStorage(file: VirtualFile, holder: SfcFileDataHolder) {
            try {
                if (file is SfcVirtualFile) {
                    file.putUserData(SFC_DECOMPILED_DATA_KEY, SfcFileDataHolder(file.sfcData))
                }
                val stream = VARIANT_FILE_ATTRIBUTE.writeFileAttribute(file)
                val json = Gson().toJson(holder)
                stream.writeInt(json.length)
                stream.writeChars(json)
                stream.close()
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
            }
        }
    }

}
