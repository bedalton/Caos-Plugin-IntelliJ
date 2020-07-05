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

enum class CaosScriptEqualityOp (val expr:List<String>) {
    EQUAL(listOf("eq", "=")),
    NOT_EQUAL(listOf("ne", "<>")),
    LESS_THAN(listOf("lt", "<")),
    LESS_THAN_EQUAL(listOf("le", "<=")),
    GREATER_THAN(listOf("gt", ">")),
    GREATER_THAN_EQUAL(listOf("ge", ">=")),
    BITWISE_AND(listOf("bt")),
    BITWISE_NAND(listOf("bf"))
}
