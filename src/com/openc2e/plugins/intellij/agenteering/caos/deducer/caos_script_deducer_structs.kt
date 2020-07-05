package com.openc2e.plugins.intellij.agenteering.caos.deducer

import com.intellij.openapi.util.TextRange
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosScriptFile


data class CaosScope(val range:TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
}

fun CaosScope?.sharesScope(otherScope: CaosScope?) : Boolean {
    if (this == null) {
        return otherScope == null
    }
    if (otherScope == null)
        return false
    if (this == otherScope) {
        return true
    }
    val thisEnclosingScopes = enclosingScope
    val otherEnclosingScope = otherScope.enclosingScope
    for(i in enclosingScope.indices) {
        val parentScope = enclosingScope[i]
        val otherParentScope = otherEnclosingScope[i]
        if (parentScope != otherParentScope) {
            return when (blockType) {
                CaosScriptBlockType.DOIF -> otherParentScope.blockType == CaosScriptBlockType.DOIF
                CaosScriptBlockType.ELIF -> otherParentScope.blockType == CaosScriptBlockType.ELIF
                CaosScriptBlockType.ELSE -> otherParentScope.blockType == CaosScriptBlockType.ELSE
                else -> true
            }
        }
    }
    return true
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
