package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleSettingsComponent
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.messages.MessageBus

private const val NOTIFICATION_ERROR_TAG = "CAOS Project: Error"
private const val NOTIFICATION_WARN_TAG = "CAOS Project: Warning"

fun errorNotification(project: Project? = null, message: String, title: String = "Error") {
    Notifications.Bus.notify(Notification(
            NOTIFICATION_ERROR_TAG,
            title,
            message,
            NotificationType.ERROR), project)
}
fun warningNotification(project: Project? = null, message: String, title: String = "Error") {
    Notifications.Bus.notify(Notification(
            NOTIFICATION_WARN_TAG,
            title,
            message,
            NotificationType.WARNING), project)
}

val Module.settings:CaosModuleSettingsService get() {
    return ModuleServiceManager.getService(this, CaosModuleSettingsService::class.java)!!
}

var Module.variant: CaosVariant?
get() {
    return settings.getState().variant
} set(newVariant) {
    settings.getState().variant = newVariant
}
    /*
    get() {
        val virtualFile = moduleFile
                ?: return CaosScriptProjectSettings.variant
        return VariantFilePropertyPusher.readFromStorage(virtualFile)
                ?: getUserData(CaosScriptFile.VariantUserDataKey)
    }
    set(newVariant) {
        val virtualFile = moduleFile
                ?: return
        VariantFilePropertyPusher.writeToStorage(virtualFile, newVariant ?: CaosVariant.UNKNOWN)
        virtualFile.putUserData(CaosScriptFile.VariantUserDataKey, newVariant ?: CaosVariant.UNKNOWN)
        FileContentUtil.reparseFiles(project, listOf(virtualFile), true)
    }*/


class VariantFilePropertyPusher private constructor() : FilePropertyPusher<CaosVariant?> {

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
            if (file is CaosVirtualFile)
                file.variant = variantIn
            if (file !is VirtualFileWithId)
                return
            val stream = VARIANT_FILE_ATTRIBUTE.writeAttribute(file)
            val variant = variantIn.code
            stream.writeInt(variant.length)
            stream.writeChars(variant)
            stream.close()
        }
    }

    override fun initExtra(project: Project, messageBus: MessageBus, engine: FilePropertyPusher.Engine) {

    }

}
