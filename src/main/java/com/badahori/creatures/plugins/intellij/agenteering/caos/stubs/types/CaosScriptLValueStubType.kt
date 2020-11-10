package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptLvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptLValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptLValueStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptLValueStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptLValueStub, CaosScriptLvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptLValueStub): CaosScriptLvalueImpl {
        return CaosScriptLvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptLValueStub, stream: StubOutputStream) {
        stream.writeName(stub.commandString)
        stream.writeExpressionValueType(stub.type)
        stream.writeList(stub.argumentValues) {
            writeExpressionValueType(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptLValueStub {
        val commandString = stream.readNameAsString()
        val caosVar = stream.readExpressionValueType()
        val arguments:List<CaosExpressionValueType> = stream.readList {
            readExpressionValueType()
        }
        return CaosScriptLValueStubImpl(parent, commandString, caosVar, arguments)
    }

    override fun createStub(element: CaosScriptLvalueImpl, parent: StubElement<*>?): CaosScriptLValueStub {
        return CaosScriptLValueStubImpl(parent, element.commandString, element.inferredType, element.argumentValues)
    }

}