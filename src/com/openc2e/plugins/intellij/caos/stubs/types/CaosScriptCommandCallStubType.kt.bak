package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandCallImpl
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandCallStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptCommandCallStubImpl
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.caos.utils.readList
import com.openc2e.plugins.intellij.caos.utils.readNameAsString
import com.openc2e.plugins.intellij.caos.utils.writeList

class CaosScriptCommandCallStubType(debugName: String) : CaosScriptStubElementType<CaosScriptCommandCallStub, CaosScriptCommandCallImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCommandCallStub): CaosScriptCommandCallImpl {
        return CaosScriptCommandCallImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCommandCallStub, stream: StubOutputStream) {
        stream.writeList(stub.commandTokens) {
            writeName(it)
        }
        stream.writeList(stub.parameterTypes) {
            writeName(it.value)
        }
        stream.writeInt(stub.numParameters)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCommandCallStub {
        val tokens = stream.readList {
            stream.readNameAsString() ?: "???"
        }
        val parameterTypes = stream.readList {
            val type = stream.readNameAsString().nullIfEmpty()
            if (type != null)
                CaosScriptExpressionType.fromValue(type)
            else
                CaosScriptExpressionType.UNKNOWN
        }
        val numParameters = stream.readInt()
        return CaosScriptCommandCallStubImpl(
                parent = parent,
                commandTokens = tokens,
                numParameters = numParameters,
                parameterTypes = parameterTypes
        )
    }

    override fun createStub(element: CaosScriptCommandCallImpl, parent: StubElement<*>): CaosScriptCommandCallStub {

        return CaosScriptCommandCallStubImpl(
                parent = parent,
                commandTokens = element.commandTokens,
                numParameters = element.parametersLength,
                parameterTypes = element.parameterTypes
        )
    }

}
