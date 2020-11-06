package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobVirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileCollector
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor

class CaosDefElementsSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    /**
     * Processes query by finding references to the element in parameters.elementToSearch
     */
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, processor: Processor<in PsiReference>) {
        val element = parameters.elementToSearch
        val project = parameters.project
        var scope: SearchScope = parameters.effectiveSearchScope
        if (scope is LocalSearchScope) {
            LOGGER.info("$scope: Files: [${scope.virtualFiles.joinToString { it.name }}]")
        } else {
            LOGGER.info("Scope: $scope; ${scope.let { it::class.java.canonicalName }}")
        }
        if (scope.toString() == "EMPTY")
            scope = GlobalSearchScope.everythingScope(project)
        if (element !is CaosDefCompositeElement) {
            return
        }
        val variants = element.variants
        getVarGroupIfAny(element)?.let { varGroup ->
            return checkVarReferences(variants, project, scope, varGroup, processor)
        }

        (element as? CaosDefValuesListValueKey)?.let {
            return isReferenceTo(variants, project, scope, it.reference, processor)
        }

        if (element is CaosDefCommandWord && element.parent?.parent is CaosDefCommandDefElement) {
            LOGGER.info("Checking references for ${element.text}")
            return isReferenceTo(variants, project, scope, element, processor)
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun getVarGroupIfAny(element: CaosDefCompositeElement): CaosScriptVarTokenGroup? {
        return when (element.text.toUpperCase()) {
            "VARX" -> CaosScriptVarTokenGroup.VARx
            "VAXX" -> CaosScriptVarTokenGroup.VAxx
            "OBVX" -> CaosScriptVarTokenGroup.OBVx
            "OVXX" -> CaosScriptVarTokenGroup.OVxx
            "MVXX" -> CaosScriptVarTokenGroup.MVxx
            else -> null
        }
    }

    /**
     * Checks for var references throughout all
     */
    private fun checkVarReferences(variants: List<CaosVariant>, project: Project, scope: SearchScope?, varGroup: CaosScriptVarTokenGroup, processor: Processor<in PsiReference>) {
        getCaosFiles(project, scope)
                .flatMap map@{ file ->
                    if (file.variant !in variants)
                        return@map emptyList<PsiReference>()
                    PsiTreeUtil.collectElementsOfType(file, CaosScriptVarToken::class.java)
                            .filter { it.varGroup == varGroup }
                            .mapNotNull { it.reference }
                }
                .all {
                    processor.process(it)
                }
    }

    /**
     * Resolves references from command words to a given command word element
     */
    private fun isReferenceTo(variants: List<CaosVariant>, project: Project, scope: SearchScope?, command: CaosDefCommandWord, processor: Processor<in PsiReference>) {

        getCaosFiles(project, scope)
                .flatMap map@{ file ->
                    if (file.variant !in variants)
                        return@map emptyList<PsiReference>()
                    LOGGER.info("Scope: $scope has CAOS file ${file.name}")
                    PsiTreeUtil.collectElementsOfType(file, CaosScriptIsCommandToken::class.java)
                            .mapNotNull { it.reference }
                            .filter { reference ->
                                ProgressIndicatorProvider.checkCanceled()
                                reference.isReferenceTo(command)
                            }
                }
                .all {
                    processor.process(it)
                }

        getCaosDefFiles(project, scope).flatMap map@{ file ->
            if (file.variants.intersect(variants).isEmpty())
                return@map emptyList<PsiReference>()
            LOGGER.info("Scope: $scope has DEF file ${file.name}")
            PsiTreeUtil.collectElementsOfType(file, CaosDefCommandWord::class.java)
                    .filter filter@{ wordElement ->
                        ProgressIndicatorProvider.checkCanceled()
                        // Do not match element to self
                        if (wordElement.parent?.parent is CaosDefCommandDefElement)
                            return@filter false
                        wordElement.reference.isReferenceTo(command).orFalse()
                    }
                    .map {
                        it.reference
                    }
        }
                .all {
                    processor.process(it)
                }
    }

    /**
     * Resolves references to rvalues and event numbers
     */
    private fun isReferenceTo(variants: List<CaosVariant>, project: Project, scope: SearchScope?, reference: PsiReference, processor: Processor<in PsiReference>) {
        getCaosFiles(project, scope)
                .flatMap map@{ file ->
                    if (file.variant !in variants)
                        return@map emptyList<PsiReference>()
                    PsiTreeUtil.collectElementsOfType(file, CaosScriptRvalue::class.java)
                            .filter { expression -> reference.isReferenceTo(expression) }
                            .mapNotNull {
                                ProgressIndicatorProvider.checkCanceled()
                                it.reference
                            }

                    PsiTreeUtil.collectElementsOfType(file, CaosScriptEventNumberElement::class.java)
                            .filter { expression ->
                                ProgressIndicatorProvider.checkCanceled()
                                reference.isReferenceTo(expression)
                            }
                            .mapNotNull { it.reference }
                }
                .all {
                    processor.process(it)
                }
    }

    companion object {

        /**
         * Gets all CAOS files in both physical and virtual file systems
         */
        fun getCaosFiles(project: Project, scope: SearchScope? = null): List<CaosScriptFile> {
            return getVirtualFilesWithExtension(project, "cos", scope).mapNotNull map@{
                it.getPsiFile(project) as? CaosScriptFile
            } + getCobCaosScriptFiles(project, scope as? GlobalSearchScope)
        }

        /**
         * Gets all CAOS def files in both physical and virtual file systems
         */
        fun getCaosDefFiles(project: Project, scope: SearchScope? = null): List<CaosDefFile> {
            return getVirtualFilesWithExtension(project, "caosdef", scope).mapNotNull { file ->
                file.getPsiFile(project) as? CaosDefFile
            }
        }

        /**
         * Gets all CAOS scripts inside COB virtual files
         */
        fun getCobCaosScriptFiles(project: Project, scope: GlobalSearchScope? = null): List<CaosScriptFile> {
            return getCobVirtualFiles(project, scope).filter {
                it.extension == "cos"
            }.mapNotNull map@{
                it.getPsiFile(project) as? CaosScriptFile
            }
        }

        /**
         * Gets all virtual files inside COBS
         */
        fun getCobVirtualFiles(project: Project, scope: GlobalSearchScope? = null): List<CaosVirtualFile> {
            // Does not need virtual file collector as COBs are physical files
            if (scope != null) {
                return FilenameIndex.getAllFilesByExt(project, "cob", scope)
                        .flatMap { file ->
                            CobVirtualFileUtil.decompiledCobFiles(file, project)
                        }
            }
            return FilenameIndex.getAllFilesByExt(project, "cob")
                    .flatMap { file ->
                        CobVirtualFileUtil.decompiledCobFiles(file, project)
                    }
        }

        private fun getVirtualFilesWithExtension(project: Project, extension: String, scope: SearchScope? = null): List<VirtualFile> {
            return when (scope) {
                is LocalSearchScope -> scope.virtualFiles.filter { it.extension == extension }
                is GlobalSearchScope -> {
                    FilenameIndex.getAllFilesByExt(project, extension, scope) +
                            CaosVirtualFileCollector.collectFilesWithExtension(extension, scope)
                }
                else -> FilenameIndex.getAllFilesByExt(project, extension) +
                        CaosVirtualFileCollector.collectFilesWithExtension(extension, scope)
            }
        }
    }


}