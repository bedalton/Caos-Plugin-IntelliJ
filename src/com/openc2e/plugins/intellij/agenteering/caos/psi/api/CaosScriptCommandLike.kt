package com.openc2e.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptCommandLike : CaosScriptCompositeElement {
    val commandString:String
    @JvmDefault
    val commandToken:CaosScriptIsCommandToken? get() = when (this) {
        is CaosScriptIsCommandToken -> this
        else -> this.getChildOfType(CaosScriptIsCommandToken::class.java)
    }
}