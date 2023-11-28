package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCaos2ValueTokenImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCaos2ValueTokenStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCaos2ValueTokenStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptCaos2ValueTokenStubType(debugName: String)
    : CaosScriptStubElementType<CaosScriptCaos2ValueTokenStub, CaosScriptCaos2ValueTokenImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCaos2ValueTokenStub): CaosScriptCaos2ValueTokenImpl {
        return CaosScriptCaos2ValueTokenImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCaos2ValueTokenStub, stream: StubOutputStream) {
        stream.writeName(stub.kind?.toString())
        stream.writeName(stub.value)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCaos2ValueTokenStub {
        val kind = stream.readNameAsString().nullIfEmpty()?.let {
            StringStubKind.fromString(it)
        }
        val value = stream.readNameAsString() ?: ""
        return CaosScriptCaos2ValueTokenStubImpl(
            parent = parent,
            kind = kind,
            value = value
        )
    }

    override fun createStub(element: CaosScriptCaos2ValueTokenImpl, parent: StubElement<*>): CaosScriptCaos2ValueTokenStub {
        return CaosScriptCaos2ValueTokenStubImpl(
            parent = parent,
            kind = element.stringStubKind,
            value = try { element.stringValue } catch (e: Exception) { "" },
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return node?.text?.isNotBlank() == false
    }

    override fun indexStub(stub: CaosScriptCaos2ValueTokenStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexString(stub, indexSink)
    }

}
