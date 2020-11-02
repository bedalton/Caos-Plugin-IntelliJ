package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRemovalScriptImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRemovalScriptStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRemovalScriptStubImpl

class CaosScriptRemovalScriptStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRemovalScriptStub, CaosScriptRemovalScriptImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRemovalScriptStub): CaosScriptRemovalScriptImpl {
        return CaosScriptRemovalScriptImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRemovalScriptStub, stream: StubOutputStream) {
        // nothing to serialize
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRemovalScriptStub {
        return CaosScriptRemovalScriptStubImpl(parent = parent)
    }

    override fun createStub(element: CaosScriptRemovalScriptImpl, parent: StubElement<*>?): CaosScriptRemovalScriptStub {
        return CaosScriptRemovalScriptStubImpl(parent)
    }

}