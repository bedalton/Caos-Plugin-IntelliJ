package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptBlockType
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.containingFilePath
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.scope
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement {
    val codeBlock: CaosScriptCodeBlock?
    val blockType:CaosScriptBlockType
    val isClosed:Boolean
}


// Get the scope of this block element
fun CaosScriptHasCodeBlock.scope() : CaosScope? {
    val path = containingFilePath
        ?: return null
    val range = textRange
    val blockType = blockType
    val parentScope = parent.getParentOfType(CaosScriptHasCodeBlock::class.java)
        ?.scope
        ?: return CaosScope(path, range, blockType, null)
    return CaosScope(path, range, blockType, parentScope)
}