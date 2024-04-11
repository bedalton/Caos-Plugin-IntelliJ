@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.variable
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClassConstants.UNPARSABLE_CLASS
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement

interface CaosScriptAssignsTarg : CaosScriptCompositeElement {
    val targClassifier: AgentClass?

//    val targAgentClass: AgentClass? get() {
//        this.getUserData(TARG_CLASS_KEY)?.let {
//            if (it.second == this.text.lowercase()) {
//                return it.first
//            } else {
//                this.putUserData(TARG_CLASS_KEY, null)
//            }
//        }
//        if (!this.isTargAgentClassSetter) {
//            return null
//        }
//        return getAgentClassGeneric(this)
//    }

    /**
     * Allows marking a multi-command element as this command not setting ownr
     */
    val isTargAgentClassSetter: Boolean get() = false

    companion object {
        val UNPARSABLE_TARG get() = UNPARSABLE_CLASS
    }
}

private fun getAgentClassGeneric(element: PsiElement): AgentClass? {
    return null
}


fun PsiElement.targAgentClass(follow:Boolean):List<AgentClass>? {
    val AgentClass = this.getUserData(TARG_CLASS_KEY)
    // TODO implement
    return null
}


private fun getTargAt(element:PsiElement, follow: Boolean, lastChecked: MutableList<PsiElement>): List<AgentClass> {
    val variant = element.variant
        ?: return emptyList()
    var AgentClasss = element.getSelfOrParentOfType(CaosScriptEnumNextStatement::class.java)?.let { enum ->
        if (lastChecked.any { it.isEquivalentTo(enum)})
            return emptyList()
        lastChecked.add(enum)
        //if ()
        val arguments = enum.enumHeaderCommand.arguments.filterIsInstance<CaosScriptRvalue>()
        val temp = if (arguments.size != 3)
            null
        else
            parseAgentClass(variant, follow, arguments[0], arguments[1], arguments[2], lastChecked)
    }
    // TODO implement
    return emptyList()
}

private fun getTargOf(element:PsiElement, follow:Boolean, lastChecked: MutableList<PsiElement>) : List<AgentClass> {
    if (element is CaosScriptRvalue) {
        return getAgentClassFromRvalue(element, follow, lastChecked)
    }
    // TODO implement
    return emptyList()
}

private fun getAgentClassFromRvalue(value:CaosScriptRvalue, follow:Boolean, lastChecked: MutableList<PsiElement>) : List<AgentClass> {
    if (lastChecked.any { it.isEquivalentTo(value)})
        return emptyList()
    lastChecked.add(value)
    (value.rvaluePrime)?.let { prime ->
        return when (value.rvaluePrime?.commandStringUpper) {
            "PNTR" ->  listOf(AgentClass(2, 1, 1))
            "NORN", "MTOC","HHLD", "CREA"  -> listOf(AgentClass(4, 0, 0))
            "TWIN" -> prime.arguments.firstOrNull()?.let { agentArg ->
                    getTargOf(agentArg, follow, lastChecked)
                } ?: emptyList()
            "TARG" -> {
                emptyList()
            }
            else -> listOf(AgentClass(0,0,0))
        }
    }
    return emptyList()
}

private fun parseAgentClass(variant:CaosVariant, follow: Boolean, family:CaosScriptRvalue, genus:CaosScriptRvalue, specie:CaosScriptRvalue, lastChecked: MutableList<PsiElement>) : List<AgentClass>? {
    val families = getInt(variant, family, follow, lastChecked = lastChecked)
        ?: return null
    val genuses = getInt(variant, genus, follow, lastChecked = lastChecked)
        ?: return null
    val species = getInt(variant, specie, follow, lastChecked = lastChecked)
        ?: return null
    return families.flatMap { aFamily ->
        genuses.flatMap { aGenus ->
            species.map { aSpecie ->
                AgentClass(aFamily, aGenus, aSpecie)
            }
        }
    }
}

private fun getInt(variant:CaosVariant, value:CaosScriptRvalue, follow:Boolean, lastChecked: MutableList<PsiElement>) : List<Int>? {
    if (value.isInt)
        return listOf(value.intValue!!)
    value.variable?.let { variable ->
        return if (follow)
            getVariableIntValue(variant, variable, lastChecked)
        else
            null
    }
    return null
}

fun getVariableIntValue(variant: CaosVariant, variable:CaosScriptIsVariable, lastChecked:MutableList<PsiElement>) : List<Int>? {
    lastChecked.add(variable)
    return when (variable) {
        is CaosScriptVarToken -> getVarTokenIntValues(variant, lastChecked)
        is CaosScriptNamedGameVar -> getNamedGameIntValues(variant, lastChecked)
        else -> null
    }
}


fun getVarTokenIntValues(variant: CaosVariant, lastChecked:MutableList<PsiElement>) : List<Int>? {
    return null
}

fun getNamedGameIntValues(variant: CaosVariant, lastChecked:MutableList<PsiElement>) : List<Int>? {
    return null
}

internal val TARG_CLASS_KEY:Key<Pair<AgentClass, String>> = Key.create("creatures.caos.TARG_AgentClass")