@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

data class CaosBlockCondition(
        val value1:CaosVar,
        val value2:CaosVar,
        val eqOp:CaosScriptEqualityOp
)

sealed class CaosLoopCondition {
    object Ever:CaosLoopCondition()
    data class Untl(val condition: CaosBlockCondition):CaosLoopCondition()
}

enum class CaosScriptEqualityOp (val expr:List<String>, private val check:(value1:Int, value2:Int) -> Boolean) {
    EQUAL(listOf("eq", "="), {value1, value2 -> value1 == value2 }),
    NOT_EQUAL(listOf("ne", "<>"), {value1, value2 -> value1 != value2 }),
    LESS_THAN(listOf("lt", "<"), {value1, value2 -> value1 < value2 }),
    LESS_THAN_EQUAL(listOf("le", "<="), {value1, value2 -> value1 <= value2 }),
    GREATER_THAN(listOf("gt", ">"), {value1, value2 -> value1 > value2 }),
    GREATER_THAN_EQUAL(listOf("ge", ">="), {value1, value2 -> value1 >= value2 }),
    BITWISE_AND(listOf("bt"), {value1, value2 -> value1 and value2 == value2 }),
    BITWISE_NAND(listOf("bf"), {value1, value2 -> (value1 and value2) != value2 }),
    ANY(listOf("??"), { _:Int, _:Int -> true});

    fun matches(value1:Int, value2:Int) : Boolean {
        return check(value1, value2)
    }
}
