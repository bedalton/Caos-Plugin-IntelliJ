package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptBlockType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement, CaosScriptAssignsTarg {
    val codeBlock: CaosScriptCodeBlock?
    val blockType:CaosScriptBlockType
    val isClosed:Boolean
    val family:Int
    val genus:Int
    val species:Int
}

fun CaosScriptHasCodeBlock.scope() : CaosScope {
    val range = textRange
    val blockType = blockType
    val enclosingScopes = mutableListOf<CaosScope>()
    var parent = getParentOfType(CaosScriptHasCodeBlock::class.java)
    while(parent != null) {
        enclosingScopes.add(parent.scope())
        if (parent.parent is CaosScriptDoifStatement) {
            val parentEnclosingScopes = parent.parent!!.getParentOfType(CaosScriptHasCodeBlock::class.java)?.scope()
                    ?.let { listOf(it) + it.enclosingScope}
                    ?: emptyList()
            enclosingScopes.add(CaosScope(parent.parent.textRange, CaosScriptBlockType.DOIF, parentEnclosingScopes.reversed()))
        }
        parent = parent.getParentOfType(CaosScriptHasCodeBlock::class.java)
    }
    return CaosScope(range, blockType, enclosingScopes.reversed())
}