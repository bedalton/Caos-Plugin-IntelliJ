package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefDocCommentImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefDocCommentStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty

class CaosDefCommentElementStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubElementType<CaosDefDocCommentStub, CaosDefDocCommentImpl>(debugName) {

    override fun createPsi(stub: CaosDefDocCommentStub): CaosDefDocCommentImpl {
        return CaosDefDocCommentImpl(stub, this)
    }

    override fun serialize(stub: CaosDefDocCommentStub, stream: StubOutputStream) {
        stream.writeList(stub.parameters, StubOutputStream::writeParameter)
        stream.writeBoolean(stub.rvalue)
        stream.writeBoolean(stub.lvalue)
        stream.writeReturnType(stub.returnType)
        stream.writeUTFFast(stub.comment ?: "")
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefDocCommentStub {
        val parameters = stream.readList(StubInputStream::readParameter).filterNotNull()
        val rvalue = stream.readBoolean()
        val lvalue = stream.readBoolean()
        val returnType = stream.readReturnType() ?: CaosDefReturnTypeStruct(
                type = CaosDefPsiImplUtil.AnyTypeType,
                comment = null
        )
        val comment = stream.readUTFFast().nullIfEmpty()
        return CaosDefDocCommentStubImpl(
                parent = parent,
                parameters = parameters,
                rvalue = rvalue,
                lvalue = lvalue,
                returnType = returnType,
                comment = comment
        )
    }

    override fun createStub(element: CaosDefDocCommentImpl, parent: StubElement<*>): CaosDefDocCommentStub {
        return CaosDefDocCommentStubImpl(
                parent = parent,
                parameters = element.parameterStructs,
                rvalue = element.rvalueList.isNotEmpty(),
                lvalue = element.lvalueList.isNotEmpty(),
                returnType = element.returnTypeStruct ?: CaosDefPsiImplUtil.UnknownReturnType,
                comment = element.docCommentFrontComment?.text
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return false
    }

    override fun indexStub(stub: CaosDefDocCommentStub, indexSink: IndexSink) {
        // no index needed
    }
}