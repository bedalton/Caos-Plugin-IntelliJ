package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCAssignmentImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptAssignmentStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptAssignmentStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptAssignmentStubType(debugName: String) :
    com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptAssignmentStub, CaosScriptCAssignmentImpl>(
        debugName
    ) {

    override fun createPsi(stub: CaosScriptAssignmentStub): CaosScriptCAssignmentImpl {
        return CaosScriptCAssignmentImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.fileName)
        stream.writeInt(stub.operation.value)
        stream.writeCaosVarSafe(stub.lvalue)
        val rvalue = stub.rvalue
        stream.writeBoolean(rvalue != null)
        if (rvalue != null) {
            stream.writeList(rvalue) {
                writeCaosVarSafe(it)
            }
        }
//        stream.writeScope(stub.enclosingScope)
        stream.writeName(stub.commandString)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptAssignmentStub {
        val fileName = stream.readNameAsString() ?: UNDEF
        val operation = CaosOp.fromValue(stream.readInt())
        val lvalue = stream.readCaosVarSafe()
        val rvalue = if (stream.readBoolean()) {
            stream.readList {
                readCaosVarSafe()
            }
        } else emptyList()
//        val enclosingScope = stream.readScope()
        val commandString = stream.readNameAsString() ?: UNDEF
        return CaosScriptAssignmentStubImpl(
            parent = parent,
            fileName = fileName,
            operation = operation,
            lvalue = lvalue,
            rvalue = rvalue,
//            enclosingScope = enclosingScope,
            commandString = commandString
        )
    }

    override fun createStub(element: CaosScriptCAssignmentImpl, parent: StubElement<*>): CaosScriptAssignmentStub {
        val bias = when (element.commandStringUpper) {
            "SETS", "NET: UNIK" -> CaosExpressionValueType.STRING
            "SETA" -> CaosExpressionValueType.AGENT
            "ANDV", "ORRV" -> CaosExpressionValueType.INT
            else -> CaosExpressionValueType.DECIMAL
        }
        return CaosScriptAssignmentStubImpl(
            parent = parent,
            fileName = element.containingFile.name,
            operation = element.op,
            lvalue = CaosExpressionValueType.VARIABLE,
            rvalue = element.rvalue?.getInferredType(bias, false),
            //enclosingScope = CaosScriptPsiImplUtil.getScope(element),
            commandString = element.commandString
        )
    }

    override fun indexStub(stub: CaosScriptAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexVarAssignment(stub, indexSink)
    }

}
