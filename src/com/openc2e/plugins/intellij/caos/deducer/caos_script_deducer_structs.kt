package com.openc2e.plugins.intellij.caos.deducer

import com.intellij.openapi.util.TextRange
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.CaosExpressionValueType
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptNamedGameVarType
import kotlin.math.max
import kotlin.math.min


data class CaosScope(val range:TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
}

fun rootScope(file:CaosScriptFile) : CaosScope {
    return CaosScope(file.textRange, CaosScriptBlockType.MACRO, emptyList())
}

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
