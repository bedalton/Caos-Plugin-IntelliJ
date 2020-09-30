package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptEqualityOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.QueryableIdentifier as QId
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.QueryableValue.Companion as QV

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

    private fun notMatches(val1: Int, val2: Int): Boolean {
        return val1 != val2 && val1 != 0 && val2 != 0
    }

    private fun matches(val1: Int, val2: Int): Boolean {
        return val1 == val2 || val1 == 0 || val2 == 0
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

data class QueryableAgentClass(
        val family: QId,
        val genus: QId,
        val species: QId) {

    companion object {
        val CREATURE: QueryableAgentClass by lazy {
            QueryableAgentClass(QId.eq(4), QId.ZERO, QId.ZERO)
        }
        val ZERO by lazy {
            QueryableAgentClass(QId.ZERO, QId.ZERO, QId.ZERO)
        }
        val POINTER: QueryableAgentClass by lazy {
            QueryableAgentClass(QId.eq(2), QId.eq(1), QId.eq(1))
        }

    }

}

data class QueryableIdentifier(val values: List<QueryableValue>) {

    constructor(value: QueryableValue) : this(listOf(value))

    constructor(value: Int, eqOp: CaosScriptEqualityOp) : this(QueryableValue(value, eqOp))

    fun matches(value: Int): Boolean {
        return values.all { it.matches(value) }
    }


    companion object {
        val ZERO by lazy {
            QId(listOf(QV.ANY))
        }

        fun eq(value: Int): QId = QId(QV.eq(value))
        fun ne(value: Int): QId = QId(QV.ne(value))
        fun gt(value: Int): QId = QId(QV.gt(value))
        fun gte(value: Int): QId = QId(QV.gte(value))
        fun lt(value: Int): QId = QId(QV.lt(value))
        fun lte(value: Int): QId = QId(QV.lte(value))
        fun bt(value: Int): QId = QId(QV.bt(value))
        fun bf(value: Int): QId = QId(QV.bf(value))

    }
}

data class QueryableValue(val value: Int, val eqOp: CaosScriptEqualityOp) {
    fun matches(otherValue: Int): Boolean = eqOp.matches(value, otherValue)

    companion object {
        val ANY by lazy {
            QueryableValue(0, CaosScriptEqualityOp.ANY)
        }

        fun eq(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.EQUAL)
        fun ne(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.NOT_EQUAL)
        fun gt(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.GREATER_THAN)
        fun gte(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.GREATER_THAN_EQUAL)
        fun lt(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.LESS_THAN)
        fun lte(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.LESS_THAN_EQUAL)
        fun bt(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.BITWISE_AND)
        fun bf(value: Int): QueryableValue = QueryableValue(value, CaosScriptEqualityOp.BITWISE_NAND)
    }
}

object CaosAgentClassUtils {

    fun toClas(agentClass: AgentClass): Int {
        return toClas(agentClass.family, agentClass.genus, agentClass.species)
    }

    fun toClas(family: Int, genus: Int, species: Int): Int {
        val fHex = Integer.toHexString(family).padStart(2, '0')
        val gHex = Integer.toHexString(genus).padStart(2, '0')
        val sHex = Integer.toHexString(species).padStart(2, '0')
        return Integer.parseInt("$fHex$gHex${sHex}00", 16)
    }

    fun parseClas(clas: Int): AgentClass? {
        val hex = Integer.toHexString(clas).padStart(8, '0')
        val family = Integer.parseInt(hex.substring(0, 2), 16)
        val genus = Integer.parseInt(hex.substring(2, 4), 16)
        val species = Integer.parseInt(hex.substring(4, 6), 16)
        return AgentClass(family, genus, species)
    }

}