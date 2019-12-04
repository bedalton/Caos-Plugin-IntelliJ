package com.openc2e.plugins.intellij.caos.def.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.stubs.interfaces.CaosDefCommandElementStub
import com.openc2e.plugins.intellij.caos.def.stubs.types.CaosDefStubTypes


class CaosDefCommandElementStubImpl(parent:StubElement<*>?, override val namespace:String?,
                                    override val name:String,
                                    override val parameters:List<CaosDefParameterStruct>,
                                    override val comment:String?
) : StubBase<CaosDefCommandDefElementImpl>(parent, CaosDefStubTypes.COMMAND_ELEMENT), CaosDefCommandElementStub



public data class CaosDefParameterStruct(val name:String, val type:String, val comment:String?);