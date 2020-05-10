package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosScriptBlockType

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement {
    val codeBlock: CaosScriptCodeBlock?
    val blockType:CaosScriptBlockType
}