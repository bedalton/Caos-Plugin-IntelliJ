package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Tag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Value
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parameter
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.bedalton.common.util.PathUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.NameSuggester
import java.net.URI

class CaosScriptFileStringRenamer(file: PsiFile, val newName: String): AutomaticRenamer() {

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
    }


    private fun collectFileNameStrings(file: PsiFile) {
        val project = file.project
        val oldName = file.virtualFile.name.nullIfEmpty()
            ?: return
        val extension = file.virtualFile.extension?.nullIfEmpty()?.let {
            "(.$it)?$"
        } ?: ""
        val pathRegex = ".*?$oldName$extension".toRegex(RegexOption.IGNORE_CASE)
        val projectScope = GlobalSearchScope.projectScope(project)
        val strings = CaosScriptStringLiteralIndex
            .instance
            .getByPattern(pathRegex, project, projectScope)
            .values
            .flatten()

        for (string in strings) {
            addElement(string)
        }
    }


    private fun addElement(element: CaosScriptStringLike) {
        myElements.add(element as PsiNamedElement)
        suggestAllNames(element.stringValue, getNewName(element, newName))
    }

    override fun suggestNameForElement(
        element: PsiNamedElement,
        suggester: NameSuggester,
        newClassName: String,
        oldClassName: String
    ): String {
        assert(element is CaosScriptStringLike)
        assert(oldAbsolutePath.endsWith(oldClassName)) { "Is suggesting name for different path than one in constructor" }
        return getNewName(element as CaosScriptStringLike, newClassName)
    }

    /**
     * Calculate the new path for a given string based on its enclosing file and new path
     */
    private fun getNewName(element: CaosScriptStringLike, newNameIn: String): String {

        assert(element !is PsiNamedElement) { "Can only rename string elements here"}

        // Get if string is in CAOS2
        val caos2 = element.getParentOfType(CaosScriptCaos2Value::class.java)

        // Determine if path should be relative to file
        val relative = caos2 != null

        // Determine if String should keep extension
        var needsExtension = caos2 != null
        if (caos2 != null && caos2.parent is CaosScriptCaos2Tag) {
            val tag = (caos2.parent as CaosScriptCaos2Tag)
                .tagName
                .lowercase()
            needsExtension = tag in prayNoExtensionLowercase
        }

        // Get the elements current/old path
        val oldPath = element.stringValue

        if (caos2 == null) {
            val parameter = element
                .getParentOfType(CaosScriptArgument::class.java)
                ?.parameter
            val variant = element.variant ?: CaosVariant.DS
            val isCaosFile = parameter?.valuesList?.get(variant)?.name?.startsWith("File.") == true
            if (!isCaosFile) {
                needsExtension = PathUtil.getFileNameWithoutExtension(oldPath) == PathUtil.getLastPathComponent(oldPath)
            }
        }

        // If file has no relative path
        return when {
            !relative && needsExtension -> PathUtil.getLastPathComponent(newNameIn) ?: newNameIn
            !relative && !needsExtension -> PathUtil.getFileNameWithoutExtension(newNameIn) ?: newNameIn
            else -> {
                val parent = element.virtualFile?.parent
                assert(parent != null) { "Element should have virtual file parent" }
                val newAbsolutePath = if (PathUtil.isAbsolute(newNameIn)) {
                    newNameIn
                } else {
                    PathUtil.combine(parentPath ?: parent!!.path, newNameIn)
                }
                URI(newAbsolutePath).relativize(URI(parent!!.path)).path
            }
        }
    }

    override fun suggestAllNames(oldClassName: String?, newClassName: String?) {
        super.suggestAllNames(oldClassName, newClassName)
    }

    companion object {
        private val prayNoExtension = listOf(
            "Agent Animation Gallery",
            "Genetics File",
            "Mother Genetic File",
            "Father Genetic File",
            "Egg Gallery male",
            "Egg Gallery female"
        )

        private val prayNoExtensionLowercase = prayNoExtension.map { it.lowercase() }
    }
}