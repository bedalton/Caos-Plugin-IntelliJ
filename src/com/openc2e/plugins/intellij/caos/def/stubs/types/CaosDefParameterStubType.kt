package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefParameterImpl
import com.openc2e.plugins.intellij.caos.def.psi.util.CaosDefPsiImplUtil
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefParameterStub
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStubImpl
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty

class CaosDefParameterStubType(debugName:String) : CaosDefStubElementType<CaosDefParameterStub, CaosDefParameterImpl>(debugName) {

    override fun createPsi(stub: CaosDefParameterStub): CaosDefParameterImpl {
        return CaosDefParameterImpl(stub, this)
    }

    override fun serialize(stub: CaosDefParameterStub, stream: StubOutputStream) {
        stream.writeName(stub.parameterName)
        stream.writeVariableType(stub.type)
        stream.writeUTFFast(stub.comment ?: "")
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefParameterStub {
        val name = stream.readNameAsString().nullIfEmpty() ?: CaosDefPsiImplUtil.UnknownReturn
        val type = stream.readVariableType() ?: CaosDefPsiImplUtil.AnyTypeType
        val comment = stream.readUTFFast().nullIfEmpty()
        return CaosDefParameterStubImpl (
                parent = parent,
                parameterName = name,
                type = type,
                comment = comment
        )
    }

    override fun createStub(element: CaosDefParameterImpl, parent: StubElement<*>): CaosDefParameterStub {
        return CaosDefParameterStubImpl (
                parent = parent,
                parameterName = element.parameterName,
                type = CaosDefVariableTypeStruct(type = element.parameterType),
                comment = null
        )
    }

    override fun indexStub(p0: CaosDefParameterStub, p1: IndexSink) {
        // ignore, no index
    }
}