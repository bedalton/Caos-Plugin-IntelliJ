package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.tree.IElementType

public class CaosElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName:String) : IElementType {
            return when (debugName) {
                else -> throw IndexOutOfBoundsException("Caos token '$debugName' is not recognized")
            }
        }
    }
}