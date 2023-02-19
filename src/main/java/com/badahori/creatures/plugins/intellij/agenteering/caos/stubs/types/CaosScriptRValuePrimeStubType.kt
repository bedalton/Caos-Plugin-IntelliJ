package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvaluePrimeImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRValuePrimeStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRValuePrimeStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptRValuePrimeStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRValuePrimeStub, CaosScriptRvaluePrimeImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRValuePrimeStub): CaosScriptRvaluePrimeImpl {
        return CaosScriptRvaluePrimeImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRValuePrimeStub, stream: StubOutputStream) {
        stream.writeName(stub.commandString)
        stream.writeExpressionValueType(stub.caosVar)
        stream.writeList(stub.argumentValues) {
            writeExpressionValueType(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRValuePrimeStub {
        val commandString = stream.readNameAsString()
        val returnType = stream.readExpressionValueType()
        val arguments = stream.readList {
            readExpressionValueType()
        }
        return CaosScriptRValuePrimeStubImpl(parent, commandString, returnType, arguments)
    }

    override fun createStub(element: CaosScriptRvaluePrimeImpl, parent: StubElement<*>?): CaosScriptRValuePrimeStub {
        return CaosScriptRValuePrimeStubImpl(parent, element.commandString, element.inferredType.firstOrNull() ?: CaosExpressionValueType.UNKNOWN, element.argumentValues)
    }

}