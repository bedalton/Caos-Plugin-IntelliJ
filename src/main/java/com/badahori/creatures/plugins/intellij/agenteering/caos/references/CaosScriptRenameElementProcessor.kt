package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.nullIfUndefOrBlank
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class CaosScriptRenameElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is CaosScriptSubroutine
                || element is CaosScriptSubroutineName
                || element is CaosScriptNamedGameVar
                || element is CaosScriptQuoteStringLiteral
                || element is CaosScriptStringLike
    }

    override fun isInplaceRenameSupported(): Boolean {
        return true
    }

    override fun findCollisions(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<out PsiElement, String>,
        result: MutableList<UsageInfo>
    ) {
        super.findCollisions(element, newName, allRenames, result)
        val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
            .filter {
                it.name == newName && it.subroutineHeader.subroutineName != null
            }.forEach{
                result.add(UsageInfo(it.subroutineHeader.subroutineName ?: it.subroutineHeader))
            }
    }

    override fun findExistingNameConflicts(
        element: PsiElement,
        newName: String,
        conflicts: MultiMap<PsiElement, String>
    ) {
        super.findExistingNameConflicts(element, newName, conflicts)
        val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
            ?: return
        PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
            .filter {
                it.name == newName
            }.forEach each@{
                val name = it.subroutineHeader.subroutineName
                    ?: return@each
                conflicts.put(name, listOf(name.text))
            }
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        val project = element.project
        if (project.isDisposed) {
            return emptyList()
        }
        val superReference: Collection<PsiReference> = super.findReferences(element, searchScope, searchInCommentsAndStrings)
        searchInCommentsAndStrings && !DumbService.isDumb(project)
        val stringFileMatches = if (searchInCommentsAndStrings) {
            val string = element.getParentOfType(CaosScriptStringLike::class.java)
            if (string != null) {
                getFileNameMatchingString(project, string)
            } else if (element is PsiFile) {
                getStringsMatchingFilename(project, element)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
        return (superReference + stringFileMatches)
            .distinct()

    }

    private fun getStringsMatchingFilename(project: Project, file: PsiFile): Collection<PsiReference> {
        val fileName = file.name
            .nullIfEmpty()
            ?: return emptyList()
        val extension = file.virtualFile.extension
        val stubKind = if (extension != null) {
            StringStubKind.values().firstOrNull kind@{ kind ->
                val extensions = kind.extensions
                    ?: return@kind false
                extension likeAny extensions
            }
        } else {
            null
        }

        // Get strings matching name exactly
        val literalNames = CaosScriptStringLiteralIndex.instance[fileName, project]
        val all = if (stubKind != null) {
            // Get strings matching name without extensions,
            // whose parameter defined extension matches
            literalNames + CaosScriptStringLiteralIndex.instance[fileName, project]
                .filter { it.stringStubKind == stubKind }
        } else {
            literalNames
        }
        return all.distinctBy { it.textRange }
            .mapNotNull { it.reference }
    }

    private fun getFileNameMatchingString(project: Project, quoteStringLiteral: CaosScriptStringLike): Collection<PsiReference> {
        if (DumbService.isDumb(project)) {
            return emptyList()
        }
        val extensions = quoteStringLiteral.stringStubKind.extensions
            ?: return emptyList()
        val name = quoteStringLiteral.stringValue
            .nullIfEmpty()
            ?: return emptyList()
        val nameHasExtension = extensions.any { name.lowercase().endsWith(it.lowercase()) }
        val files = if (nameHasExtension) {
            CaseInsensitiveFileIndex.findWithFileName(project, fileName = name)
        } else {
            CaseInsensitiveFileIndex.findWithFileNameAndExtensions(project, name, extensions)
        }.distinctBy { it.path }
        return files.mapNotNull { it.getPsiFile(project)?.reference }
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        if (element is CaosScriptSubroutineName) {
            val name = element.name.nullIfUndefOrBlank()
                    ?: return null
            val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
                    ?: return null
            return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
                .filter {
                    it.name == name
                }.firstNotNullOfOrNull {
                    it.subroutineHeader.subroutineName
                }
        }
        if (element is CaosScriptSubroutine) {
            return element.subroutineHeader.subroutineName
        }
        if (element is CaosScriptQuoteStringLiteral)
            return element
        return super.substituteElementToRename(element, editor)
    }


}