@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.injector.CorruptInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.injector.NoneInjectorInterface
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

    override fun getDefaultValue(): GameInterfaceName = NoneInjectorInterface

    override fun getFileDataKey(): Key<GameInterfaceName?> {
        return INJECTOR_INTERFACE_USER_DATA_KEY
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): GameInterfaceName? {
        if (file == null)
            return null
        return file.getUserData(INJECTOR_INTERFACE_USER_DATA_KEY)
            ?: readFromStorage(file, null)
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

    companion object {
        private val INJECTOR_ATTRIBUTE = FileAttribute("caos.CAOS_FILE_INJECTOR", 0, true)

        internal fun readFromStorage(file: VirtualFile, variant: CaosVariant?): GameInterfaceName? {
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

            val stream = INJECTOR_ATTRIBUTE.readFileAttribute(file)
                ?: return null
            val length = stream.readInt()
            if (length <= 0) {
                return null
            }
            val out = StringBuilder()
            (0 until length).forEach { _ ->
                out.append(stream.readChar())
            }
            stream.close()
            val key = out.toString().nullIfEmpty()
                ?: return null
            return CaosInjectorApplicationSettingsService.getInstance().gameInterfaceForKey(variant, key)
        }

        internal fun writeToStorage(file: VirtualFile, gameInterfaceName: GameInterfaceName?) {
            if (file !is VirtualFileWithId)
                return

            if (gameInterfaceName == NoneInjectorInterface) {
                return
            }
            if (gameInterfaceName is CorruptInjectorInterface) {
                return
            }
            file.putUserData(INJECTOR_INTERFACE_USER_DATA_KEY, gameInterfaceName)
            val stream = INJECTOR_ATTRIBUTE.writeFileAttribute(file)
            val name = gameInterfaceName?.id ?: ""
            stream.writeInt(name.length)
            stream.writeChars(name)
            stream.close()
        }
    }
}

internal val INJECTOR_INTERFACE_USER_DATA_KEY =
    Key<GameInterfaceName?>("com.badahori.creatures.plugins.intellij.agenteering.caos.INJECTOR_INTERFACE_NAME")
