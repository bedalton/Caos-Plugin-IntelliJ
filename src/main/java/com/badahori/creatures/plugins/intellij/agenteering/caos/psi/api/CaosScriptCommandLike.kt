package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand

/**
 * Represents command like elements.
 * This encompases both commands with arguments and command keyword elements
 */
interface CaosScriptCommandLike : CaosScriptCompositeElement {
    val commandString:String
    @JvmDefault
    val commandToken:CaosScriptIsCommandToken? get() = when (this) {
        is CaosScriptIsCommandToken -> this
        else -> this.getChildOfType(CaosScriptIsCommandToken::class.java)
    }
}