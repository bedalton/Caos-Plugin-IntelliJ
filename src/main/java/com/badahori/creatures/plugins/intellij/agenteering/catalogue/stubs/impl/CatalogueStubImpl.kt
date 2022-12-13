package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.impl

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueArrayImpl
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl.CatalogueTagImpl
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueArrayStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueItemType
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueTagStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types.CatalogueStubTypes
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement


class CatalogueTagStubImpl(
    parent: StubElement<*>,
    override val name: String,
    override val type: CatalogueItemType,
    override val itemCount: Int
) : StubBase<CatalogueTagImpl>(parent, CatalogueStubTypes.TAG_ELEMENT), CatalogueTagStub

class CatalogueArrayStubImpl(
    parent: StubElement<*>,
    override val name: String,
    override val type: CatalogueItemType,
    override val itemCount: Int,
    override val override: Boolean,
    override val expectedValuesCount: Int
) : StubBase<CatalogueArrayImpl>(parent, CatalogueStubTypes.ARRAY_ELEMENT), CatalogueArrayStub