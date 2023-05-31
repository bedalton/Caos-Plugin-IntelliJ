package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.joinToString
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.endsWithName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope

internal object FilenameStringCollector {
    private const val asteriskPlaceholder = "___STAR___"
    private const val questionMarkPlaceholder = "___QUESTION___"
    private const val defaultCheckCancelled = false

    fun collect(project: Project, file: VirtualFile, checkCancelled: Boolean = defaultCheckCancelled): CollectedFilenameStrings? {
        return collect(project, file.path, checkCancelled)
    }

    fun collect(project: Project, path: String, checkCancelled: Boolean = defaultCheckCancelled): CollectedFilenameStrings? {

        // Get file name components
        val extensionLowercase = PathUtil.getExtension(path)
            ?.nullIfEmpty()
            ?.lowercase()
            ?: return null

        val basenameLowercase = PathUtil.getLastPathComponent(path)
            ?.nullIfEmpty()
            ?.lowercase()
            ?: return null

        val filenameWithoutExtensionLowercase = PathUtil.getFileNameWithoutExtension(path)
            ?.nullIfEmpty()
            ?.lowercase()
            ?: return null

        // Get target file stub type
        val fileStubKind = StringStubKind.fromExtension(extensionLowercase)
            ?: return null

        // Ensure that string stub kind is a kind of file
        if (!fileStubKind.isFile) {
            return null
        }

        // Make pattern matching regex
        val fileNameEscaped = formatBasenameForRegex(basenameLowercase)
        val pathRegex = ".*?$fileNameEscaped".toRegex(RegexOption.IGNORE_CASE)

        // Use Project scope
        val scope = GlobalSearchScope.projectScope(project)

        // Find strings based on filename regex
        val patternMatches = CaosScriptStringLiteralIndex.instance
            .getByPattern(pathRegex, project, scope)

        // Rename groups
        val matchesWithoutExtensionRenames = mutableListOf<CaosScriptStringLike>()
        val matchesWithBasenameRenames = mutableListOf<CaosScriptStringLike>()
        val matchesWithPathRenames = mutableListOf<CaosScriptStringLike>()

        // Handle renames in matches
        for ((keyText, matches) in patternMatches) {
            // If desired, allow search to be cancelled by IDE
            if (checkCancelled) {
                ProgressIndicatorProvider.checkCanceled()
            }

            val keyTextLowercase = keyText.lowercase()
            if (keyTextLowercase == filenameWithoutExtensionLowercase) {
                // File name is direct mapping
                matchesWithoutExtensionRenames.addAll(getPointingToFileOfKind(fileStubKind, matches))
            } else if (keyTextLowercase.endsWith(filenameWithoutExtensionLowercase)) {
                continue
            } else if (keyTextLowercase == basenameLowercase) {
                // File is simply the full name without relative path
                matchesWithBasenameRenames.addAll(getPointingToFileOfKind(fileStubKind, matches))
            } else if (keyTextLowercase.endsWith(basenameLowercase)) {
                // File path is relative
                matchesWithPathRenames.addAll(getRelativePathRenames(path, matches, checkCancelled))
            }
        }
        val hasRenames = matchesWithBasenameRenames.isNotEmpty() ||
                matchesWithoutExtensionRenames.isNotEmpty() ||
                matchesWithPathRenames.isNotEmpty()

        if (!hasRenames) {
            return null
        }
        return CollectedFilenameStrings(
            withFullName = matchesWithBasenameRenames.distinctBy { it.textRange },
            withFilenameWithoutExtension = matchesWithoutExtensionRenames.distinctBy { it.textRange },
            withRelativePath = matchesWithPathRenames.distinctBy { it.textRange }
        )
    }

    private fun getPointingToFileOfKind(
        fileStubKind: StringStubKind,
        matches: List<CaosScriptStringLike>,
    ): List<CaosScriptStringLike> {
        return matches
            .filter { match ->
                val elementStringStubKind = match.stringStubKind
                elementStringStubKind like fileStubKind
            }
    }

    private fun getRelativePathRenames(
        path: String,
        matches: List<CaosScriptStringLike>,
        checkCancelled: Boolean,
    ): List<CaosScriptStringLike> {

        val out = mutableListOf<CaosScriptStringLike>()

        val oldPathLower = path.lowercase()

        // Look for paths matching the old path
        for (match in matches) {
            // If desired, allow search to be cancelled by IDE
            if (checkCancelled) {
                ProgressIndicatorProvider.checkCanceled()
            }
            if (match !is PsiNamedElement) {
                continue
            }
            val textPathLower = match.stringValue.lowercase()

            val stringFilenameLower = if (textPathLower == oldPathLower) {
                oldPathLower
            } else {
                // Get this strings parent to resolve relative path
                val parent = match.virtualFile?.parent
                    ?: continue
                val parentPath = parent.path
                PathUtil.combine(parentPath, textPathLower).lowercase()
            }

            if (oldPathLower == stringFilenameLower) {
                out.add(match)
            }
        }
        return out
    }

    private fun formatBasenameForRegex(basename: String): String {
        var formatted = basename.lowercase()

        val hasWildCards = formatted.contains('?') || formatted.contains('*')
        formatted = if (hasWildCards) {
            formatted
                .replace("*", asteriskPlaceholder)
                .replace("?", questionMarkPlaceholder)
        } else {
            PathUtil.getFileNameWithoutExtension(formatted) ?: formatted
        }

        formatted = Regex.escape(formatted)
        if (hasWildCards) {
            formatted = formatted
                .replace(asteriskPlaceholder, ".*?")
                .replace(questionMarkPlaceholder, ".")
        }
        if (formatted.endsWith('.')) {
            formatted += '+'
        } else if (!formatted.endsWith('?')) {
            formatted += "(\\..+)?"
        }
        return "$formatted\$"
    }


    data class CollectedFilenameStrings(
        val withFullName: List<CaosScriptStringLike>,
        val withFilenameWithoutExtension: List<CaosScriptStringLike>,
        val withRelativePath: List<CaosScriptStringLike>
    ) {
        fun flattened(): List<CaosScriptStringLike> {
            return withFullName + withFilenameWithoutExtension + withRelativePath
        }
    }
}