package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptLvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptLValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptLValueStubImpl

class CaosScriptLValueStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptLValueStub, CaosScriptLvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptLValueStub): CaosScriptLvalueImpl {
        return CaosScriptLvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptLValueStub, stream: StubOutputStream) {
        stream.writeCaosVar(stub.caosVar)
        stream.writeList(stub.argumentValues) {
            writeCaosVar(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptLValueStub {
        val caosVar = stream.readCaosVar()
        val arguments:List<CaosVar> = stream.readList {
            readCaosVar()
        }
        return CaosScriptLValueStubImpl(parent, caosVar, arguments)
    }

    override fun createStub(element: CaosScriptLvalueImpl, parent: StubElement<*>?): CaosScriptLValueStub {
        return CaosScriptLValueStubImpl(parent, element.toCaosVar(), element.argumentValues)
    }

}