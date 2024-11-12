package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptQuoteStringLiteralImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptQuoteStringLiteralStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptQuoteStringLiteralStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptQuoteStringLiteralStubType(debugName: String) :
    CaosScriptStubElementType<CaosScriptQuoteStringLiteralStub, CaosScriptQuoteStringLiteralImpl>(debugName) {

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

    override fun createStub(
        element: CaosScriptQuoteStringLiteralImpl,
        parent: StubElement<*>,
    ): CaosScriptQuoteStringLiteralStub {
        return CaosScriptQuoteStringLiteralStubImpl(
            parent = parent,
            kind = element.stringStubKind,
            value = try {
                element.stringValue
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                ""
            },
            meta = element.meta
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        val psi = (node?.psi as? CaosScriptQuoteStringLiteral)
            ?: return false

        if (psi.stringStubKind == null) {
            return false
        }
        val text = try {
            node.text
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            null
        } ?: return false
        return text.length > 2 && text[0] == '"' && text.last() == '"'
    }

    override fun indexStub(stub: CaosScriptQuoteStringLiteralStub, indexSink: IndexSink) {
        CaosScriptIndexService.indexString(stub, indexSink)
    }

}
