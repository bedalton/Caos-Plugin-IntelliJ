package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.intellij.openapi.util.TextRange
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile


data class CaosScope(val range:TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
}

fun CaosScope?.sharesScope(otherScopeIn: CaosScope?) : Boolean {
    if (this == null) {
        return otherScopeIn == null
    }
    if (otherScopeIn == null)
        return false
    if (this == otherScopeIn) {
        return true
    }
    val thisEnclosingScopes = enclosingScope
    val otherEnclosingScope = otherScopeIn.enclosingScope
    val longestScope:List<CaosScope>
    val otherScope:List<CaosScope>
    if (thisEnclosingScopes.size > otherEnclosingScope.size) {
        longestScope = thisEnclosingScopes
        otherScope = otherEnclosingScope
    } else {
        longestScope = otherEnclosingScope
        otherScope = thisEnclosingScopes
    }
    for(i in longestScope.indices) {
        val parentScope = longestScope[i]
        val otherParentScope = otherScope.getOrNull(i)
                ?: return true
        if (parentScope != otherParentScope) {
            return when (parentScope.blockType) {
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
