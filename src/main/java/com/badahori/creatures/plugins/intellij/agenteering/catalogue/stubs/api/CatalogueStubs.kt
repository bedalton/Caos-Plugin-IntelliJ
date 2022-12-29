package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueArrayImpl
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueTagImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

interface CatalogueElementStub<T: PsiElement>: StubElement<T> {
    val name: String
    val type: CatalogueItemType
    val itemCount: Int
    val items: List<String>
}

interface CatalogueTagStub : CatalogueElementStub<CatalogueTagImpl> {
    // No extra properties are necessary other than the mains name, kind, values count
}

interface CatalogueArrayStub : CatalogueElementStub<CatalogueArrayImpl> {
    val override: Boolean
    val expectedValuesCount: Int
}

enum class CatalogueItemType(val value: Int) {
    TAG(0),
    ARRAY(1);

    companion object {
        fun fromValue(value: Int): CatalogueItemType {
            return when (value) {
                TAG.value -> TAG
                ARRAY.value -> ARRAY
                else -> throw ArrayIndexOutOfBoundsException("Invalid Catalogue item index passed. Expected 0(Tag), or 1(Array)")
            }
        }
    }
}