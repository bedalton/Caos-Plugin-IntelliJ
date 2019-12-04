package com.openc2e.plugins.intellij.caos.def.stubs.interfaces

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct

public interface CaosDefCommandElementStub : StubElement<CaosDefCommandDefElementImpl> {
    val namespace:String?
    val name:String
    val parameters:List<CaosDefParameterStruct>
    val comment:String?
}