package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvaluePrimeImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRValuePrimeStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRValuePrimeStubImpl

class CaosScriptRValuePrimeStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptRValuePrimeStub, CaosScriptRvaluePrimeImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRValuePrimeStub): CaosScriptRvaluePrimeImpl {
        return CaosScriptRvaluePrimeImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRValuePrimeStub, stream: StubOutputStream) {
        stream.writeCaosVar(stub.caosVar)
        stream.writeList(stub.argumentValues) {
            writeCaosVar(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRValuePrimeStub {
        val selfAsCaosVar = stream.readCaosVar()
        val arguments = stream.readList {
            readCaosVar()
        }
        return CaosScriptRValuePrimeStubImpl(parent, selfAsCaosVar, arguments)
    }

    override fun createStub(element: CaosScriptRvaluePrimeImpl, parent: StubElement<*>?): CaosScriptRValuePrimeStub {
        return CaosScriptRValuePrimeStubImpl(parent, element.toCaosVar(), element.argumentValues)
    }

}