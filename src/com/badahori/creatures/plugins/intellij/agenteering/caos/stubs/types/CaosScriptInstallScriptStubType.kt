package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptInstallScriptImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptInstallScriptStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptInstallScriptStubImpl

class CaosScriptInstallScriptStubType(debugName:String) : CaosScriptStubElementType<CaosScriptInstallScriptStub, CaosScriptInstallScriptImpl>(debugName) {
    override fun createPsi(parent: CaosScriptInstallScriptStub): CaosScriptInstallScriptImpl {
        return CaosScriptInstallScriptImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptInstallScriptStub, stream: StubOutputStream) {
        // nothing to serialize
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptInstallScriptStub {
        return CaosScriptInstallScriptStubImpl(parent = parent)
    }

    override fun createStub(element: CaosScriptInstallScriptImpl, parent: StubElement<*>?): CaosScriptInstallScriptStub {
        return CaosScriptInstallScriptStubImpl(parent)
    }

}