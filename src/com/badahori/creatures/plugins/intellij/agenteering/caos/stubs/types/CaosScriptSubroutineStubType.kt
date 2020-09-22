package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptSubroutineImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptSubroutineStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptSubroutineStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString

class CaosScriptSubroutineStubType(debugName:String) : com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType<CaosScriptSubroutineStub, CaosScriptSubroutineImpl>(debugName) {

    override fun createPsi(stub: CaosScriptSubroutineStub): CaosScriptSubroutineImpl {
        return CaosScriptSubroutineImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptSubroutineStub, stream: StubOutputStream) {
        stream.writeName(stub.name)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptSubroutineStub {
        val name = stream.readNameAsString()
                ?: ""
        return CaosScriptSubroutineStubImpl(parent, name)
    }

    override fun createStub(element: CaosScriptSubroutineImpl, parent: StubElement<*>): CaosScriptSubroutineStub {
        return CaosScriptSubroutineStubImpl(
                parent = parent,
                name = element.name
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return super.shouldCreateStub(node) && (node?.psi as? CaosScriptSubroutine)?.name.nullIfEmpty() != null
    }

    override fun indexStub(stub: CaosScriptSubroutineStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexSubroutine(stub, indexSink)
    }

}
