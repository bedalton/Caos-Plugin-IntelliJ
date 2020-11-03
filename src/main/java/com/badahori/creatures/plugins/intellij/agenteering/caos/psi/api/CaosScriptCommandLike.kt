package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand

interface CaosScriptCommandLike : CaosScriptCompositeElement {
    val commandString:String
    val commandDefinition: CaosCommand?
    @JvmDefault
    val commandToken:CaosScriptIsCommandToken? get() = when (this) {
        is CaosScriptIsCommandToken -> this
        else -> this.getChildOfType(CaosScriptIsCommandToken::class.java)
    }
}