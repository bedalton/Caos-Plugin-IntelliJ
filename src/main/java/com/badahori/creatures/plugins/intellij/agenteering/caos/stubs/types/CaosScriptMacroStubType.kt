package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptMacroImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptMacroStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptMacroStubImpl

class CaosScriptMacroStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptMacroStub, CaosScriptMacroImpl>(debugName) {
    override fun createPsi(parent: CaosScriptMacroStub): CaosScriptMacroImpl {
        return CaosScriptMacroImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptMacroStub, stream: StubOutputStream) {
        // nothing to serialize
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptMacroStub {
        return CaosScriptMacroStubImpl(parent = parent)
    }

    override fun createStub(element: CaosScriptMacroImpl, parent: StubElement<*>?): CaosScriptMacroStub {
        return CaosScriptMacroStubImpl(parent)
    }

}