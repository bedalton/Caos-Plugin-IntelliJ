@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.forKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getFilesWithExtension
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes.directoryScope
import java.util.*


val Project.settings: CaosProjectSettingsService get() = CaosProjectSettingsService.getInstance(this)


val CaosProjectSettingsService.ignoredFiles: List<String> get() = state.ignoredFilenames


/**
 * Sets the project wide default variant
 */
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


// Sets the module variant in a persistent way
fun Module.inferVariantHard(): CaosVariant? {
    // Get variant normally if possible
    variant?.nullIfUnknown()?.let {
        return it
    }
    moduleFile?.cachedVariantExplicitOrImplicit?.let {
        return it
    }
    val getFilesWithExtension = { extension: String ->
        getFilesWithExtension(project, this, this.myModuleFile, extension)
    }
    val scope = getModuleScope(false)
    return variantInScope(scope, getFilesWithExtension)
}

/**
 * Infers or returns this projects variant
 */
fun Project.inferVariantHard(): CaosVariant? {
    settings.defaultVariant?.nullIfUnknown()?.let {
        return it
    }
    this.projectFile?.cachedVariantExplicitOrImplicit?.let {
        return it
    }
    val getFilesWithExtension = { extension: String ->
        getFilesWithExtension(this, null, this.projectFile, extension)
    }
    val scope = GlobalSearchScope.everythingScope(this)
    val variant = variantInScope(scope, getFilesWithExtension)
    if (variant != null) {
        this.settings.defaultVariant = variant
    }
    this.projectFile?.setCachedVariant(variant, false)
    return variant
}

fun VirtualFile.inferVariantHard(project: Project, searchParent: Boolean = true): CaosVariant? {
    cachedVariantExplicitOrImplicit?.nullIfUnknown()?.let {
        return it
    }
    val getFilesWithExtension = { extension: String ->
        getFilesWithExtension(project, null, this, extension)
    }
    val isDirectory = isDirectory
    val directory = if (isDirectory) {
        this
    } else {
        parent
    }
    val simple = if (!isDirectory) {
        inferVariantSimple()
    } else {
        null
    }
    if (!isDirectory && ScratchUtil.isScratch(this)) {
        setCachedVariant(simple, false)
        return simple
    }
    val variant: CaosVariant? = simple
        ?: if (!isDirectory) {
            parent?.inferVariantHard(project)
        } else {
            // Get without searching subdirectories
            variantInScope(directoryScope(project, this, false), getFilesWithExtension)
            // Get while searching subdirectories as fallback
                ?: variantInScope(directoryScope(project, this, true), getFilesWithExtension)
        }
    if (variant == null) {
        // If we are allowed to search parent, do so
        // This should only look one folder up
        return if (searchParent) {
            val parent = parent
                ?: return null
            parent.inferVariantHard(project, false)
        } else {
            null
        }
    }
    setCachedVariant(variant, false)

    // DO not set directory for a single file in what could be a mixed up set-of files
    if (this.isDirectory || !ScratchUtil.isScratch(this)) {
        directory?.setCachedVariant(variant, false)
    }
    return variant
}

private fun VirtualFile.inferVariantSimple(): CaosVariant? {
    val text = try {
        contents.replace(COMMENTS, "")
    } catch (e: Exception) {
        LOGGER.severe("Failed to replace strings and comments in ${path}")
        return null
    }
    val matchC1 = C1_ITEMS.containsMatchIn(text)
    val matchC2 = C2_ITEMS.containsMatchIn(text)
    val matchDS = DS_ITEMS.containsMatchIn(text)
    return if (matchDS) {
        CaosVariant.DS
    } else if (matchC1 && !matchC2) {
        CaosVariant.C1
    } else if (matchC2 && !matchC1) {
        CaosVariant.CV
    } else if (matchC1) {
        CaosVariant.C1
    } else {
        null
    }
}

private val COMMENTS = "^[ \t]*[*][^\n]*".toRegex()
private const val C1_STRING = "\\[\\s*([^]0-9 ]|[0-9 ]+[^]0-9 ])[^]]*\\s*]"

private val C1_ITEMS by lazy {
    "(clas|var\\d|obv\\d|edit|bbd:|dde:|bt|bf|objp|setv\\s+attr|setv\\s+driv|(simp|comp)\\s+[a-zA-Z0-9]{4}|$C1_STRING)".toRegex(RegexOption.IGNORE_CASE)
}
private val C2_ITEMS by lazy {
    "(cls2|va\\d{2}|ov\\d+|esee|etch|var\\d|obv\\d|edit|bbd:|bbd2|dde:|bt|bf|objp|setv\\s+attr|setv\\s+driv|grav|escn|radn|obsv|size|rest|(simp|comp)\\s+[a-zA-Z0-9]{4})|$C1_STRING".toRegex(RegexOption.IGNORE_CASE)
}
private val C3_ITEMS_ARRAY by lazy {
    listOf(
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
        "\"(([^\"]|\\.))\"",
        "'",
        "^\\s*attr",
        "^\\s*driv",
        "text",
        "CAOS2Pray",
        "mame",
        "eame",
        "avar",
        "avel",
        ">|<|=",
        "ject",
        "pat:",
        "dull",
        "mvft",
        "mvsf",
        "tmvb",
        "tmvf",
        "tmvt",
        "name",
        "abba",
        "read",
        "reaf",
        "caos",
        "pray",
        "fvel",
        "flto",
        "face",
        "eame",
        "econ",
        "clik",
        "clac",
        "sets",
        "seta",
        "\\[[0-9 ]+\\]",
        "(simp|comp)\\s+([0-9]+|(va|mv|ov)[0-9])"
    )
}

private val C3_ITEMS by lazy {
    C3_ITEMS_ARRAY.joinToString("|")
        .let {
            "($it)".toRegex(RegexOption.IGNORE_CASE)
        }
}

private val DS_ITEMS by lazy {
    (C3_ITEMS_ARRAY + arrayOf("net:", "soul")).joinToString("|")
        .let {
            "($it)".toRegex(RegexOption.IGNORE_CASE)
        }
}


/**
 * Checks a project for its file types in a given scope,
 * And tries to determine what variant it could be
 */
private fun variantInScope(scope: GlobalSearchScope, filesByExtension: (extension: String) -> List<VirtualFile>): CaosVariant? {

    val getFilesByExtension = { extension: String ->
        filesByExtension(extension).filter {
            scope.accept(it)
        }
    }

    val hasSpr = getFilesByExtension("spr").isNotEmpty()
    val hasS16 = getFilesByExtension("s16").isNotEmpty()
    val hasC16 = getFilesByExtension("c16").isNotEmpty()

    if (hasSpr && !(hasS16 || hasC16)) {
        return CaosVariant.C1
    }
    if (hasC16 && !(hasSpr || hasS16)) {
        return CaosVariant.DS // Do not assume CV
    }
    val agents = getFilesByExtension("agent").isNotEmpty()
            || getFilesByExtension("agents").isNotEmpty()
    if (agents) {
        return CaosVariant.DS
    }
    val cobs = getFilesByExtension("cob")
    if (cobs.isNotEmpty()) {
        if (hasS16) {
            return CaosVariant.C2
        }
        val cobHeader = byteArrayOf(
            'C'.code.toByte(),
            'O'.code.toByte(),
            'B'.code.toByte(),
            '2'.code.toByte()
        )
        val cob2 = cobs.any { it.inputStream?.readNBytes(4)?.contentEquals(cobHeader) == true }
        if (cob2) {
            return CaosVariant.C2
        }
    }

    val caosFiles = getFilesByExtension("cos")

    for (i in 0 until minOf(5, caosFiles.size)) {
        val file = try {
            caosFiles.randomOrNull()?.contents
                ?: continue
        } catch (e: Exception) {
            return null
        }
        val withoutQuotes = file.replace(COMMENTS, " ")
        if (!hasSpr && C3_ITEMS.matches(withoutQuotes)) {
            return CaosVariant.DS
        }
        if (withoutQuotes.count("scrp ") > 1 && !withoutQuotes.contains("*#")) {
            return CaosVariant.DS
        }
        if (!hasSpr && !hasC16 && C2_ITEMS.matches(withoutQuotes)) {
            return CaosVariant.C2
        }
        if (!hasS16 && !hasC16 && C1_ITEMS.matches(withoutQuotes)) {
            return CaosVariant.C1
        }
    }
    return null
}


/**
 * Add a file to the ignored list that will no longer be checked for validity
 */
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

/**
 * Remove a file from the ignored list, so it will be checked for existence in project
 */
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

/**
 * Adds a game interface name used to communicate with a running instance of Creatures on the OS
 */
fun CaosApplicationSettingsService.addGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = state
    loadState(
        state.copy(
            gameInterfaceNames = (state.gameInterfaceNames + interfaceName)
                .distinct()
                .filter { it !is NativeInjectorInterface || !it.isDefault }
        )
    )
}

/**
 * Removes a game interface name from the project
 * GAME interface names are used to communicate with running Creatures instances
 */
fun CaosApplicationSettingsService.removeGameInterfaceName(interfaceName: GameInterfaceName) {
    val state = state
    loadState(state.copy(
        gameInterfaceNames = state.gameInterfaceNames
            .filter { it != interfaceName }
    ))
}

/**
 * List all game interface names, formatted, with asterisks(*) expanded
 */
val CaosApplicationSettingsService.gameInterfaceNames: List<GameInterfaceName>
    get() {
        return state.gameInterfaceNames
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
fun CaosApplicationSettingsService.gameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
    val interfaces = gameInterfaceNames
    if (variant.nullIfUnknown() == null)
        return interfaces
    return interfaces.filter { it.isVariant(variant) }

}

/**
 * A key for storing the last interface name used for a variant in the project
 */
private val CaosVariant.lastInterfacePrefix get() = "$code=="

fun CaosProjectSettingsService.lastInterface(variant: CaosVariant, interfaceName: GameInterfaceName) {
    val state = state
    val prefix = variant.lastInterfacePrefix
    loadState(
        state.copy(
            lastGameInterfaceNames = state.lastGameInterfaceNames.filterNot {
                it.startsWith(prefix)
            } + (prefix + interfaceName.serialize())
        )
    )
}

fun CaosApplicationSettingsService.gameInterfaceForKey(key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(null, key)
}

fun CaosApplicationSettingsService.gameInterfaceForKey(variant: CaosVariant?, key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(variant, key)
}

fun CaosProjectSettingsService.lastInterface(variant: CaosVariant?): GameInterfaceName? {
    if (variant == null)
        return null
    val prefix = variant.lastInterfacePrefix
    return state.lastGameInterfaceNames
        .mapNotNull map@{ entry ->
            if (!entry.startsWith(prefix))
                return@map null
            val key = entry.substring(prefix.length)
            CaosApplicationSettingsService.getInstance().gameInterfaceNames.forKey(variant, key)
        }
        .firstOrNull()
}

interface CaosProjectSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosProjectSettingsComponent.State, newState: CaosProjectSettingsComponent.State)
}

/**
 * Gets/Sets whether to check a file for valid CAOS before injecting
 * This can be used when the plugin does not account for a command, or one is incorrectly
 * described
 */
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

/**
 * Flatten ATTs for duplicate images
 * Used for things like the front facing ATTs with C1e -> C2e conversions
 */
var CaosApplicationSettingsService.replicateAttToDuplicateSprites: Boolean?
    get() = state.replicateAttToDuplicateSprite
    set(value) {
        if (value == state.replicateAttToDuplicateSprite)
            return
        loadState(
            state.copy(
                replicateAttToDuplicateSprite = value != false
            )
        )
    }
