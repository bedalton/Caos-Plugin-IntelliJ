@file:Suppress("unused", "UNUSED_PARAMETER")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.psi.search.GlobalSearchScope

object CaosScriptVariableTypeDeducer {


    fun infer(variable:CaosScriptIsVariable, skipNames:MutableList<CaosScriptIsVariable>) : Set<CaosExpressionValueType>  {
        return emptySet()
    }


}


private object NumberedVariableTypeDeducer {

    fun infer(variable: CaosScriptIsVariable, skipNames:MutableList<CaosScriptIsVariable>) : Set<CaosExpressionValueType> {
        return emptySet()
    }

}



private object NamedVariableTypeDeducer {

    fun infer(variable: CaosScriptIsVariable, skipNames:MutableList<CaosScriptIsVariable>) : Set<CaosExpressionValueType> {
        //TODO()
        return emptySet()
    }

}


private object GameVariableTypeDeducer {
    fun infer(variable: CaosScriptNamedGameVar, skipNames:MutableList<CaosScriptIsVariable>) : Set<CaosExpressionValueType> {
        val module = variable.containingCaosFile?.module

        val scope = if (module != null)
            GlobalSearchScope.moduleScope(module)
        else
            GlobalSearchScope.everythingScope(variable.project)
        val variableName = variable.name
        val thisScriptParent = variable.getParentOfType(CaosScriptScriptElement::class.java)
        val thisPosition = variable.startOffset
        var variables = CaosScriptNamedGameVarIndex.instance[variableName, variable.project, scope]
            .filter { other ->
                isSimilar(variable, other) && isNotEquivalentToAny(other, skipNames)
                        && other.parent is CaosScriptLvalue
                        && other.parent?.parent is CaosScriptIsAssignment
            }
        if (variable.varType == CaosScriptNamedGameVarType.NAME || variable.varType == CaosScriptNamedGameVarType.MAME) {
            variables = variables.filter {
                // TODO FILTER BY OWNR Object
                true
            }
        }
        return variables
            .mapNotNull { other ->
                val otherVariableParentScript = other.getParentOfType(CaosScriptScriptElement::class.java)
                // Ensure if they are in the same script, that they are not assigned after this variable
                if (otherVariableParentScript?.isEquivalentTo(thisScriptParent).orFalse() && thisPosition < other.startOffset)
                    null
                else
                    getAssignedType(other.parent.parent as CaosScriptIsAssignment)
            }
            .toSet()
    }
}

private fun getAssignedType(other:CaosScriptIsAssignment) : CaosExpressionValueType {
    return when (other.commandStringUpper) {
        "SETS" -> STRING
        "SETA" -> AGENT
        "SETV" -> DECIMAL
        "RNDV" -> DECIMAL
        "NET: UNIK" -> STRING
        "ADDS" -> STRING
        "NEGV", "ABSV", "NOTV" -> DECIMAL
        "CHAR" -> STRING
        "NEW: GENE" -> INT
        "ADDV", "SUBV", "MULV", "DIVV", "MODV" -> DECIMAL
        "ORRV", "ANDV" -> INT
        else -> INT // TODO Should I throw an error here until I get this sorted
    }
}

private fun isNotEquivalentToAny(aVariable: CaosScriptIsVariable, others:List<CaosScriptIsVariable>) : Boolean {
    return others.none { other ->
        aVariable.isEquivalentTo(other)
    }
}

private fun isSimilar(aVariable: CaosScriptIsVariable, other:CaosScriptIsVariable) : Boolean {
    return when (aVariable) {
        is CaosScriptVarToken -> {
            if (other is CaosScriptVarToken) {
                if (aVariable.varIndex == other.varIndex) {
                    val otherIsEventVar = (other.varGroup == CaosScriptVarTokenGroup.VARx || other.varGroup == CaosScriptVarTokenGroup.VAxx)
                    when (aVariable.varGroup) {
                        CaosScriptVarTokenGroup.VAxx, CaosScriptVarTokenGroup.VARx -> otherIsEventVar
                        else -> !otherIsEventVar
                    }
                } else {
                    false
                }
            } else {
                false
            }
        }
        is CaosScriptNamedGameVar -> {
            if (other is CaosScriptNamedGameVar) {
                when (val variableType = aVariable.varType) {
                    CaosScriptNamedGameVarType.NAME, CaosScriptNamedGameVarType.MAME -> aVariable.name == other.name
                    CaosScriptNamedGameVarType.GAME -> other.varType == variableType && aVariable.name == other.name
                    CaosScriptNamedGameVarType.EAME -> other.varType == variableType && aVariable.name == other.name
                    else -> false
                }
            } else {
                false
            }
        }
        else -> false
    }
}