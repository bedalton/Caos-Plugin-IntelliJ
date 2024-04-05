package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefStubIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefCommandDefinitionStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readSimpleType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

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
        stream.writeInt(stub.simpleReturnType.value)
        stream.writeBoolean(stub.requiresOwner)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefCommandDefinitionStub {
        val variants = stream.readList { readNameAsString() }.filterNotNull().map { CaosVariant.fromVal(it) }
        val namespace = stream.readNameAsString()
        val command = stream.readNameAsString()
        val parameters = stream.readList(StubInputStream::readParameter)
        val isCommand = stream.readBoolean()
        val rvalue = stream.readBoolean()
        val lvalue = stream.readBoolean()
        val returnType = stream.readReturnType() ?: CaosDefReturnTypeStruct(
                type = CaosDefPsiImplUtil.AnyTypeType,
                comment = null
        )
        val comment = stream.readUTFFast().nullIfEmpty()
        val simpleReturnType = stream.readSimpleType()
        val requiresOwner = stream.readBoolean()
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
                variants = variants,
                simpleReturnType = simpleReturnType,
                requiresOwner = requiresOwner
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
                variants = element.variants,
                simpleReturnType = element.simpleReturnType,
                requiresOwner = element.requiresOwner
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as CaosDefCommandDefElement).commandName.nullIfEmpty() != null
    }

    override fun indexStub(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        CaosDefStubIndexService.indexCommand(stub, indexSink)
    }
}