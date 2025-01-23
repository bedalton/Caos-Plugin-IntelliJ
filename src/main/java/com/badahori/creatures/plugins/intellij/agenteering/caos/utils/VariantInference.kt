package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.formatted
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import java.io.InputStream


private const val VARIANT_VALID_FOR = 40_000
private val VARIANT_WITH_EXPIRY = Key<Pair<Long, CaosVariant?>>("bedalton.creatures.VARIANT_WITH_EXPIRY")

// Sets the module variant in a persistent way
fun Module.inferVariantHard(): CaosVariant? {
    // Get variant normally if possible
    variant?.nullIfUnknown()?.let {
        return it
    }
    cachedVariantExplicitOrImplicit?.let {
        return it
    }

    val getFilesWithExtension = getEmptyFilesListLambda // getFilesWithExtensionLambda(project, this, this.myModuleFile)

    val scope = getModuleScope(false)
    return variantInScope(this, scope, getFilesWithExtension)
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

    val getFilesWithExtension = getEmptyFilesListLambda // getFilesWithExtensionLambda(this, null, this.projectFile)

    val scope = GlobalSearchScope.everythingScope(this)
    val variant = variantInScope(this, scope, getFilesWithExtension)
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
        ?: if (searchParent) {
            parent?.inferVariantHard(project, searchParent = false)
        } else {
            val getFilesWithExtension = getEmptyFilesListLambda // getFilesWithExtensionLambda(project, null, this)

            // Get without searching subdirectories
            variantInScope(this, GlobalSearchScopes.directoryScope(project, this, false), getFilesWithExtension)
            // Get while searching subdirectories as fallback
                ?: variantInScope(this, GlobalSearchScopes.directoryScope(project, this, true), getFilesWithExtension)
        }

    setCachedVariant(variant, false)

    // DO not set directory for a single file in what could be a mixed up set-of files
    if (this.isDirectory || !ScratchUtil.isScratch(this)) {
        directory?.setCachedVariant(variant, false)
    }
    return variant
}

private val getEmptyFilesListLambda: ((extension: String) -> List<VirtualFile>) by lazy {
    val emptyFiles = emptyList<VirtualFile>();
    {
        emptyFiles
    }
}

@Suppress("unused")
private fun getFilesWithExtensionLambda(project: Project, module: Module?, file: VirtualFile?): (extension: String) -> List<VirtualFile> {
    return { extension: String ->
        getFilesWithExtension(project, module, module?.myModuleFile, extension)
    }
}

private fun VirtualFile.inferVariantSimple(): CaosVariant? {
    ProgressIndicatorProvider.checkCanceled()
    val text = try {
        contents.replace(COMMENTS, "").replace("\\s".toRegex(), " ").let {
            if (contents.length > 500) {
                val lastIndex = contents.indexOf(' ', 480)
                if (lastIndex < 0) {
                    it
                } else {
                    contents.substring(0, lastIndex)
                }
            } else {
                it
            }
        }
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        LOGGER.severe("Failed to replace strings and comments in $path; ${e.formatted(true)}")
        return null
    }
    val matchC1 = C1_ITEMS.containsMatchIn(text)
    ProgressIndicatorProvider.checkCanceled()
    val matchC2 = C2_ITEMS.containsMatchIn(text)
    ProgressIndicatorProvider.checkCanceled()
    val matchDS = DS_ITEMS.containsMatchIn(text)
    ProgressIndicatorProvider.checkCanceled()
    return if (matchDS && !matchC2 && !matchC1) {
        CaosVariant.DS
    } else if (matchC1 && !matchC2 && !matchDS) {
        CaosVariant.C1
    } else if (matchC2 && !matchC1 && !matchDS) {
        CaosVariant.C2
    } else {
        null
    }
}

internal fun isPossiblyCaos(contents: String): Boolean {
    ProgressIndicatorProvider.checkCanceled()
    val text = contents.replace(COMMENTS, "")
        .replace("\\s".toRegex(), " ").let {
            if (contents.length > 500) {
                val lastIndex = contents.indexOf(' ', 480)
                if (lastIndex < 0) {
                    it
                } else {
                    contents.substring(0, lastIndex)
                }
            } else {
                it
            }
        }.nullIfEmpty()
        ?: return false

    if (RSCR_LIKE.containsMatchIn(text)) {
        return true
    }
    if (DS_ITEMS.containsMatchIn(text)) {
        return true
    }
    if (C1_ITEMS.containsMatchIn(text)) {
        return true
    }
    if (C2_ITEMS.containsMatchIn(text)) {
        return true
    }
    return false
}

@Suppress("unused")
internal fun inferVariantInParent(project: Project, file: VirtualFile?): CaosVariant? {
    return if (file == null) {
        return null
    } else if (file.isDirectory) {
        file.inferVariantHard(project, true)
    } else {
        file.parent.inferVariantHard(project, true)
    }
}

private val COMMENTS = "^[ \t]*[*][^\n]*".toRegex()
private const val C1_STRING = "\\[\\s*([^]0-9 ]|[0-9 ]+[^]0-9 ])[^]]*\\s*]"

private val RSCR_LIKE = "(scrx|kill targ|delg|deln|delm) ".toRegex(RegexOption.IGNORE_CASE)

private val C1_ITEMS by lazy {
    "(clas|var\\d|obv\\d|edit|bbd:|dde:|bt|bf|objp|setv\\s+attr|f\\*\\*k|setv\\s+driv|(simp|comp)\\s+[a-zA-Z0-9]{4}|$C1_STRING)".toRegex(
        RegexOption.IGNORE_CASE
    )
}
private val C2_ITEMS by lazy {
    "(cls2|esee|etch|var\\d|obv\\d|edit|bbd:|bbd2|dde:|bt|bf|objp|setv\\s+attr|setv\\s+driv|grav|escn|radn|obsv|size|rest|(simp|comp)\\s+[a-zA-Z0-9]{4})|$C1_STRING".toRegex(
        RegexOption.IGNORE_CASE
    )
}
private val C3_ITEMS_ARRAY by lazy {
    listOf(
        "mv\\d{2}",
        "absv",
        "hist",
        "pray",
        "pat:",
        "dull",
        "delg",
        "delm",
        "deln",
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

@Suppress("UNUSED_PARAMETER")
private fun variantInScope(
    key: UserDataHolder,
    scope: GlobalSearchScope,
    filesByExtension: (extension: String) -> List<VirtualFile>,
): CaosVariant? {

    key.getUserData(VARIANT_WITH_EXPIRY)?.let { variantWithExpiry ->
        variantWithExpiry.second?.let {
            return it
        }
        if (variantWithExpiry.first < now) {
            return null
        }
    }

    // Quickly set variant to null to prevent multiple calls all fighting to set the value
    key.putUserData(VARIANT_WITH_EXPIRY, Pair(now + 10_000, null))

    // Gets the variant
    val variant = null //variantInScopeFinal(scope, filesByExtension)
    key.putUserData(VARIANT_WITH_EXPIRY, Pair(now + VARIANT_VALID_FOR, variant))
    return variant
}


/**
 * Checks a project for its file types in a given scope,
 * And tries to determine what variant it could be
 */
@Suppress("unused")
private fun variantInScopeFinal(
    scope: GlobalSearchScope,
    filesByExtension: (extension: String) -> List<VirtualFile>,
): CaosVariant? {

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
        val buffer = ByteArray(4)
        val cob2 = cobs.any {
            var inputStream: InputStream? = null
            try {
                inputStream = it.inputStream
                inputStream.read(buffer) == 4 && buffer.contentEquals(cobHeader)
            } catch (e: Exception) {
                inputStream?.close()
                inputStream = null
                e.rethrowAnyCancellationException()
                false
            } finally {
                inputStream?.close()
            }
        }
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
            e.rethrowAnyCancellationException()
            return null
        }
        val withoutQuotes = Regex.escape(file.replace(COMMENTS, " "))
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
