package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefValuesListValueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefValuesListValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.ValuesListEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosDefValuesListValueStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubElementType<CaosDefValuesListValueStub, CaosDefValuesListValueImpl>(debugName) {

    override fun createPsi(stub: CaosDefValuesListValueStub): CaosDefValuesListValueImpl {
        return CaosDefValuesListValueImpl(stub, this)
    }

    override fun serialize(stub: CaosDefValuesListValueStub, stream: StubOutputStream) {
        stream.writeName(stub.key)
        stream.writeName(stub.value)
        stream.writeUTFFast(stub.description ?: "")
        val equality = when (stub.equality) {
            ValuesListEq.EQUAL -> 0
            ValuesListEq.GREATER_THAN -> 1
            ValuesListEq.NOT_EQUAL -> 2
        }
        stream.writeInt(equality)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefValuesListValueStub {
        val key = stream.readNameAsString() ?: CaosDefPsiImplUtil.UnknownReturn
        val value = stream.readNameAsString() ?: CaosDefPsiImplUtil.UnknownReturn
        val description = stream.readUTFFast().nullIfEmpty()
        val equality = when (stream.readInt()) {
            0 -> ValuesListEq.EQUAL
            1 -> ValuesListEq.GREATER_THAN
            2 -> ValuesListEq.NOT_EQUAL
            else -> throw Exception("Invalid equals value encountered")
        }
        return CaosDefValuesListValueStubImpl(
                parent = parent,
                key = key,
                value = value,
                description = description,
                equality = equality
        )
    }

    override fun createStub(element: CaosDefValuesListValueImpl, parent: StubElement<*>): CaosDefValuesListValueStub {
        var key = element.key
        if (key.length > 1 && (key.startsWith(">") || key.startsWith("!"))) {
            key = key.substring(1)
        }
        return CaosDefValuesListValueStubImpl(
                parent = parent,
                key = key,
                value = element.value,
                description = element.description,
                equality = element.equality
        )
    }

    override fun indexStub(p0: CaosDefValuesListValueStub, p1: IndexSink) {
        // ignore
    }

}