package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

data class AgentClass(val family: Int, val genus: Int, val species: Int) {
    fun like(otherClass: AgentClass): Boolean {
        if (notMatches(family, otherClass.family))
            return false
        if (notMatches(genus, otherClass.genus))
            return false
        if (notMatches(species, otherClass.species))
            return false
        return true
    }

    private fun notMatches(val1:Int, val2:Int) : Boolean {
        return val1 != val2 && val1 != 0 && val2 != 0
    }

    companion object {
        val POINTER by lazy {
            AgentClass(2, 1, 1)
        }

        val CREATURE: AgentClass by lazy {
            AgentClass(4, 0, 0)
        }
        val ZERO by lazy {
            AgentClass(0, 0, 0)
        }
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