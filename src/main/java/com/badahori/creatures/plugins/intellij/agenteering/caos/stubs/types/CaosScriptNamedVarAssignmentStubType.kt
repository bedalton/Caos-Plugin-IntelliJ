package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedVarAssignmentImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptNamedVarAssignmentStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptNamedVarAssignmentStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptNamedVarAssignmentStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptNamedVarAssignmentStub, CaosScriptNamedVarAssignmentImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedVarAssignmentStub): CaosScriptNamedVarAssignmentImpl {
        return CaosScriptNamedVarAssignmentImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedVarAssignmentStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
        stream.writeCaosVarSafe(stub.value)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedVarAssignmentStub {
        val name = stream.readNameAsString() ?: UNDEF
        val value = stream.readCaosVarSafe()
        return CaosScriptNamedVarAssignmentStubImpl(
                parent = parent,
                name = name,
                value = value
        )
    }

    override fun createStub(element: CaosScriptNamedVarAssignmentImpl, parent: StubElement<*>?): CaosScriptNamedVarAssignmentStub {
        return CaosScriptNamedVarAssignmentStubImpl(
                parent = parent,
                name = element.name,
                value = element.value
        )
    }

    override fun indexStub(stub: CaosScriptNamedVarAssignmentStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexNamedVarAssignment(stub, indexSink)
    }

}