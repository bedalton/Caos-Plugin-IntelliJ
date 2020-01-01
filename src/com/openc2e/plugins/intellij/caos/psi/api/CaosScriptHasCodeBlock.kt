package com.openc2e.plugins.intellij.caos.psi.api

interface CaosScriptHasCodeBlock : CaosScriptCompositeElement {
    val codeBlock: CaosScriptCodeBlock?
}