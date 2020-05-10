package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCAssignmentImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptVarTokenImpl
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptAssignmentStub
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptVarTokenStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptAssignmentStubImpl
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptVarTokenStubImpl
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptAssignmentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptAssignmentStub, CaosScriptCAssignmentImpl>(debugName) {

    override fun createPsi(stub: CaosScriptAssignmentStub): CaosScriptCAssignmentImpl {
        return CaosScriptCAssignmentImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.varGroup.value)
        stream.writeInt(stub.varIndex ?: -1)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptAssignmentStub {
        val typeString = stream.readNameAsString().nullIfEmpty()
        val group = if (typeString != null)
            CaosScriptVarTokenGroup.fromValue(typeString)
        else
            CaosScriptVarTokenGroup.UNKNOWN
        val varIndex = stream.readInt().let {
            if (it >= 0)
                it
            else
                null
        }
        return CaosScriptAssignmentStub(
                parent = parent,
                varGroup = group,
                varIndex = varIndex
        )
    }

    override fun createStub(element: CaosScriptCAssignmentImpl, parent: StubElement<*>): CaosScriptAssignmentStub {

        return CaosScriptAssignmentStubImpl(
                parent = parent,
                varGroup = element.varGroup,
                varIndex = element.varIndex
        )
    }

}
