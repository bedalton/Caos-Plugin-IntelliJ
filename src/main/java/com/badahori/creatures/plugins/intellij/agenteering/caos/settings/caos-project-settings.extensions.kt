@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.injector.forKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.filterNotNull
import com.intellij.openapi.project.Project


val Project.settings: CaosProjectSettingsService get() = CaosProjectSettingsService.getInstance(this)


val CaosProjectSettingsService.ignoredFiles: List<String> get() = stateNonNull.ignoredFilenames


/**
 * Sets the project wide default variant
 */
fun CaosProjectSettingsService.setDefaultVariant(variant: CaosVariant?) {
    val state = stateNonNull
    if (state.defaultVariant == variant)
        return
    loadState(
        state.copy(
            defaultVariant = variant
        )
    )
}


/**
 * Add a file to the ignored list that will no longer be checked for validity
 */
fun CaosProjectSettingsService.addIgnoredFile(fileName: String) {
    if (fileName.isBlank())
        return
    val state = stateNonNull
    if (state.ignoredFilenames.contains(fileName)) {
        return
    }
    loadState(
        state.copy(
            ignoredFilenames = (state.ignoredFilenames + fileName).distinct()
        )
    )
}

/**
 * Remove a file from the ignored list, so it will be checked for existence in project
 */
fun CaosProjectSettingsService.removeIgnoredFile(fileName: String) {
    if (fileName.isBlank())
        return
    val state = stateNonNull
    if (!state.ignoredFilenames.contains(fileName)) {
        return
    }
    loadState(state.copy(
        ignoredFilenames = state.ignoredFilenames.filter { it != fileName }
    ))
}

/**
 * Adds a game interface name used to communicate with a running instance of Creatures on the OS
 */
fun CaosInjectorApplicationSettingsService.addGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = stateNonNull
    loadState(
        state.copy(
            gameInterfaceNames = (state.gameInterfaceNames + interfaceName)
                .distinct()
                .filter { it != null && (it !is NativeInjectorInterface || !it.isDefault) }
        )
    )
}

/**
 * Removes a game interface name from the project
 * GAME interface names are used to communicate with running Creatures instances
 */
fun CaosInjectorApplicationSettingsService.removeGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = stateNonNull
    loadState(state.copy(
        gameInterfaceNames = state.gameInterfaceNames
            .filter { it != null && it != interfaceName }
    ))
}

/**
 * List all game interface names, formatted, with asterisks(*) expanded
 */
val CaosInjectorApplicationSettingsService.allGameInterfaceNames: List<GameInterfaceName>
    get() {
        return stateNonNull.gameInterfaceNames
            .filterNotNull()
            .flatMap { gameInterfaceName ->
                if (gameInterfaceName.variant != CaosVariant.ANY) {
                    listOf(gameInterfaceName)
                } else {
                    CaosConstants
                        .VARIANTS
                        .map { variant ->
                            gameInterfaceName.withCode(
                                code = variant.code
                            )
                        }
                }
            }
    }

/**
 * Gets game interface names for a variant after expanding asterisks(*)
 */
fun CaosInjectorApplicationSettingsService.gameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
    val interfaces = allGameInterfaceNames.filterNotNull()
    if (variant.nullIfUnknown() == null) {
        return interfaces
    }
    return interfaces.filter { it.isVariant(variant) }

}

/**
 * A key for storing the last interface name used for a variant in the project
 */
private val CaosVariant.lastInterfacePrefix get() = "$code=="

fun CaosInjectorApplicationSettingsService.lastInterface(variant: CaosVariant, interfaceName: GameInterfaceName) {
    val state = stateNonNull
    val prefix = variant.lastInterfacePrefix
    loadState(
        state.copy(
            lastGameInterfaceNames = state.lastGameInterfaceNames.filterNot {
                it.startsWith(prefix)
            } + (prefix + interfaceName.id)
        )
    )
}

fun CaosInjectorApplicationSettingsService.gameInterfaceForKey(key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(null, key)
}

fun CaosInjectorApplicationSettingsService.gameInterfaceForKey(variant: CaosVariant?, key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(variant, key)
}

fun CaosInjectorApplicationSettingsService.lastInterface(variant: CaosVariant?): GameInterfaceName? {
    if (variant == null)
        return null
    val prefix = variant.lastInterfacePrefix
    return stateNonNull.lastGameInterfaceNames
        .mapNotNull map@{ entry ->
            if (!entry.startsWith(prefix))
                return@map null
            val key = entry.substring(prefix.length)
            allGameInterfaceNames.forKey(variant, key)
        }
        .firstOrNull()
}


/**
 * Gets/Sets whether to check a file for valid CAOS before injecting
 * This can be used when the plugin does not account for a command, or one is incorrectly
 * described
 */
var CaosProjectSettingsService.injectionCheckDisabled: Boolean
    get() = stateNonNull.injectionCheckDisabled
    set(value) {
        val state = stateNonNull
        if (state.injectionCheckDisabled == value) {
            return
        }
        loadState(
            state.copy().apply {
                disableInjectionCheck(value)
            }
        )
    }

/**
 * Flatten ATTs for duplicate images
 * Used for things like the front facing ATTs with C1e -> C2e conversions
 */
var CaosApplicationSettingsService.replicateAttToDuplicateSprites: Boolean?
    get() = state.replicateAttsToDuplicateSprites
    set(value) {
        if (value == state.replicateAttsToDuplicateSprites)
            return
        loadState(
            state.copy(
                replicateAttsToDuplicateSprites = value != false
            )
        )
    }
