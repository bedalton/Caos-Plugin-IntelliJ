package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.gameInterfaceForKey
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile

private const val NOTIFICATION_ERROR_TAG = "CAOS Project: Error"
private const val NOTIFICATION_WARN_TAG = "CAOS Project: Warning"

fun errorNotification(project: Project? = null, message: String, title: String = "Error") {
    Notifications.Bus.notify(
        Notification(
            NOTIFICATION_ERROR_TAG,
            title,
            message,
            NotificationType.ERROR
        ), project
    )
}

fun warningNotification(project: Project? = null, message: String, title: String = "Error") {
    Notifications.Bus.notify(
        Notification(
            NOTIFICATION_WARN_TAG,
            title,
            message,
            NotificationType.WARNING
        ), project
    )
}

val Module.settings: CaosModuleSettingsService
    get() {
        return CaosModuleSettingsService.getInstance(this)!!
    }

var Module.variant: CaosVariant?
    get() {
        return settings.getState().variant ?: getUserData(CaosScriptFile.ExplicitVariantUserDataKey)
    }
    set(newVariant) {
        val settings = settings
        val state = settings.getState()
        putUserData(CaosScriptFile.ExplicitVariantUserDataKey, newVariant)
        settings.loadState(
            state.copy(
                variant = newVariant,
            )
        )
    }

internal fun CaosModuleSettingsService.addIgnoredFile(fileName: String) {
    val state = getState()
    this.loadState(
        state.copy(
            ignoredFiles = state.ignoredFiles + fileName
        )
    )
}

internal fun CaosModuleSettingsService.removeIgnoredFile(fileName: String) {
    val state = getState()
    this.loadState(state.copy(
        ignoredFiles = state.ignoredFiles.filter { it notLike fileName }
    ))
}

internal var CaosModuleSettingsService.ignoredFiles
    get() = getState().ignoredFiles
    set(files) {
        val state = getState()
        this.loadState(
            state.copy(
                ignoredFiles = files
            )
        )
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




internal fun CaosModuleSettingsService.lastGameInterface(): GameInterfaceName? {
    val state = getState()
    val variant = state.variant
        ?: return null
    val key = state.lastGameInterfaceName
        ?: return null
    return CaosApplicationSettingsService.getInstance().gameInterfaceForKey(variant, key)
}

internal fun CaosModuleSettingsService.lastGameInterface(gameInterfaceName: GameInterfaceName) {
    val state = getState()
    val variant = state.variant
    if (gameInterfaceName.isVariant(variant))
        loadState(
            state.copy(
                lastGameInterfaceName = gameInterfaceName.id
            )
        )
}

internal val Module.myModuleFile: VirtualFile? get() = rootManager.contentRoots.firstOrNull()
internal val Module.myModulePath: String? get() = rootManager.contentRoots.firstOrNull()?.path