@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.forKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.*


val Project.settings: CaosProjectSettingsService get() = CaosProjectSettingsService.getInstance(this)


val CaosProjectSettingsService.ignoredFiles: List<String> get() = state.ignoredFilenames


fun CaosProjectSettingsService.setDefaultVariant(variant: CaosVariant?) {
    val state = state
    if (state.defaultVariant == variant)
        return
    loadState(
        state.copy(
            defaultVariant = variant
        )
    )
}

val CaosProjectSettingsService.defaultVariant: CaosVariant? get() = state.defaultVariant

fun Module.inferVariantHard(): CaosVariant? {
    // Get variant normally if possible
    variant?.let {
        return it
    }

    val scope = getModuleScope(false)
    return variantInScope(project, scope)
}

fun Project.inferVariantHard(): CaosVariant? {
    settings.defaultVariant?.let {
        return it
    }
    val scope = GlobalSearchScope.everythingScope(this)
    return variantInScope(this, scope)
}

private fun variantInScope(project: Project, scope: GlobalSearchScope): CaosVariant? {
    val hasSpr = FilenameIndex.getAllFilesByExt(project, "spr", scope).isNotEmpty()
    val hasS16 = FilenameIndex.getAllFilesByExt(project, "s16", scope).isNotEmpty()
    val hasC16 = FilenameIndex.getAllFilesByExt(project, "c16", scope).isNotEmpty()

    if (hasSpr && !(hasS16 || hasC16)) {
        return CaosVariant.C1
    }
    if (hasC16 && !(hasSpr || hasS16)) {
        return CaosVariant.C3 // Do not assume CV
    }
    val agents = FilenameIndex.getAllFilesByExt(project, "agent", scope).isNotEmpty()
            || FilenameIndex.getAllFilesByExt(project, "agents", scope).isNotEmpty()
    if (agents) {
        return CaosVariant.C3
    }
    val cobs = FilenameIndex.getAllFilesByExt(project, "cob")
    if (!cobs.isEmpty()) {
        if (hasS16) {
            return CaosVariant.C2
        }
        val cobHeader = byteArrayOf(
            'C'.code.toByte(),
            'O'.code.toByte(),
            'B'.code.toByte(),
            '2'.code.toByte()
        )
        val cob2 = cobs.any { it.inputStream?.readNBytes(4).contentEquals(cobHeader) }
        if (cob2) {
            return CaosVariant.C2
        }
    }
    val caosFiles = FilenameIndex.getAllFilesByExt(project, "cos", scope)
    val c3Items = listOf(
        "mv\\d{2}",
        "absv",
        "hist",
        "pray",
        "pat:",
        "dull",
        "monk",
        "gtos",
        "game",
        "name",
        "'",
        "text",
        "CAOS2Pray",
        "mame",
        "eame",
        "avar",
        "avel",
        ">|<|="
    ).joinToString("|")
    val c1 = "clas|var\\d|obv\\d|edit|bbd:|dde:|bt|bf|objp".toRegex(RegexOption.IGNORE_CASE)
    val c2 = "cls2|va\\d{2}|ov\\d+|esee|etch|var\\d|obv\\d|edit|bbd:|dde:|bt|bf|objp".toRegex(RegexOption.IGNORE_CASE)
    val c3Regex = "($c3Items)".toRegex(RegexOption.IGNORE_CASE)
    val string = "(\\[[^\\\\]*]|\"([^\"]|\\\\.)*\")".toRegex()
    for (i in 0 until minOf(5, caosFiles.size)) {
        val file = try {
            caosFiles.random()?.contents
                ?: continue
        } catch (e: Exception) {
            return null
        }
        val withoutQuotes = file.replace(string, " ")
        if (!hasSpr && c3Regex.matches(withoutQuotes)) {
            return CaosVariant.C3
        }
        if (withoutQuotes.count("scrp ") > 1 && !withoutQuotes.contains("*#")) {
            return CaosVariant.C3
        }
        if (!hasSpr && !hasC16 && c2.matches(withoutQuotes)) {
            return CaosVariant.C2
        }
        if (!hasS16 && !hasC16 && c1.matches(withoutQuotes)) {
            return CaosVariant.C1
        }
    }
    return null
}

fun CaosProjectSettingsService.addIgnoredFile(fileName: String) {
    if (fileName.isBlank())
        return
    val state = state
    if (state.ignoredFilenames.contains(fileName))
        return
    loadState(
        state.copy(
            ignoredFilenames = (state.ignoredFilenames + fileName).distinct()
        )
    )
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


interface CaosProjectSettingsChangeListener : EventListener {
    fun onChange(settings: CaosProjectSettingsComponent.State)
}

var CaosProjectSettingsService.injectionCheckDisabled: Boolean
    get() = state.injectionCheckDisabled
    set(value) {
        if (state.injectionCheckDisabled == value)
            return
        loadState(
            state.copy().apply {
                disableInjectionCheck(value)
            }
        )
    }


var CaosProjectSettingsService.useJectByDefault: Boolean
    get() = state.useJectByDefault.apply {
        if (this) {
            LOGGER.severe("JECT should not be enabled as it is not implemented")
            useJectByDefault = false
            // TODO("Set up JECT file settings")
        }
    }
    set(value) {
        if (value == state.useJectByDefault)
            return
        loadState(
            state.copy(
                useJectByDefault = value
            )
        )
    }

var CaosProjectSettingsService.combineAttNodes: Boolean
    get() = state.combineAttNodes
    set(value) {
        if (value == state.combineAttNodes)
            return
        loadState(
            state.copy(
                combineAttNodes = value
            )
        )
    }


fun CaosProjectSettingsService.lastInterface(variant: CaosVariant, interfaceName: GameInterfaceName) {
    val state = state
    val prefix = variant.lastInterfacePrefix
    CaosApplicationSettings.lastInterface(variant, interfaceName)
    loadState(
        state.copy(
            lastGameInterfaceNames = state.lastGameInterfaceNames.filterNot {
                it.startsWith(prefix)
            } + interfaceName.storageKey
        )
    )
}


fun CaosProjectSettingsService.lastInterface(variant: CaosVariant?): GameInterfaceName? {
    if (variant == null)
        return null
    val prefix = variant.lastInterfacePrefix
    val last = state.lastGameInterfaceNames
    return last
        .mapNotNull map@{ entry ->
            if (!entry.startsWith(prefix))
                return@map null
            val key = entry.substring(prefix.length)
            state.gameInterfaceNames.forKey(variant, key)
        }
        .firstOrNull()
        ?: CaosApplicationSettings.lastInterface(variant)
}