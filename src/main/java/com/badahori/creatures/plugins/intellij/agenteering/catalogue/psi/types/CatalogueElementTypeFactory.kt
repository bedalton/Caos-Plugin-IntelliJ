package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.types

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types.CatalogueStubTypes
import com.intellij.psi.tree.IElementType

object CatalogueElementTypeFactory {

    @JvmStatic
    fun factory(debugName:String): IElementType {
        return when(debugName) {
            "CATALOGUE_TAG" -> CatalogueStubTypes.TAG_ELEMENT
            "CATALOGUE_ARRAY" -> CatalogueStubTypes.ARRAY_ELEMENT
            else->throw IndexOutOfBoundsException("Failed to recognize token type: $debugName")
        }
    }
}