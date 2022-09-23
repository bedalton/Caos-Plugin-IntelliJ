package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScopeResult.SharedScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.scope
import com.badahori.creatures.plugins.intellij.agenteering.utils.insertFront
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class CaosScope(val file: String?, val range: TextRange, val blockType:CaosScriptBlockType, val parentScope: CaosScope?) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
    val enclosingScope:List<CaosScope> get() {
        return if (parentScope != null) {
            parentScope.enclosingScope + parentScope
        } else {
            emptyList()
        }
    }
}

sealed class CaosScopeResult {
    class SharedScope(val closestScope: CaosScope): CaosScopeResult()
    object NoSharedScope: CaosScopeResult()
    object NoMoreElements: CaosScopeResult()
}

private fun popUntilClosest(scopesIn: List<CaosScope>, otherScopesIn: List<CaosScope>): CaosScopeResult {

    val scopes = scopesIn.sortedByDescending { it.startOffset }.toMutableList()
    val otherScopes = otherScopesIn.sortedByDescending { it.startOffset }.toMutableList()

    if (scopes.isEmpty() || otherScopes.isEmpty()) {
        return CaosScopeResult.NoMoreElements
    }
    // Only elements in the same file could share a scope
    if (scopes.firstNotNullOfOrNull { it.file } != otherScopes.firstNotNullOfOrNull { it.file }) {
        return CaosScopeResult.NoSharedScope
    }

    while (scopes.isNotEmpty() && otherScopes.isNotEmpty()) {
        val theScope = scopes.removeAt(0)
        val theOtherScope = otherScopes.removeAt(0)

        // Quick check to see if they are equal
        if (theScope == theOtherScope) {
            return SharedScope(theScope)
        }

        // Try to find first shared parent scope
        val theParentScope = theScope.parentScope
        val theOtherParentScope = theOtherScope.parentScope
        if (theParentScope == null || theOtherParentScope == null) {
            return if (encloses(theScope, theOtherScope)) {
                SharedScope(theScope)
            } else if (encloses(theOtherScope, theScope)) {
                SharedScope(theOtherScope)
            } else {
                CaosScopeResult.NoMoreElements
            }
        }
        if (theParentScope != theOtherParentScope) {
            if (encloses(theParentScope, theOtherParentScope)) {
                otherScopes.insertFront(theOtherScope)
            } else if (encloses(theOtherScope, theParentScope)) {
                scopes.add(theScope)
            } else {
                return CaosScopeResult.NoSharedScope
            }
            continue
        }
    }
    return CaosScopeResult.NoMoreElements
}

private fun encloses(first: CaosScope, second: CaosScope): Boolean {
    return (first.startOffset < second.endOffset && first.endOffset >= second.endOffset) ||
            (first.startOffset == second.endOffset) && first.endOffset > second.endOffset
}

fun CaosScope?.sharesScope(otherScopeIn: CaosScope?, nullsEqual: Boolean = true) : Boolean {

    if (this == null) {
        return otherScopeIn == null && nullsEqual
    }

    if (otherScopeIn == null) {
        return false
    }

    if (this == otherScopeIn) {
        return true
    }
    val thisEnclosingScopes = enclosingScope.reversed().toMutableList()
    val otherEnclosingScope = otherScopeIn.enclosingScope.reversed().toMutableList()
    for (aScope in thisEnclosingScopes) {

    }
    val firstSharedScope:CaosScope = enclosingScope.lastOrNull { thisScope ->
        otherEnclosingScope.any { otherScope ->
            thisScope.startOffset == otherScope.startOffset && thisScope.endOffset == otherScope.endOffset && thisScope.blockType == otherScope.blockType
        }
    } ?: return false
    val thisSharedScopesStartIndex = thisEnclosingScopes.indexOf(firstSharedScope)

    if (thisSharedScopesStartIndex < 0)
        return false

    val thisSharedScopes = thisEnclosingScopes.subList(thisSharedScopesStartIndex, thisEnclosingScopes.lastIndex)

    val otherSharedScope:CaosScope = otherEnclosingScope.firstOrNull { otherScope ->
        firstSharedScope.startOffset == otherScope.startOffset && firstSharedScope.endOffset == otherScope.endOffset && firstSharedScope.blockType == otherScope.blockType
    } ?: return false

    val otherSharedScopesStartIndex = thisEnclosingScopes.indexOf(otherSharedScope)

    if (otherSharedScopesStartIndex < 0)
        return false

    val otherSharedScopes = otherEnclosingScope.subList(otherSharedScopesStartIndex, otherEnclosingScope.lastIndex)

    val longestScope:List<CaosScope>
    val otherScope:List<CaosScope>
    if (thisSharedScopes.size > otherSharedScopes.size) {
        longestScope = thisSharedScopes
        otherScope = otherSharedScopes
    } else {
        longestScope = otherSharedScopes
        otherScope = thisSharedScopes
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
    return CaosScope(file.containingFilePath, file.textRange, CaosScriptBlockType.MACRO, null)
}

internal val PsiElement.containingFilePath: String? get() {
    return containingFile?.virtualFile?.path
        ?: originalElement.containingFile?.virtualFile?.path
        ?: containingFile?.name
        ?: originalElement?.containingFile?.name
}


fun CaosScriptCompositeElement.sharesScope(otherScope: CaosScope?): Boolean {
    return scope.sharesScope(otherScope)
}

fun CaosScriptCompositeElement.sharesScope(other: CaosScriptCompositeElement): Boolean {
    return scope.sharesScope(other.scope)
}
val CaosScriptCompositeElement.scope: CaosScope?
    get() = (this as? CaosScriptHasCodeBlock ?: this.getParentOfType(CaosScriptHasCodeBlock::class.java))?.scope()

