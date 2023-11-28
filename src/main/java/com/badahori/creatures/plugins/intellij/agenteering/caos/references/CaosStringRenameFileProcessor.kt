package com.badahori.creatures.plugins.intellij.agenteering.caos.references
//
//import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
//import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
//import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil
//import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
//import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
//import com.bedalton.common.util.*
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VfsUtil
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.psi.PsiDirectory
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiFile
//import com.intellij.refactoring.rename.RenamePsiFileProcessor
//import java.nio.file.Path
//
//
//class CaosStringRenameFileProcessor : RenamePsiFileProcessor() {
//    override fun canProcessElement(element: PsiElement): Boolean {
//        return element is PsiFile || element is CaosScriptStringLike || element is PsiDirectory
//    }
//
//    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
////        val oldStringValue = if (element is CaosScriptStringLike) {
////            element.stringValue
////        } else {
////            element.text
////        }
////        when (element) {
////            is CaosScriptStringLike -> {
////                prepareRenameFromString(element, oldStringValue, newName, allRenames)
////            }
////
////            is PsiDirectory -> {
////                prepareRenameFromDirectory(element, oldStringValue, newName, allRenames)
////            }
////
////            is PsiFile -> {
////                prepareRenameFromFile(element, oldStringValue, newName, allRenames)
////            }
////        }
//    }
//
////    private fun prepareRenameFromFile(
////        element: PsiElement,
////        oldStringValue: String,
////        newName: String,
////        allRenames: MutableMap<PsiElement, String>
////    ) {
////
////    }
////
////    private fun prepareRenameFromDirectory(
////        element: PsiDirectory,
////        newName: String,
////        allRenames: MutableMap<PsiElement, String>
////    ) {
////        val virtualFile = element.virtualFile
////            ?: return
////        val oldFilePath = virtualFile.path
////        val prefix = virtualFile.parent.path
////        val newAbsolutePath = prefix.ensureEndsWith('/') + newName
////        addStringRenames(
////            project = element.project,
////            oldFilePath = oldFilePath,
////            newAbsolutePath = newAbsolutePath,
////            allRenames = allRenames,
////            isDirectory = true
////        )
////    }
////
////    private fun prepareRenameFromString(
////        element: CaosScriptStringLike,
////        newName: String,
////        allRenames: MutableMap<PsiElement, String>
////    ) {
////        val oldPath = virtualFileFromStringLike(element)
////            ?.path
////            ?.normalize()
////            ?: return
////
////        val newPath = if (newName.contains('/')) {
////            getNewPath(element, newName) ?: newName
////        } else {
////            newName
////        }
////        addStringRenames(element.project, oldPath, newPath, allRenames, false)
////    }
////
////    private fun addStringRenames(
////        project: Project,
////        oldFilePath: String,
////        newAbsolutePath: String,
////        allRenames: MutableMap<PsiElement, String>,
////        isDirectory: Boolean,
////    ) {
////
////    }
////
////    private fun getStringRename(
////        project: Project,
////        oldFilePath: String,
////        newAbsolutePath: String,
////        stringData: Pair<CaosScriptStringLike, String>,
////        isDirectory: Boolean,
////    ): String? {
////        val (otherElement, path) = stringData
////
////        val parentFile = otherElement
////            .virtualFile
////            ?.parent
////
////        val stringValue = otherElement
////            .stringValue
////            .normalize()
////
////        val basename = PathUtil.getLastPathComponent(stringValue) ?: stringValue
////
////        val withExtension = (PathUtil.getFileNameWithoutExtension(basename) != basename)
////
////        val relative = otherElement.hasParentOfType(PrayInputFileName::class.java)
////                || otherElement.hasParentOfType(PrayTagValue::class.java)
////
////        val thisPath = when {
////            isDirectory -> {
////                if (path.startsWith(oldFilePath)) {
////                    newAbsolutePath.ensureEndsWith('/') + path.substring(oldFilePath.length)
////                } else {
////                    return null
////                }
////            }
////
////            else -> {
////                val parent = otherElement.parent
////
////                PathUtil.relativePath(oldVirtualFile.path, newAbsolutePath)
////            }
////        }
////    }
////
////    private fun stringsWithPaths(
////        project: Project,
////        oldStringValue: String,
////        isDirectory: Boolean
////    ): List<Pair<CaosScriptStringLike, @NonNls String>> {
////        val projectScope = GlobalSearchScope.projectScope(project)
////        val basename = PathUtil.getLastPathComponent(oldStringValue)?.lowercase() ?: oldStringValue.lowercase()
////        var filenameWithoutExtension = PathUtil.getFileNameWithoutExtension(oldStringValue)?.lowercase()
////        if (filenameWithoutExtension == basename) {
////            filenameWithoutExtension = null
////        }
////        val filter: (string: CaosScriptStringLike) -> Boolean = when {
////            isDirectory -> {
////                val directoryNormalized = oldStringValue.normalize().lowercase();
////                { element ->
////                    virtualFileFromStringLike(element)
////                        ?.path
////                        ?.normalize()
////                        ?.startsWith(directoryNormalized) == true
////                }
////            }
////
////            (filenameWithoutExtension != null) -> {
////                {
////                    val lower = it.stringValue.lowercase()
////                    lower.endsWith(filenameWithoutExtension) || lower.endsWith(basename)
////                }
////            }
////
////            else -> {
////                {
////                    it.stringValue.lowercase().endsWith(basename)
////                }
////            }
////        }
////        return CaosScriptStringLiteralIndex
////            .instance
////            .getAllInScope(project, projectScope)
////            .filter(filter)
////            .mapNotNull map@{ element ->
////                val path = virtualFileFromStringLike(element)
////                    ?.path
////                    ?: return@map null
////                Pair(element, path)
////            }
////    }
////
////    private fun virtualFileFromStringLike(element: CaosScriptStringLike): VirtualFile? {
////        val stringValue = element.stringValue
////            .ensureNotStartsWith("./")
////            .nullIfEmpty()
////            ?: return null
////        return if (!PathUtil.isAbsolute(stringValue)) {
////            val virtualFile = element.virtualFile
////            if (virtualFile != null) {
////                val temp = PathUtil.combine(virtualFile.path, stringValue)
////                val tempPath = Path.of(temp)
////                VfsUtil.findFile(tempPath, false)
////            } else {
////                null
////            }
////        } else {
////            val normalized = PathUtil.combine(stringValue)
////            val tempPath = Path.of(normalized)
////            VfsUtil.findFile(tempPath, false)
////        } ?: virtualFileFromFromUnknownRoot(element.project, stringValue)
////    }
////
////
////    private fun getNewPath(element: CaosScriptStringLike, newName: String): String? {
////        if (PathUtil.isAbsolute(newName)) {
////            return newName.normalize()
////        }
////        val virtualFile = element.virtualFile
////        return if (virtualFile != null) {
////            return PathUtil.combine(virtualFile.path, newName.normalize())
////        } else {
////            newName.normalize()
////        }
////    }
//
//    companion object {
//
//        @JvmStatic
//        internal fun createPathFromElement(
//            baseElement: PsiElement,
//            childPath: String
//        ): String? {
//            if (PathUtil.isAbsolute(childPath)) {
//                return childPath
//            }
//            val directory = when (baseElement) {
//                is PsiDirectory -> baseElement.virtualFile
//                is PsiFile -> baseElement.virtualFile.parent
//                else -> baseElement.virtualFile?.let { virtualFile ->
//                    if (virtualFile.isDirectory) {
//                        virtualFile
//                    } else {
//                        virtualFile.parent
//                    }
//                }
//            } ?: return null
//            val directoryPath = directory.path
//            val normalizedChildPath = childPath.normalize()
//            return PathUtil.combine(directoryPath, normalizedChildPath).normalize()
//        }
//
//        private fun getFilePathFromElementText(element: PsiElement, text: String? = null): VirtualFile? {
//
//            val elementText = text ?: if (element is CaosScriptStringLike) {
//                element.stringValue
//            } else {
//                element.text
//                    .stripSurroundingQuotes(false)
//                    .unescape()
//            }
//
//            if (element is PsiFile) {
//                val file = element.virtualFile
//                return if (file != null && file.exists()) {
//                    file
//                } else {
//                    null
//                }
//            }
//
//            var virtualFile = if (PathUtil.isAbsolute(elementText)) {
//                val tempPath = Path.of(elementText)
//                VfsUtil.findFile(tempPath, false)
//            } else {
//                val parent = element.virtualFile?.parent
//                if (parent != null) {
//                    val temp = PathUtil.combine(parent.path, elementText)
//                    val tempPath = Path.of(temp)
//                    VfsUtil.findFile(tempPath, false)
//                } else {
//                    val tempPath = Path.of(elementText)
//                    VfsUtil.findFile(tempPath, false)
//                }
//            }
//
//            if (virtualFile == null || !virtualFile.exists()) {
//                virtualFile = virtualFileFromFromUnknownRoot(element.project, elementText)
//            }
//
//            return if (virtualFile != null && virtualFile.exists()) {
//                virtualFile
//            } else {
//                null
//            }
//        }
//
//        private fun virtualFileFromFromUnknownRoot(project: Project, path: String): VirtualFile? {
//            val isMatch = isMatch(path)
//            val basename = PathUtil.getLastPathComponent(path) ?: path
//            val matches = if (PathUtil.getFileNameWithoutExtension(basename) == basename) {
//                CaseInsensitiveFileIndex.findWithFileName(project, basename)
//            } else {
//                CaseInsensitiveFileIndex.findWithoutExtension(project, basename)
//            }
//                .nullIfEmpty()
//                ?.filter(isMatch)
//                ?: return null
//            return matches.singleOrNull()
//        }
//
//
//        private fun isMatch(suffix: String): (file: VirtualFile) -> Boolean {
//            val separator = if (suffix.contains(pathSeparatorChar)) {
//                pathSeparatorChar
//            } else if (OsUtil.isWindows && isWindows(suffix.replace("\\\"", "").replace("\\\'", ""))) {
//                '\\'
//            } else {
//                '/'
//            }
//            val components = suffix
//                .normalize()
//                .split(separator)
//                .reversed()
//                .takeWhile { it != ".." }
//                .reversed()
//            val normalizedSuffix = components.joinToString("/")
//                .lowercase()
//            val regexSuffix = (".*?" + Regex.escape(normalizedSuffix) + "\\..+$").toRegex()
//            return { file: VirtualFile ->
//                val normalizedPath = file
//                    .path
//                    .replace("\\'", "'")
//                    .replace("\\\"", "\"")
//                    .replace('\\', '/')
//                    .lowercase()
//                if (normalizedPath.endsWith(normalizedSuffix)) {
//                    true
//                } else {
//                    regexSuffix.matches(normalizedPath)
//                }
//            }
//        }
//
//        private fun String.normalize(): String {
//            return replace("\\'", "'")
//                .replace("\\\"", "\"")
//                .replace("\\ ", " ")
//                .replace('\\', '/')
//        }
//
//
//    }
//}