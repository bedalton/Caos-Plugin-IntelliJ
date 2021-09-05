@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.forKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.intellij.openapi.project.Project
import java.util.*


val Project.settings: CaosProjectSettingsService get() = CaosProjectSettingsService.getInstance(this)


val CaosProjectSettingsService.ignoredFiles: List<String> get() = state.ignoredFilenames


fun CaosProjectSettingsService.setDefaultVariant(variant: CaosVariant?) {
    val state = state
    if (state.defaultVariant == variant)
        return
    loadState(state.copy(
        defaultVariant = variant
    ))
}
val CaosProjectSettingsService.defaultVariant: CaosVariant? get() = state.defaultVariant



fun CaosProjectSettingsService.addIgnoredFile(fileName: String) {
    if (fileName.isBlank())
        return
    val state = state
    if (state.ignoredFilenames.contains(fileName))
        return
    loadState(state.copy(
        ignoredFilenames = (state.ignoredFilenames + fileName).distinct()
    ))
}

fun CaosProjectSettingsService.removeIgnoredFile(fileName: String) {
    if (fileName.isBlank())
        return
    val state = state
    if (!state.ignoredFilenames.contains(fileName))
        return
    loadState(state.copy(
        ignoredFilenames = state.ignoredFilenames.filter { it != fileName }
    ))
}

fun CaosProjectSettingsService.addGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = state
    loadState(state.copy(
        gameInterfaceNames = (state.gameInterfaceNames + interfaceName).distinct()
    ))
}

fun CaosProjectSettingsService.removeGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = state
    loadState(state.copy(
        gameInterfaceNames = state.gameInterfaceNames.filter { it != interfaceName }
    ))
}

val CaosProjectSettingsService.gameInterfaceNames: List<GameInterfaceName> get() {
    return state.gameInterfaceNames
}

fun CaosProjectSettingsService.gameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
    val interfaces = gameInterfaceNames
    if (variant.nullIfUnknown() == null)
        return interfaces
    return interfaces.filter { it.isVariant(variant) }

}

private val CaosVariant.lastInterfacePrefix get() = "$code=="

fun CaosProjectSettingsService.lastInterface(variant: CaosVariant, interfaceName: GameInterfaceName) {
    val state = state
    val prefix = variant.lastInterfacePrefix
    loadState(
        state.copy(
            lastGameInterfaceNames = state.lastGameInterfaceNames.filterNot {
                it.startsWith(prefix)
            } + interfaceName.storageKey
        )
    )
}

fun CaosProjectSettingsService.gameInterfaceForKey(key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(null, key)
}

fun CaosProjectSettingsService.gameInterfaceForKey(variant: CaosVariant?, key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(variant, key)
}

fun CaosProjectSettingsService.lastInterface(variant: CaosVariant?): GameInterfaceName? {
    if (variant == null)
        return null
    val prefix = variant.lastInterfacePrefix
    val interfaces = state.gameInterfaceNames
        .filter { it.isVariant(variant) }
    return state.lastGameInterfaceNames
        .mapNotNull map@{ entry ->
            if (!entry.startsWith(prefix))
                return@map null
            val key = entry.substring(prefix.length)
            interfaces.firstOrNull { gameInterface ->
                gameInterface.keyMatches(key)
            }
        }
        .firstOrNull()
}

interface CaosProjectSettingsChangeListener: EventListener {
    fun onChange(settings: CaosProjectSettingsComponent.State)
}

var CaosProjectSettingsService.injectionCheckDisabled: Boolean
    get() = state.injectionCheckDisabled
    set(value) {
        if (state.injectionCheckDisabled == value)
            return
        loadState(
            state.copy(
                injectionCheckDisabled = value
            )
        )
    }


var CaosProjectSettingsService.useJectByDefault: Boolean
    get() = state.useJectByDefault
    set(value) {
        if (value == state.useJectByDefault)
            return
        loadState(
            state.copy(
                useJectByDefault = value
            )
        )
    }