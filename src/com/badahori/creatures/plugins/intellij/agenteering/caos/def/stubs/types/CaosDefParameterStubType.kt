package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefParameterImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefParameterStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readVariableType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.writeVariableType

class CaosDefParameterStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubElementType<CaosDefParameterStub, CaosDefParameterImpl>(debugName) {

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