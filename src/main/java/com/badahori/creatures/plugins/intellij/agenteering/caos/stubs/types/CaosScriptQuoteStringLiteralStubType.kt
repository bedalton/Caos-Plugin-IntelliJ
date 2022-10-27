package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptQuoteStringLiteralImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptQuoteStringLiteralStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptQuoteStringLiteralStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptQuoteStringLiteralStubType(debugName: String)
    : CaosScriptStubElementType<CaosScriptQuoteStringLiteralStub, CaosScriptQuoteStringLiteralImpl>(debugName) {

    override fun createPsi(stub: CaosScriptQuoteStringLiteralStub): CaosScriptQuoteStringLiteralImpl {
        return CaosScriptQuoteStringLiteralImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptQuoteStringLiteralStub, stream: StubOutputStream) {
        stream.writeName(stub.kind?.toString())
        stream.writeName(stub.value)
        stream.writeInt(stub.meta)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptQuoteStringLiteralStub {
        val kind = stream.readNameAsString().nullIfEmpty()?.let {
            StringStubKind.fromString(it)
        }
        val value = stream.readNameAsString() ?: ""
        val meta = stream.readInt()
        return CaosScriptQuoteStringLiteralStubImpl(
            parent = parent,
            kind = kind,
            value = value,
            meta = meta
        )
    }

    override fun createStub(element: CaosScriptQuoteStringLiteralImpl, parent: StubElement<*>): CaosScriptQuoteStringLiteralStub {
        return CaosScriptQuoteStringLiteralStubImpl(
            parent = parent,
            kind = element.stringStubKind,
            value = try { element.stringValue } catch (e: Exception) { "" },
            meta = element.meta
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        val psi = (node?.psi as? CaosScriptQuoteStringLiteral)
            ?: return false
        return (psi.stringStubKind != null)
    }

    override fun indexStub(stub: CaosScriptQuoteStringLiteralStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexString(stub, indexSink)
    }

}
