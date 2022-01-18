package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptSubroutineNameImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptSubroutineNameStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptSubroutineNameStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptSubroutineNameStubType(debugName:String) : CaosScriptStubElementType<CaosScriptSubroutineNameStub, CaosScriptSubroutineNameImpl>(debugName) {
    override fun createPsi(parent: CaosScriptSubroutineNameStub): CaosScriptSubroutineNameImpl {
        return CaosScriptSubroutineNameImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptSubroutineNameStub, stream: StubOutputStream) {
        stream.writeName(stub.tokenText)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptSubroutineNameStub {
        val token = stream.readNameAsString()!!
        return CaosScriptSubroutineNameStubImpl(parent, token)
    }

    override fun createStub(element: CaosScriptSubroutineNameImpl, parent: StubElement<*>?): CaosScriptSubroutineNameStub {
        return CaosScriptSubroutineNameStubImpl(parent, element.name)
    }

}