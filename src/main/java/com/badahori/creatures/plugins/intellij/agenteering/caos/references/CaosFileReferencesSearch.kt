package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor


internal class CaosFileElementsSearchExecutor: QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {

        // Make sure project is valid
        val project = queryParameters.project
        if (project.isDisposed) {
            return
        }

        // Get element
        val element = queryParameters.elementToSearch

        // Make sure element is a file
        if (element is PsiFile) {
            processStringMatchesOfReferencingFiles(project, element, consumer)
        } else if (element is CaosScriptStringLike) {
            processFilesMatchingStringReferences(project, element, consumer)
        }

    }

    private fun processStringMatchesOfReferencingFiles(
        project: Project,
        element: PsiFile,
        consumer: Processor<in PsiReference>
    ) {
        // Get matches as PSI references
        val matchingStrings = FilenameStringCollector
            .collect(project, element.virtualFile.path)
            ?.flattened()
            ?.mapNotNull { it.reference }

        // If no matches
        if (matchingStrings.isNullOrEmpty()) {
            return
        }

        // Process all matches
        for (string in matchingStrings) {
            consumer.process(string)
        }
    }


    private fun processFilesMatchingStringReferences(
        project: Project,
        element: CaosScriptStringLike,
        consumer: Processor<in PsiReference>
    ) {
        val files = CaosStringToFileResolver
            .resolveToFiles(project, element)
            .nullIfEmpty()
            ?: return

        for (virtualFile in files) {
            val psiFile = virtualFile.getPsiFile(project)
                ?.reference
                ?: continue
            consumer.process(psiFile)
        }
    }

}