package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptBlockType
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.scope
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement {
    val codeBlock: CaosScriptCodeBlock?
    val blockType:CaosScriptBlockType
    val isClosed:Boolean
}

fun CaosScriptHasCodeBlock.scope() : CaosScope {
    val range = textRange
    val blockType = blockType
    val parent = getParentOfType(CaosScriptHasCodeBlock::class.java)
    val parentScope = parent?.scope
//    while(parent != null) {
//        enclosingScopes.add(parent.scope())
//        if (parent.parent is CaosScriptDoifStatement) {
//            val parentEnclosingScopes = parent.parent!!.getParentOfType(CaosScriptHasCodeBlock::class.java)?.scope()
//                    ?.let { listOf(it) + it.enclosingScope}
//                    ?: emptyList()
//            enclosingScopes.add(CaosScope(parent.parent.textRange, CaosScriptBlockType.DOIF, parentEnclosingScopes.reversed()))
//        }
//        parent = parent.getParentOfType(CaosScriptHasCodeBlock::class.java)
//    }
    return CaosScope(range, blockType, parentScope)
}