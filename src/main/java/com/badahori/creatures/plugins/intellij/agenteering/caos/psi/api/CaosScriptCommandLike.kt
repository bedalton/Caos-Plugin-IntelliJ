package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiImplUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

/**
 * Represents command like elements.
 * This encompasses both commands with arguments and command keyword elements
 */
interface CaosScriptCommandLike : CaosScriptCompositeElement {
    val commandString:String?

    @JvmDefault
    val commandTokenElement:CaosScriptIsCommandToken? get() = when (this) {
        is CaosScriptIsCommandToken -> this
        else -> this.getChildOfType(CaosScriptIsCommandToken::class.java)
    }

    @JvmDefault
    val commandTokenElementType: IElementType? get() = when (this) {
        is CaosScriptIsCommandToken -> CaosScriptPsiImplUtil.getCommandTokenElementType(this)
        else -> CaosScriptPsiImplUtil.getCommandTokenElementType(this.getChildOfType(CaosScriptIsCommandToken::class.java))
    }
}

interface CaosScriptCommandCallLike : CaosScriptCompositeElement


val CaosScriptCommandLike.commandToken2ElementType: IElementType? get() = when (this) {
    is CaosScriptIsCommandToken -> getCommandToken2ElementType(this)
    else -> getCommandToken2ElementType(this.getChildOfType(CaosScriptIsCommandToken::class.java))
}

private fun getCommandToken2ElementType(token: CaosScriptIsCommandToken?): IElementType? {
    val temp = token?.children?.getOrNull(1)
        ?: return null
    return temp.firstChild?.firstChild?.firstChild?.elementType
        ?: temp.firstChild?.firstChild?.elementType
        ?: temp.firstChild?.elementType
        ?: temp.elementType
}