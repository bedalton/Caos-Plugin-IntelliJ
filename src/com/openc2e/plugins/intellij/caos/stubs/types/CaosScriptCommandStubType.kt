package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptCommandStubImpl
import com.openc2e.plugins.intellij.caos.utils.readList
import com.openc2e.plugins.intellij.caos.utils.readNameAsString
import com.openc2e.plugins.intellij.caos.utils.writeList

class CaosScriptCommandStubType(debugName:String) : CaosScriptStubElementType<CaosScriptCommandStub, CaosScriptCommandImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCommandStub): CaosScriptCommandImpl {
        return CaosScriptCommandImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCommandStub, stream: StubOutputStream) {
        stream.writeList(stub.commandTokens) {
            writeName(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCommandStub {
        val tokens = stream.readList {
            stream.readNameAsString() ?: "???"
        }
        return CaosScriptCommandStubImpl(
                parent = parent,
                commandTokens = tokens
        )
    }

    override fun createStub(element: CaosScriptCommandImpl, parent: StubElement<*>): CaosScriptCommandStub {

        return CaosScriptCommandStubImpl(
                parent = parent,
                commandTokens = element.commandTokens
        )
    }

}
