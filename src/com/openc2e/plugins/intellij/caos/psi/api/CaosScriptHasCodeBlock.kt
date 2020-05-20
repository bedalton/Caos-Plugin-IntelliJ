package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosScope
import com.openc2e.plugins.intellij.caos.deducer.CaosScriptBlockType

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement {
    val codeBlock: CaosScriptCodeBlock?
    val blockType:CaosScriptBlockType
}

fun CaosScriptHasCodeBlock.scope() : CaosScope {
    val range = textRange
    val blockType = blockType
    val enclosingScopes = mutableListOf<CaosScope>()
    var parent = getParentOfType(CaosScriptHasCodeBlock::class.java)
    while(parent != null) {
        enclosingScopes.add(parent.scope())
        parent = parent.getParentOfType(CaosScriptHasCodeBlock::class.java)
    }
    return CaosScope(range, blockType, enclosingScopes)
}