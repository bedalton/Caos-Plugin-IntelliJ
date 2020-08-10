package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor

class CaosScriptVarReferencesSearch : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, processor: Processor<in PsiReference>) {
        val element = parameters.elementToSearch
        if (element is CaosDefCommand) {
            val varGroup = when (element.text.toUpperCase()) {
                "VARX" -> CaosScriptVarTokenGroup.VARx
                "VAXX" -> CaosScriptVarTokenGroup.VAxx
                "OBVX" -> CaosScriptVarTokenGroup.OBVx
                "OVXX" -> CaosScriptVarTokenGroup.OVxx
                "MVXX" -> CaosScriptVarTokenGroup.MVxx
                else -> return
            }
            val range = when (varGroup) {
                CaosScriptVarTokenGroup.VARx, CaosScriptVarTokenGroup.OBVx -> 9
                else -> 99
            }
            val prefix = when (varGroup) {
                CaosScriptVarTokenGroup.VARx -> "var"
                CaosScriptVarTokenGroup.VAxx -> "va"
                CaosScriptVarTokenGroup.OBVx -> "obv"
                CaosScriptVarTokenGroup.OVxx -> "ov"
                CaosScriptVarTokenGroup.MVxx -> "mv"
                else -> return
            }
            val pad = range > 10
            //parameters.optimizer.searchWord(varGroup.value, parameters.effectiveSearchScope, false, element)
            for (i in 0..range) {
                val number = if (pad) "$i".padStart(2, '0') else "$i"
                //  parameters.optimizer.searchWord(prefix + number, parameters.effectiveSearchScope, false, element)
            }
            return
        }
        val reference = parameters.elementToSearch.getSelfOrParentOfType(CaosScriptVarToken::class.java)
                ?: return
        LOGGER.info("Process references for token")
        parameters.optimizer.searchWord(reference.varGroup.value, parameters.effectiveSearchScope, false, reference)
        parameters.optimizer.searchWord(reference.text, parameters.effectiveSearchScope, false, reference)
    }
}