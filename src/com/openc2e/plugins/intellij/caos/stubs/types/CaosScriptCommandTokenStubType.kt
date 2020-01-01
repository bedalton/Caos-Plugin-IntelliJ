package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandTokenImpl
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandTokenStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptCommandTokenStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptCommandTokenStubType(debugName:String) : CaosScriptStubElementType<CaosScriptCommandTokenStub, CaosScriptCommandTokenImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCommandTokenStub): CaosScriptCommandTokenImpl {
        return CaosScriptCommandTokenImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCommandTokenStub, stream: StubOutputStream) {
        stream.writeName(stub.text)
        stream.writeInt(stub.index)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCommandTokenStub {
        val text = stream.readNameAsString() ?: "???"
        val index = stream.readInt()
        return CaosScriptCommandTokenStubImpl(
                parent = parent,
                text = text,
                index = index
        )
    }

    override fun createStub(element: CaosScriptCommandTokenImpl, parent: StubElement<*>): CaosScriptCommandTokenStub {
        return CaosScriptCommandTokenStubImpl(
                parent = parent,
                text = element.commandString,
                index = element.index
        )
    }

}
