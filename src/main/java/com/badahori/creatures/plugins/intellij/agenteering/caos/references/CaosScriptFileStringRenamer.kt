package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parameter
import com.badahori.creatures.plugins.intellij.agenteering.utils.INTELLIJ_LOG
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.bedalton.common.util.PathUtil
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.NameSuggester
import java.net.URI

class CaosScriptFileStringRenamer(file: PsiFile, val newName: String) : AutomaticRenamer() {

    private val oldAbsolutePath: String = file.virtualFile.path
    private val parentPath: String? = file.virtualFile.parent?.path

    override fun getDialogTitle(): String {
        return CaosBundle.message("caos.file-renamer.renamer")
    }

    override fun getDialogDescription(): String {
        @Suppress("DialogTitleCapitalization") // Examples in IntelliJ source seem to have sentence case
        return CaosBundle.message("caos.file-renamer.dialog-description")
    }

    override fun entityName(): String {
        return CaosBundle.message("caos.file-renamer.entity-name")
    }

    init {
        collectFileNameStrings(file)
        suggestAllNames(file.name, newName)
    }

    override fun allowChangeSuggestedName(): Boolean {
        return false
    }

    private fun collectFileNameStrings(file: PsiFile) {
        val project = file.project
        val oldName = file.virtualFile.name.nullIfEmpty()
            ?: return
        val extension = file.virtualFile.extension?.nullIfEmpty()?.let {
            "(\\.$it)?$"
        } ?: ""
        val pathRegex = ".*?$oldName$extension".toRegex(RegexOption.IGNORE_CASE)
        val projectScope = GlobalSearchScope.projectScope(project)
        val strings = CaosScriptStringLiteralIndex
            .instance
            .getByPattern(pathRegex, project, projectScope)
            .values
            .flatten()

        for (string in strings) {
            if (string is PsiNamedElement) {
                myElements.add(string)
            }
        }
    }

    override fun isSelectedByDefault(): Boolean {
        return true
    }

    override fun suggestNameForElement(
        element: PsiNamedElement,
        suggester: NameSuggester,
        newClassName: String,
        oldClassName: String
    ): String {
        if (element !is CaosScriptStringLike) {
            return newClassName
        }
        if (!oldAbsolutePath.endsWith(oldClassName) || PathUtil.getFileNameWithoutExtension(oldAbsolutePath) == oldClassName) {
            Log.iIf(INTELLIJ_LOG) { "Is suggesting name for different path than one in constructor" }
            return newClassName
        }
        return getNewName(element as CaosScriptStringLike, newClassName).also {
            element.putUserData(FILE_RENAME_NAME_IN_STRING_KEY, it)
        }
    }

    /**
     * Calculate the new path for a given string based on its enclosing file and new path
     */
    private fun getNewName(element: CaosScriptStringLike, newNameIn: String): String {

        if (element !is PsiNamedElement) {
            return newNameIn
        }

        // Get if string is in CAOS2
        val caos2 = element.getParentOfType(CaosScriptCaos2Value::class.java)

        // Determine if path should be relative to file
        val isFilenameOnlyWithoutPath = caos2 == null || caos2.parent !is CaosScriptCaos2Command

        // Determine if String should keep extension
        var needsExtension = caos2 != null
        if (caos2 != null && caos2.parent is CaosScriptCaos2Tag) {
            val tag = (caos2.parent as CaosScriptCaos2Tag)
                .tagName
                .lowercase()
            needsExtension = (tag !in prayNoExtensionLowercase)/*.also {
                Log.i {
                    if (it) {
                        "TAG[$tag] -> Needs Extension -> YES"
                    } else {
                        "TAG[$tag] -> Needs Extension -> NO; Not in $prayNoExtensionLowercase"
                    }
                }
            }*/
        } /* else if (caos2 != null) {
            Log.i { "Caos2Value parent is ${caos2.parent?.className}" }
        } else {
            Log.i { "String: ${(element as PsiNamedElement).name} is not CAOS2" }
        }*/

        // Get the elements current/old path
        val oldPath = element.stringValue

        if (caos2 == null) {
            val parameter = element
                .getParentOfType(CaosScriptArgument::class.java)
                ?.parameter
            val variant = element.variant ?: CaosVariant.DS
            val isCaosFile = parameter?.valuesList?.get(variant)?.name?.startsWith("File.") == true
            if (isCaosFile) {
                needsExtension = PathUtil.getFileNameWithoutExtension(oldPath) != PathUtil.getLastPathComponent(oldPath)
            }
        }

        // If file has no relative path
        return when {
            !needsExtension -> PathUtil.getFileNameWithoutExtension(newNameIn) ?: newNameIn
            isFilenameOnlyWithoutPath -> PathUtil.getLastPathComponent(newNameIn) ?: newNameIn
            else -> {
                val parent = element.virtualFile?.parent
                if (parent == null) {
                    Log.iIf(INTELLIJ_LOG) { "Element should have virtual file parent" }
                    return newNameIn
                }
                val newAbsolutePath = if (PathUtil.isAbsolute(newNameIn)) {
                    newNameIn
                } else {
                    PathUtil.combine(parentPath ?: parent.path, newNameIn)
                }
                val relativePath = URI(parent.path).relativize(URI(newAbsolutePath)).path
//                Log.i { "NewAbsolutePath: $newAbsolutePath; Parent: ${parent.path}; Relativize: $relativePath" }

                if (relativePath == parent.path) {
                    // There was no valid relative path
                    newNameIn
                } else {
                    relativePath
                }
            }
        }
    }

    companion object {
    }
}

internal val FILE_RENAME_NAME_IN_STRING_KEY = Key<String?>("creatures.FILE_NAME_IN_STRING")

private val prayNoExtension = listOf(
    "Agent Animation Gallery",
    "Genetics File",
    "Mother Genetic File",
    "Father Genetic File",
    "Egg Gallery male",
    "Egg Gallery female"
)

private val prayNoExtensionLowercase = prayNoExtension.map { it.lowercase() }