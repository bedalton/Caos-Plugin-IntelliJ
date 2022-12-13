package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types

interface CatalogueStubTypes {
    companion object {

        @JvmStatic
        val TAG_ELEMENT = CatalogueTagStubType("Catalogue_TAG")

        @JvmStatic
        val ARRAY_ELEMENT = CatalogueArrayStubType("Catalogue_ARRAY")

        @JvmStatic
        val FILE: CatalogueFileStubType = CatalogueFileStubType()
    }
}