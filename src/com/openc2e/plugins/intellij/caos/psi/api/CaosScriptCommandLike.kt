package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

interface CaosScriptCommandLike : CaosScriptCompositeElement {
    val commandString:String
    @JvmDefault
    val commandToken:CaosScriptIsCommandToken? get() = when (this) {
        is CaosScriptIsCommandToken -> this
        else -> this.getChildOfType(CaosScriptIsCommandToken::class.java)
    }
}