@file:Suppress("SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

enum class CaosOp(val value:Int) {
    SETV(0),
    ORRV(1),
    ANDV(2),
    DIVV(3),
    MULV(4),
    ADDV(5),
    SUBV(6),
    NEGV(7),
    MODV(8),
    UNDEF(-1),
    SETS(9);

    companion object {
        fun fromValue(value:Int) : CaosOp = values().first { it.value == value }
    }
}

enum class CaosScriptBlockType(val value:String) {
    UNDEF("UNDEF"),
    SCRP("scrp"),
    ISCR("iscr"),
    RSCR("rscr"),
    MACRO("inst"),
    DOIF("doif"),
    ELIF("elif"),
    ELSE("else"),
    SUBR("subr"),
    ENUM("enum"),
    LOOP("loop"),
    REPS("reps"),
    ESCN("escn");

    companion object {
        fun fromValue(value:String) : CaosScriptBlockType {
            return values().first { it.value == value}
        }
    }
}
