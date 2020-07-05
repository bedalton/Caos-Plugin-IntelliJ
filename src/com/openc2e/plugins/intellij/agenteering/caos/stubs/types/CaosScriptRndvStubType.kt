package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCRndvImpl
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRndvStub
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRndvStubImpl

class CaosScriptRndvStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRndvStub, CaosScriptCRndvImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRndvStub): CaosScriptCRndvImpl {
        return CaosScriptCRndvImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRndvStub, stream: StubOutputStream) {
        stream.writeBoolean(stub.min != null)
        stub.min?.let {
            stream.writeInt(it)
        }
        stream.writeBoolean(stub.max != null)
        stub.max?.let {
            stream.writeInt(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRndvStub {
        val min = if (stream.readBoolean())
            stream.readInt()
        else
            null
        val max = if (stream.readBoolean())
            stream.readInt()
        else
            null
        return CaosScriptRndvStubImpl(parent, min, max)
    }

    override fun createStub(element: CaosScriptCRndvImpl, parent: StubElement<*>?): CaosScriptRndvStub {
        val minMax:Pair<Int?, Int?> = element.rndvIntRange
        return CaosScriptRndvStubImpl(parent, minMax.first, minMax.second)
    }

}