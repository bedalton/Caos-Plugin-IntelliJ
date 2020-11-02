package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobVirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.variants
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileCollector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class CaosDefElementsSearchExecutor : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {

    /**
     * Searches for references to CaosDefElements
     */
    override fun execute(parameters: ReferencesSearch.SearchParameters, processor: Processor<in PsiReference>): Boolean {
        val element = parameters.elementToSearch
        val project = element.project
        if (DumbService.isDumb(element.project)) {
            DumbService.getInstance(project).runReadActionInSmartMode {
                executeActual(element, processor)
            }
            return true
        }
        return if (ApplicationManager.getApplication().isReadAccessAllowed) {
            executeActual(element, processor)
        } else {
            ApplicationManager.getApplication().runReadAction<Boolean> {
                executeActual(element, processor)
            }
        }
    }

    /**
     * Actual execution method used for use in runReadAction runnables
     */
    private fun executeActual(element: PsiElement, processor: Processor<in PsiReference>): Boolean {
        val project = element.project
        if (element !is CaosDefCompositeElement) {
            return true
        }

        val variants = element.variants
        getVarGroupIfAny(element)?.let { varGroup ->
            return checkVarReferences(variants, project, varGroup, processor)
        }

        (element as? CaosDefValuesListValueKey)?.let {
            return isReferenceTo(variants, project, it.reference, processor)
        }

        (element as? CaosDefCommandWord)?.let {
            return isReferenceTo(variants, project, it, processor)
        }
        return true
    }

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

    private fun getCaosFiles(project: Project): List<CaosScriptFile> {
        return (FilenameIndex.getAllFilesByExt(project, "cos") + CaosVirtualFileCollector.collectFilesWithExtension("cos"))
                .mapNotNull {
                    it.getPsiFile(project) as? CaosScriptFile
                } + getCobCaosScriptFiles(project)
    }

    private fun getCobCaosScriptFiles(project: Project): List<CaosScriptFile> {
        return getCobVirtualFiles(project).filter {
            it.extension == "cos"
        }.mapNotNull map@{
            it.getPsiFile(project) as? CaosScriptFile
        }
    }

    private fun getCobVirtualFiles(project: Project): List<CaosVirtualFile> {
        // Does not need virtual file collector as COBs are physical files
        return FilenameIndex.getAllFilesByExt(project, "cob")
                .flatMap { file ->
                    CobVirtualFileUtil.decompiledCobFiles(file, project)
                }
    }

    /**
     * Checks for var references throughout all
     */
    private fun checkVarReferences(variants: List<CaosVariant>, project: Project, varGroup: CaosScriptVarTokenGroup, processor: Processor<in PsiReference>): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            getCaosFiles(project)
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
    }

    private fun isReferenceTo(variants: List<CaosVariant>, project: Project, command: CaosDefCommandWord, processor: Processor<in PsiReference>): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            (getCaosFiles(project))
                    .flatMap map@{ file ->
                        if (file.variant !in variants)
                            return@map emptyList<PsiReference>()
                        PsiTreeUtil.collectElementsOfType(file, CaosScriptIsCommandToken::class.java)
                                .mapNotNull { it.reference }
                                .filter { reference ->
                                    reference.isReferenceTo(command)
                                }
                    }
                    .all {
                        processor.process(it)
                    } &&
                    FilenameIndex.getAllFilesByExt(project, "caosdef").mapNotNull {
                        it.getPsiFile(project) as? CaosDefFile
                    }.flatMap map@{ file ->
                        if (file.variants.intersect(variants).isEmpty())
                            return@map emptyList<PsiReference>()
                        PsiTreeUtil.collectElementsOfType(file, CaosDefCommandWord::class.java)
                                .mapNotNull { it.reference }
                                .filter { reference ->
                                    reference.isReferenceTo(command)
                                }
                    }
                            .all {
                                processor.process(it)
                            }
        }
    }

    private fun isReferenceTo(variants: List<CaosVariant>, project: Project, reference: PsiReference, processor: Processor<in PsiReference>): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            getCaosFiles(project)
                    .flatMap map@{ file ->
                        if (file.variant !in variants)
                            return@map emptyList<PsiReference>()
                        PsiTreeUtil.collectElementsOfType(file, CaosScriptLiteral::class.java)
                                .filter { expression -> reference.isReferenceTo(expression) }
                                .mapNotNull {
                                    it.reference
                                }

                        PsiTreeUtil.collectElementsOfType(file, CaosScriptEventNumberElement::class.java)
                                .filter { expression -> reference.isReferenceTo(expression) }
                                .mapNotNull { it.reference }
                    }
                    .all {
                        processor.process(it)
                    }
        }
    }


}