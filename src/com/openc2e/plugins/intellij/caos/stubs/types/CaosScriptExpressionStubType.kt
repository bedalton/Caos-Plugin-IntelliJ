package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptExpressionImpl
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptExpressionStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptExpressionStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptExpressionStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpressionStub, CaosScriptExpressionImpl>(debugName) {

    override fun createPsi(stub: CaosScriptExpressionStub): CaosScriptExpressionImpl {
        return CaosScriptExpressionImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpressionStub, stream: StubOutputStream) {
        stream.writeName(stub.type.value)
        stream.writeName(stub.text)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptExpressionStub {
        val type = CaosScriptExpressionType.fromValue(stream.readNameAsString() ?: "")
        val text = stream.readNameAsString() ?: "???"
        return CaosScriptExpressionStubImpl(
                parent = parent,
                type = type,
                text = text
        )
    }

    override fun createStub(element: CaosScriptExpressionImpl, parent: StubElement<*>): CaosScriptExpressionStub {
        return CaosScriptExpressionStubImpl(
                parent = parent,
                type = element.type,
                text = element.text
        )
    }

}
