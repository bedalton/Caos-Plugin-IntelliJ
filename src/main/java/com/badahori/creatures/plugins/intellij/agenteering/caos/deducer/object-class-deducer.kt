@file:Suppress("UNUSED_PARAMETER", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptAssignsTarg
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsVariable
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.intellij.psi.PsiElement

object ObjectClassDeducer {

    fun getTargType(element: PsiElement) : List<AgentClass>? {
        // TODO implement
        return null
    }

    fun getOwnerClass(element: PsiElement) : AgentClass? {
        return element.getParentOfType(CaosScriptEventScript::class.java)?.let {
            AgentClass(it.family, it.genus, it.species)
        }
    }

}

private fun getTargAssignments(element:PsiElement, lastChecked:List<CaosScriptIsVariable>) : List<CaosScriptAssignsTarg> {
    //val enumAgentClass = element.getParentOfType()
    // TODO implement
    return emptyList()
}

private val concreteAgentCommands by lazy {
    listOf(
        "TARG",
        "PNTR",
        "CREA",
        "OWNR",
        "NCLS",
        "NULL",
        "PCLS",
        "TWIN",
        "HHLD",
        "NORN",
        "MTOC"
    )
}
