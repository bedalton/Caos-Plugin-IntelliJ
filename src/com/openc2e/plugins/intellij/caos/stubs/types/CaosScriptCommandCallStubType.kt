package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandCallImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandCallStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptCommandCallStubImpl

class CaosScriptCommandCallStubType(debugName: String) : CaosScriptStubElementType<CaosScriptCommandCallStub, CaosScriptCommandCallImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCommandCallStub): CaosScriptCommandCallImpl {
        return CaosScriptCommandCallImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCommandCallStub, stream: StubOutputStream) {
        stream.writeStringList(stub.commandTokens)
        stream.writeList(stub.argumentValues) {
            writeCaosVar(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCommandCallStub {
        val tokens = stream.readStringList()
        val arguments = stream.readList {
            readCaosVar()
        }
        return CaosScriptCommandCallStubImpl(
                parent = parent,
                commandTokens = tokens,
                argumentValues = arguments
        )
    }

    override fun createStub(element: CaosScriptCommandCallImpl, parent: StubElement<*>): CaosScriptCommandCallStub {

        return CaosScriptCommandCallStubImpl(
                parent = parent,
                commandTokens = element.commandTokens,
                argumentValues = element.argumentValues
        )
    }

}
