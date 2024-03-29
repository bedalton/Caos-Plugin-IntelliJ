package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ignoredFiles
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.PathUtil.getExtension
import com.bedalton.common.util.toListOf
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import java.io.File
import java.nio.file.FileSystems

private const val DEFAULT_ORB = 5

/**
 * Gets filenames with a certain level of similarity to the one in question
 * @param element is the element to replace using the new file path
 */
internal fun getFilenameSuggestions(
    element: PsiElement,
    removeExtension: Boolean,
    baseFileName: String,
    extensions: List<String>? = null,
    orb: Int = DEFAULT_ORB,
    includedFiles: List<String>,
    pathless: Boolean = false
): List<CaosScriptReplaceElementFix>? {
    val fileNameExtension = extensions
        ?: getExtension(baseFileName)
            ?.lowercase()
            ?.toListOf()
    val directory = element.directory
        ?: return emptyList()
    return getFilenameSuggestions(
        directory,
        element,
        removeExtension,
        baseFileName,
        extensions = fileNameExtension,
        orb = orb,
        includedFiles,
        pathless
    )
}

private val PATH_SPLIT = "[/\\\\]".toRegex()

/**
 * Gets filenames with a certain level of similarity to the one in question
 * @param element is the element to replace using the new file path
 */
internal fun getFilenameSuggestions(
    directory: VirtualFile,
    element: PsiElement,
    removeExtension: Boolean,
    filePath: String,
    extensions: List<String>? = null,
    orb: Int = DEFAULT_ORB,
    includedFiles: List<String>,
    pathless: Boolean = false,
): List<CaosScriptReplaceElementFix>? {

    val fileName = filePath.split(PATH_SPLIT).last()
//    val subdirectory = filePath.split(pathSeparator)
//        .dropLast(1)
//        .nullIfEmpty()
//        ?.joinToString(pathSeparator)
//        ?.let { "$it$pathSeparator" }

    val fileNameWithoutExtension = PathUtil.getFileNameWithoutExtension(fileName)
        ?: return null

    val ignoredFilesRaw = element.containingFile?.module?.settings?.ignoredFiles.orEmpty() +
            element.project.settings.ignoredFiles +
            includedFiles


    val ignoredFiles = if (pathless) {
        (ignoredFilesRaw + includedFiles.mapNotNull { PathUtil.getLastPathComponent(it) }).distinct()
    } else {
        ignoredFilesRaw
    }

    if (fileName in ignoredFiles) {
        return null
    }

    if (removeExtension && ignoredFiles.any { PathUtil.getFileNameWithoutExtension(it) == fileNameWithoutExtension }) {
        return null
    }

    // Get target file matching baseFileName case-insensitive
    val ignoredFilename: String? =
        if (!fileName.contains('/') && (removeExtension || fileNameWithoutExtension == fileName)) {
            if (extensions != null) {
                // Filename should not have an extension, so attach all possible and check
                extensions.firstOrNull { extension ->
                    "$fileNameWithoutExtension.$extension" in ignoredFiles
                } ?: extensions.firstOrNull { extension ->
                    "$fileNameWithoutExtension.$extension" likeAny ignoredFiles
                }
            } else {
                ignoredFiles.firstOrNull {
                    PathUtil.getFileNameWithoutExtension(it) == fileNameWithoutExtension
                } ?:  ignoredFiles.firstOrNull {
                    PathUtil.getFileNameWithoutExtension(it).equals(fileNameWithoutExtension, true)
                }
            }
        } else if (!fileName.contains('/')) {
            ignoredFiles.firstOrNull { it.equals(fileName, true) }
        } else {
            null
        }

    if (ignoredFilename == fileName) {
        return null
    }

    if (ignoredFilename != null) {
        // Is case-insensitive file system
        if (OsUtil.isWindows) {
            return null
        }

        if (removeExtension && PathUtil.getFileNameWithoutExtension(ignoredFilename) == fileNameWithoutExtension) {
            return null
        }
        // Create single fix array
        return CaosScriptReplaceElementFix(
            element,
            "\"$ignoredFilename\"",
            "Fix filename case"
        ).toListOf()
    }

    val trueDirectory = PathUtil.combine(directory.path, *filePath.split("[/\\\\]".toRegex()).toTypedArray()).let { subPath ->
        VfsUtil.findFile(FileSystems.getDefault().getPath(subPath), false)?.parent
    } ?: directory

    // Get target file matching baseFileName case-insensitive
    val targetFile: VirtualFile? = if (extensions.isNullOrEmpty()) {
        VirtualFileUtil.findChildIgnoreCase(trueDirectory, removeExtension, directory = false, fileName)
    } else if (removeExtension && fileNameWithoutExtension.equals(fileName, ignoreCase = true)) {
        // Filename should not have an extension, so attach all possible and check
        extensions.firstNotNullOfOrNull { extension ->
            VirtualFileUtil.findChildIgnoreCase(trueDirectory,
                ignoreExtension = false,
                directory = false,
                "$fileName.$extension")
        }
    } else {
        VirtualFileUtil.findChildIgnoreCase(trueDirectory, ignoreExtension = false, directory = false, fileName)
    }


    if (targetFile != null) {
        if (PathUtil.combine(directory.path, filePath) == targetFile.path) {
            return null
        }
        if (removeExtension && fileNameWithoutExtension == targetFile.nameWithoutExtension) {
            return null
        }
        // Windows is case-insensitive, so return
        if (OsUtil.isWindows)
            return null

        // Get relative replacement path
        val replacement = getRelativePath(directory, targetFile, removeExtension)
            ?: return emptyList()

        // Create single fix array
        return CaosScriptReplaceElementFix(
            element,
            "\"$replacement\"",
            "Fix filename case"
        ).toListOf()
    }

    // Create a list of allowed extensions
    val targetExtensions = (extensions?.map { it.lowercase() } ?: listOfNotNull(
        getExtension(fileName)?.lowercase()?.nullIfEmpty()
    )).nullIfEmpty()
    val filenameForDistanceCheck = fileNameWithoutExtension.lowercase()
    // Find siblings and filter by extension, and similarity to the current file name

    val siblings = VirtualFileUtil.findChildrenIfDirectoryOrSiblingsIfLeaf(trueDirectory, fileName)
        ?.filter {
            if (targetExtensions != null && getExtension(it.name)?.lowercase() !in targetExtensions)
                return@filter false
            it.nameWithoutExtension.lowercase().levenshteinDistance(filenameForDistanceCheck) < orb
        }
    val ignoredSiblings = ignoredFiles.filter {
        if (targetExtensions != null && getExtension(it)?.lowercase() !in targetExtensions)
            return@filter false
        PathUtil.getFileNameWithoutExtension(it).orEmpty().lowercase().levenshteinDistance(filenameForDistanceCheck) < orb
    }.map {
        CaosScriptReplaceElementFix(
            element,
            "\"$it\"",
            "Fix filename case"
        )
    }
    return siblings
        ?.mapNotNull { similarFile ->
            makeReplaceFilenameFix(directory, similarFile, removeExtension, element)
        }.orEmpty() + ignoredSiblings
}


/**
 * Gets filenames with a certain level of similarity to the one in question
 * @param element is the element to replace using the new file path
 */
internal fun getSimilarFileNames(
    element: PsiElement,
    removeExtension: Boolean,
    baseFileName: String,
    extensions: List<String>? = null,
    orb: Int = DEFAULT_ORB
): List<String>? {
    val fileNameExtension = extensions
        ?: getExtension(baseFileName)
            ?.lowercase()
            ?.toListOf()
        ?: return emptyList()
    val directory = element.directory
        ?: return emptyList()
    return getSimilarFileNames(
        directory = directory,
        removeExtension = removeExtension,
        baseFileName = baseFileName,
        extensions = fileNameExtension,
        orb = orb
    )
}

/**
 * Gets filenames with a certain level of similarity to the one in question
 */
internal fun getSimilarFileNames(
    directory: VirtualFile,
    removeExtension: Boolean,
    baseFileName: String,
    extensions: List<String>? = null,
    orb: Int = DEFAULT_ORB
): List<String>? {
    // Get name without extension for simple name checks
    val fileNameWithoutExtension = PathUtil.getFileNameWithoutExtension(baseFileName)
        ?: return null

    // Try to find the intended file caseless if needed
    val targetFile: VirtualFile? = if (extensions.isNullOrEmpty()) {
        VirtualFileUtil.findChildIgnoreCase(directory, removeExtension, directory = false, baseFileName)
    } else if (fileNameWithoutExtension.lowercase() == baseFileName) {
        extensions.firstNotNullOfOrNull { extension ->
            VirtualFileUtil.findChildIgnoreCase(directory,
                ignoreExtension = false,
                directory = false,
                "$baseFileName.$extension")
        }
    } else {
        VirtualFileUtil.findChildIgnoreCase(directory, ignoreExtension = false, directory = false, baseFileName)
    }

    // Found target file
    // Return case fix if needed else returns null
    if (targetFile != null) {
        if (baseFileName == targetFile.name) {
            return null
        }
        // Windows is case-insensitive, so return
        if (OsUtil.isWindows)
            return null

        // Get relative replacement path
        val replacement = getRelativePath(directory, targetFile, removeExtension)
            ?: return emptyList()

        // Create single fix array
        return replacement.toListOf()
    }

    // Create a list of allowed extensions
    val targetExtensions = (extensions?.map { it.lowercase() }
        ?: listOfNotNull(getExtension(baseFileName)?.lowercase()))
        .nullIfEmpty()

    val filenameForDistanceCheck = fileNameWithoutExtension.lowercase()
    // Find siblings and filter by extension, and similarity to the current file name
    val siblings = VirtualFileUtil.findChildrenIfDirectoryOrSiblingsIfLeaf(directory, baseFileName)
        ?.filter {
            if (targetExtensions != null && getExtension(it.name)?.lowercase() !in targetExtensions)
                return@filter false
            it.nameWithoutExtension.lowercase().levenshteinDistance(filenameForDistanceCheck) <= orb
        }.nullIfEmpty()
        ?: return emptyList()

    // Return similar files relative to the directory given
    return siblings
        .mapNotNull { similarFile ->
            getRelativePath(directory, similarFile, removeExtension)
        }
}

/**
 * Makes a fix to replace an element with a path to another element and an element to replace
 */
internal fun makeReplaceFilenameFix(
    directory: VirtualFile,
    similarFile: VirtualFile,
    removeExtension: Boolean,
    element: PsiElement
): CaosScriptReplaceElementFix? {
    val replacement = getRelativePath(directory, similarFile, removeExtension)
        ?: return null
    return CaosScriptReplaceElementFix(
        element,
        "\"$replacement\"",
        "Replace with file '${similarFile.name}'"
    )
}

/**
 * Gets and formats the relative path from a given directory
 * TODO, see if I should limit to only child paths
 */
private fun getRelativePath(directory: VirtualFile, similarFile: VirtualFile, removeExtension: Boolean): String? {
    val newPath = VfsUtil.findRelativePath(directory, similarFile, File.separatorChar)
        ?: return null

    val path = newPath.split(File.separatorChar)
        .dropLast(1)
        .joinToString("" + File.separatorChar)
        .nullIfEmpty()
        ?.let { it + File.pathSeparator }
        .orElse("")
    val fileName = if (removeExtension) {
        similarFile.nameWithoutExtension
    } else {
        similarFile.name
    }
    return path + fileName
}