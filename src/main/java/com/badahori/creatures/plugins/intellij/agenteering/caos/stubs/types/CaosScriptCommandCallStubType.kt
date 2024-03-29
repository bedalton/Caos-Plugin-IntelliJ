package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCommandCallImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCommandCallStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCommandCallStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptCommandCallStubType(debugName: String) : CaosScriptStubElementType<CaosScriptCommandCallStub, CaosScriptCommandCallImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCommandCallStub): CaosScriptCommandCallImpl {
        return CaosScriptCommandCallImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCommandCallStub, stream: StubOutputStream) {
        stream.writeName(stub.command)
        stream.writeList(stub.argumentValues) {
            writeExpressionValueType(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCommandCallStub {
        val command = stream.readNameAsString() ?: UNDEF
        val arguments = stream.readList {
            CaosExpressionValueType.fromIntValue(readInt())
        }
        return CaosScriptCommandCallStubImpl(
                parent = parent,
                command = command,
                argumentValues = arguments
        )
    }

    override fun createStub(element: CaosScriptCommandCallImpl, parent: StubElement<*>): CaosScriptCommandCallStub {

        return CaosScriptCommandCallStubImpl(
                parent = parent,
                command = element.commandString ?: "",
                argumentValues = element.getArgumentValues(false)
        )
    }

}
