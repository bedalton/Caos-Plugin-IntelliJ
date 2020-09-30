package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.variants
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class CaosDefElementsSearchExecutor : QueryExecutor<PsiElement, ReferencesSearch.SearchParameters> {

    /**
     * Searches for references to CaosDefElements
     */
    override fun execute(parameters: ReferencesSearch.SearchParameters, processor: Processor<in PsiElement>): Boolean {
        val element = parameters.elementToSearch
        val project = element.project
        if (element !is CaosDefCommand) {
            return false
        }
        val variants = element.variants
        getVarGroupIfAny(element)?.let {varGroup ->
            checkVarReferences(variants, project, varGroup, processor)
            return true
        }

        (element as? CaosDefValuesListValueKey)?.let {
            isReferenceTo(variants, project, it.reference, processor)
            return true
        }

        (element as? CaosScriptIsCommandToken)?.let {
            isReferenceTo(variants, project, element.reference, processor)
            return true
        }

        return false
    }

    private fun getVarGroupIfAny(element:CaosDefCommand) : CaosScriptVarTokenGroup? {
        return when (element.text.toUpperCase()) {
            "VARX" -> CaosScriptVarTokenGroup.VARx
            "VAXX" -> CaosScriptVarTokenGroup.VAxx
            "OBVX" -> CaosScriptVarTokenGroup.OBVx
            "OVXX" -> CaosScriptVarTokenGroup.OVxx
            "MVXX" -> CaosScriptVarTokenGroup.MVxx
            else -> null
        }
    }

    private fun checkVarReferences(variants:List<CaosVariant>, project:Project, varGroup: CaosScriptVarTokenGroup, processor: Processor<in PsiElement>) {
        FilenameIndex.getAllFilesByExt(project, "cos")
                .mapNotNull { it.getPsiFile(project) as? CaosScriptFile }
                .flatMap map@{file->
                    if (file.variant !in variants)
                        return@map emptyList<PsiElement>()
                    PsiTreeUtil.collectElementsOfType(file, CaosScriptVarToken::class.java)
                            .filter { it.varGroup == varGroup}
                }
                .forEach {
                    processor.process(it)
                }
    }

    private fun isReferenceTo(variants:List<CaosVariant>, project:Project, reference: PsiReference, processor: Processor<in PsiElement>) {
        FilenameIndex.getAllFilesByExt(project, "cos")
                .mapNotNull { it.getPsiFile(project) as? CaosScriptFile }
                .flatMap map@{file->
                    if (file.variant !in variants)
                        return@map emptyList<PsiElement>()
                    PsiTreeUtil.collectElementsOfType(file, CaosScriptExpression::class.java)
                            .filter { expression -> reference.isReferenceTo(expression) }
                }
                .forEach {
                    processor.process(it)
                }
    }


}