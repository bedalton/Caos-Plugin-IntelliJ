package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import bedalton.creatures.util.toArrayOf
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.psiDirectory
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.badahori.creatures.plugins.intellij.agenteering.utils.getFilesWithExtension
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil


internal fun getNamedGameVarNames(
    project: Project,
    element: PsiElement,
    expandedSearch: Boolean,
    types: List<CaosScriptNamedGameVarType>,
): Set<String> {

    return getNamedGameVarElements(project, element, expandedSearch, types)
        .map { gameVar ->
            gameVar.name
        }
        .toSet()
}


internal inline fun getAllNamedVarsFromRawFiles(
    project: Project,
    module: Module?,
    parent: VirtualFile?,
    check: (element: CaosScriptNamedGameVar) -> Boolean
): List<CaosScriptNamedGameVar> {
    val files = if (DumbService.isDumb(project)) {
        getFilesWithExtension(project, module, parent, "cos")
    } else {
        (FilenameIndex.getAllFilesByExt(project, "cos") + FilenameIndex.getAllFilesByExt(project, "COS"))
    }
    return files
        .distinctBy { it.path.lowercase() }
        .flatMap map@{
            val psiFile = it.getPsiFile(project)
                ?: return@map emptyList()
            PsiTreeUtil.collectElementsOfType(psiFile, CaosScriptNamedGameVar::class.java)
                .filter(check)
        }
}


internal fun getNamedGameVarElements(
    project: Project,
    element: PsiElement,
    expandedSearch: Boolean,
    types: List<CaosScriptNamedGameVarType>,
): List<CaosScriptNamedGameVar> {
    val filter = { gameVar: CaosScriptNamedGameVar ->
                gameVar.varType in types &&
                gameVar.rvalue?.text?.trim()?.startsWith('"') == true
    }
    val rawElements = if (DumbService.isDumb(project)) {
        val files = element.psiDirectory?.files
            ?: element.containingFile?.toArrayOf()
            ?: element.originalElement?.containingFile?.toArrayOf()
            ?: return emptyList()
        return files.flatMap { file ->
            PsiTreeUtil.collectElementsOfType(file, CaosScriptNamedGameVar::class.java)
        }
            .filter(filter)
    } else {
        val searchScope = getNamedGameVarScope(project, element, expandedSearch)
        CaosScriptNamedGameVarIndex.instance.getAllInScope(project, searchScope)
    }

    return rawElements.filter(filter)
}

private fun getNamedGameVarScope(
    project: Project,
    element: PsiElement,
    expandedSearch: Boolean,
): GlobalSearchScope {
    // If index is built, get user defined named variables
    // Get module of containing file
    val module = element.module

    // Get search scope
    // If expanded search is false, only get files in the parent directory
    if (expandedSearch) {
        module?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)
    }
    element.psiDirectory?.let {
        return GlobalSearchScopes.directoryScope(it, true)
    }
    return module?.moduleContentScope
        ?: GlobalSearchScope.projectScope(project)
}

val CaosScriptNamedGameVar.varTypes: List<CaosScriptNamedGameVarType> get() {
    return when (varType) {
        CaosScriptNamedGameVarType.MAME, CaosScriptNamedGameVarType.NAME -> listOf(
            CaosScriptNamedGameVarType.MAME,
            CaosScriptNamedGameVarType.NAME
        )
        CaosScriptNamedGameVarType.GAME -> listOf(CaosScriptNamedGameVarType.GAME)
        CaosScriptNamedGameVarType.EAME -> listOf(CaosScriptNamedGameVarType.EAME)
        else -> return emptyList()
    }
}