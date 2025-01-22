package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClassConstants.INDETERMINATE_VALUE
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException

typealias AgentClass = com.bedalton.creatures.common.structs.AgentClass

val AgentClass.hasIndeterminateValue get() = family == INDETERMINATE_VALUE
        || genus == INDETERMINATE_VALUE
        || species == INDETERMINATE_VALUE

val AgentClass.isCompletelyIndeterminate get() = family == INDETERMINATE_VALUE
        && genus == INDETERMINATE_VALUE
        && species == INDETERMINATE_VALUE

object AgentClassConstants {
    const val INDETERMINATE_VALUE = -255

    val POINTER get() = AgentClass.POINTER

    val CREATURE: AgentClass get() = AgentClass.CREATURE

    val ZERO: AgentClass get() = AgentClass.ZERO

    val UNPARSABLE_CLASS by lazy {
        AgentClass(INDETERMINATE_VALUE, INDETERMINATE_VALUE, INDETERMINATE_VALUE)
    }
}

object CaosAgentClassUtils {

    fun toClas(agentClass: AgentClass) : Int {
        return toClas(agentClass.family, agentClass.genus, agentClass.species)
    }

    fun toClas(family:Int, genus:Int, species:Int) : Int {
        val familyInt = family shl 24
        val genusInt = genus shl 16
        val speciesInt = species shl 8
        return familyInt or genusInt or speciesInt
    }

    fun parseClas(clas:Int) : AgentClass? {
        try {
            val family = (clas shr 24) and 0xFF
            val genus = (clas shr 16) and 0xFF
            val species = (clas shr 8) and 0xFF
            return AgentClass(family, genus, species)
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
            return null
        }
    }

}