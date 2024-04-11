package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClassConstants.INDETERMINATE_VALUE

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
        val fHex = Integer.toHexString(family).padStart(2,  '0')
        val gHex = Integer.toHexString(genus).padStart(2, '0')
        val sHex = Integer.toHexString(species).padStart(2, '0')
        return Integer.parseInt("$fHex$gHex${sHex}00", 16)
    }

    fun parseClas(clas:Int) : AgentClass? {
        val hex = Integer.toHexString(clas).padStart(8,'0')
        val family = Integer.parseInt(hex.substring(0, 2), 16)
        val genus = Integer.parseInt(hex.substring(2, 4), 16)
        val species = Integer.parseInt(hex.substring(4, 6), 16)
        return AgentClass(family, genus, species)
    }

}