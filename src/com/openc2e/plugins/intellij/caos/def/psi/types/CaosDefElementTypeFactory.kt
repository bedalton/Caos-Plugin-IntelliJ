package com.openc2e.plugins.intellij.caos.def.psi.types

import com.intellij.psi.tree.IElementType

object CaosDefElementTypeFactory {

    @JvmStatic
    public fun factory(debugName:String): IElementType {
        return when(debugName) {
            else->throw IndexOutOfBoundsException("Failed to recognize token type: $debugName")
        }
    }
}