package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.scope
import com.intellij.openapi.util.TextRange

data class CaosScope(val range: TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
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

fun rootScope(file: CaosScriptFile) : CaosScope {
    return CaosScope(file.textRange, CaosScriptBlockType.MACRO, emptyList())
}


fun CaosScriptCompositeElement.sharesScope(otherScope: CaosScope?): Boolean {
    return scope.sharesScope(otherScope)
}

fun CaosScriptCompositeElement.sharesScope(other: CaosScriptCompositeElement): Boolean {
    return scope.sharesScope(other.scope)
}
val CaosScriptCompositeElement.scope: CaosScope?
    get() = (this as? CaosScriptHasCodeBlock ?: this.getParentOfType(CaosScriptHasCodeBlock::class.java))?.scope()
