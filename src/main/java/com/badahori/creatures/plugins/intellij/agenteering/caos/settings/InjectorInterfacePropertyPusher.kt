package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute

internal class InjectorInterfacePropertyPusher private constructor() : FilePropertyPusher<GameInterfaceName?> {

    override fun getDefaultValue(): GameInterfaceName = GameInterfaceName(CaosVariant.UNKNOWN)

    override fun getFileDataKey(): Key<GameInterfaceName?> {
        return INJECTOR_INTERFACE_USER_DATA_KEY
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): GameInterfaceName? {
        if (file == null)
            return null
        return file.getUserData(INJECTOR_INTERFACE_USER_DATA_KEY)
            ?: readFromStorage(project, file)
    }

    override fun getImmediateValue(module: Module): GameInterfaceName? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: GameInterfaceName) {
        writeToStorage(file, variant)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file is CaosVirtualFile || (file.fileType as? LanguageFileType) is CaosScriptFileType
    }

    companion object {
        private val INJECTOR_ATTRIBUTE = FileAttribute("caos.CAOS_FILE_INJECTOR", 0, true)

        internal fun readFromStorage(project: Project, file: VirtualFile): GameInterfaceName? {
            file.getUserData(INJECTOR_INTERFACE_USER_DATA_KEY)?.let {
                return it
            }
            // If file is not virtual file
            // Bail out as only VirtualFileWithId files
            // Have data that could be read through the stream.
            if (file !is VirtualFileWithId) {
                // Get possible session user data written on top of this file
                return null
            }

            val stream = INJECTOR_ATTRIBUTE.readAttribute(file)
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
            val key = out.toString().nullIfEmpty()
                ?: return null
            return project.settings.gameInterfaceForKey(null, key)
        }

        internal fun writeToStorage(file: VirtualFile, gameInterfaceName: GameInterfaceName?) {
            if (file !is VirtualFileWithId)
                return
            file.putUserData(INJECTOR_INTERFACE_USER_DATA_KEY, gameInterfaceName)
            val stream = INJECTOR_ATTRIBUTE.writeAttribute(file)
            val variant = gameInterfaceName?.storageKey ?: ""
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }
    }
}

internal val INJECTOR_INTERFACE_USER_DATA_KEY =
    Key<GameInterfaceName?>("com.badahori.creatures.plugins.intellij.agenteering.caos.INJECTOR_INTERFACE_NAME")
