package com.openc2e.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.def.indices.CaosDefStubIndexService
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefCommandDefinitionStubImpl
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.utils.*

class CaosDefCommandElementStubType(debugName:String) : CaosDefStubElementType<CaosDefCommandDefinitionStub, CaosDefCommandDefElementImpl>(debugName) {

    override fun createPsi(stub: CaosDefCommandDefinitionStub): CaosDefCommandDefElementImpl {
        return CaosDefCommandDefElementImpl(stub, this)
    }

    override fun serialize(stub: CaosDefCommandDefinitionStub, stream: StubOutputStream) {
        stream.writeList(stub.variants) { writeName(it.code) }
        stream.writeName(stub.namespace)
        stream.writeName(stub.command)
        stream.writeList(stub.parameters, StubOutputStream::writeParameter)
        stream.writeBoolean(stub.isCommand)
        stream.writeBoolean(stub.rvalue)
        stream.writeBoolean(stub.lvalue)
        stream.writeReturnType(stub.returnType)
        stream.writeUTFFast(stub.comment ?: "")
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefCommandDefinitionStub {
        val variants = stream.readList { readNameAsString() }.filterNotNull().map { CaosVariant.fromVal(it) }
        val namespace = stream.readNameAsString()
        val command = stream.readNameAsString()
        val parameters = stream.readList(StubInputStream::readParameter).filterNotNull()
        val isCommand = stream.readBoolean()
        val rvalue = stream.readBoolean()
        val lvalue = stream.readBoolean()
        val returnType = stream.readReturnType() ?: CaosDefReturnTypeStruct(
                type = CaosDefPsiImplUtil.AnyTypeType,
                comment = null
        )
        val comment = stream.readUTFFast().nullIfEmpty()
        return CaosDefCommandDefinitionStubImpl(
                parent = parent,
                namespace = namespace,
                command = command!!,
                parameters = parameters,
                isCommand = isCommand,
                rvalue = rvalue,
                lvalue = lvalue,
                returnType = returnType,
                comment = comment,
                variants = variants
        )
    }

    override fun createStub(element: CaosDefCommandDefElementImpl, parent: StubElement<*>): CaosDefCommandDefinitionStub {
        return CaosDefCommandDefinitionStubImpl(
                parent = parent,
                namespace = element.namespace,
                command = element.commandName,
                parameters = element.parameterStructs,
                isCommand = element.isCommand,
                rvalue = element.isRvalue,
                lvalue = element.isLvalue,
                returnType = element.returnTypeStruct ?: CaosDefPsiImplUtil.UnknownReturnType,
                comment = element.comment,
                variants = element.variants
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as CaosDefCommandDefElement).commandName.nullIfEmpty() != null
    }

    override fun indexStub(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosDefStubIndexService::class.java).indexCommand(stub, indexSink)
    }
}