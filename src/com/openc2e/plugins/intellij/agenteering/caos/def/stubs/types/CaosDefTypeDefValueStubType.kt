package com.openc2e.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefTypeDefinitionImpl
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefTypeDefValueStub
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.TypeDefEq
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosDefTypeDefValueStubType(debugName:String) : CaosDefStubElementType<CaosDefTypeDefValueStub, CaosDefTypeDefinitionImpl>(debugName) {

    override fun createPsi(stub: CaosDefTypeDefValueStub): CaosDefTypeDefinitionImpl {
        return CaosDefTypeDefinitionImpl(stub, this)
    }

    override fun serialize(stub: CaosDefTypeDefValueStub, stream: StubOutputStream) {
        stream.writeName(stub.key)
        stream.writeName(stub.value)
        stream.writeUTFFast(stub.description ?: "")
        val equality = when (stub.equality) {
            TypeDefEq.EQUAL -> 0
            TypeDefEq.GREATER_THAN -> 1
            TypeDefEq.NOT_EQUAL -> 2
        }
        stream.writeInt(equality)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefTypeDefValueStub {
        val key = stream.readNameAsString() ?: CaosDefPsiImplUtil.UnknownReturn
        val value = stream.readNameAsString() ?: CaosDefPsiImplUtil.UnknownReturn
        val description = stream.readUTFFast().nullIfEmpty()
        val equality = when (stream.readInt()) {
            0 -> TypeDefEq.EQUAL
            1 -> TypeDefEq.GREATER_THAN
            2 -> TypeDefEq.NOT_EQUAL
            else -> throw Exception("Invalid equals value encountered")
        }
        return CaosDefTypeDefValueStubImpl(
                parent = parent,
                key = key,
                value = value,
                description = description,
                equality = equality
        )
    }

    override fun createStub(element: CaosDefTypeDefinitionImpl, parent: StubElement<*>): CaosDefTypeDefValueStub {
        var key = element.key
        if (key.length > 1 && (key.startsWith(">") || key.startsWith("!"))) {
            key = key.substring(1)
        }
        return CaosDefTypeDefValueStubImpl(
                parent = parent,
                key = key,
                value = element.value,
                description = element.description,
                equality = element.equality
        )
    }

    override fun indexStub(p0: CaosDefTypeDefValueStub, p1: IndexSink) {
        // ignore
    }

}