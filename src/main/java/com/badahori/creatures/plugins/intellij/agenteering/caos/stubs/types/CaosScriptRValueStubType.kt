package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRValueStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptRValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRValueStub, CaosScriptRvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRValueStub): CaosScriptRvalueImpl {
        return CaosScriptRvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRValueStub, stream: StubOutputStream) {
        stream.writeName(stub.commandString)
        stream.writeExpressionValueType(stub.type)
        stream.writeList(stub.argumentValues) {
            writeExpressionValueType(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRValueStub {
        val commandString = stream.readNameAsString()
        val returnType = stream.readExpressionValueType()
        val arguments = stream.readList {
            readExpressionValueType()
        }
        return CaosScriptRValueStubImpl(parent, commandString, returnType, arguments)
    }

    override fun createStub(element: CaosScriptRvalueImpl, parent: StubElement<*>?): CaosScriptRValueStub {
        return CaosScriptRValueStubImpl(parent, element.commandString, element.inferredType, element.argumentValues)
    }

}