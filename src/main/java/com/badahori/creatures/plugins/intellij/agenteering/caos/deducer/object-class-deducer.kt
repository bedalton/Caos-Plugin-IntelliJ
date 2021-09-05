@file:Suppress("UNUSED_PARAMETER", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptAssignsTarg
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsVariable
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.intellij.psi.PsiElement

object ObjectClassDeducer {

    fun getTargType(element: PsiElement) : List<Classifier>? {
        // TODO implement
        return null
    }

    fun getOwnerClass(element: PsiElement) : Classifier? {
        return element.getParentOfType(CaosScriptEventScript::class.java)?.let {
            Classifier(it.family, it.genus, it.species)
        }
    }

}

data class Classifier(
    val family:Int = 0,
    val genus:Int = 0,
    val species:Int = 0
) {

    // Check if classifiers are alike
    fun isLike(other:Classifier) : Boolean {
        if (family != other.family && family != 0 && other.family != 0)
            return false
        if (genus != other.genus && genus != 0 && other.genus != 0)
            return false
        return species == other.species || species == 0 || other.species == 0
    }

    companion object {
        val NULL = Classifier(-1,-1,-1)
    }
}

private fun getTargAssignments(element:PsiElement, lastChecked:List<CaosScriptIsVariable>) : List<CaosScriptAssignsTarg> {
    //val enumClassifier = element.getParentOfType()
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
