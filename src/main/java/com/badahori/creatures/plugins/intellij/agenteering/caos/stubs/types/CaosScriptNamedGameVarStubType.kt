package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptIndexService
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedGameVarImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptNamedGameVarStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptNamedGameVarStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.ASTNode
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink

class CaosScriptNamedGameVarStubType(debugName:String) : CaosScriptStubElementType<CaosScriptNamedGameVarStub, CaosScriptNamedGameVarImpl>(debugName) {
    override fun createPsi(parent: CaosScriptNamedGameVarStub): CaosScriptNamedGameVarImpl {
        return CaosScriptNamedGameVarImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptNamedGameVarStub, stream: StubOutputStream) {
        stream.writeInt(stub.type.value)
        stream.writeName(stub.key)
        val keyType = stub.keyType
        stream.writeBoolean(keyType != null)
        if (keyType != null) {
            stream.writeList(keyType) {
                writeCaosVarSafe(it)
            }
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        val type = CaosScriptNamedGameVarType.fromValue(stream.readInt())
        val name = stream.readNameAsString() ?: UNDEF

        val hasKeyType = stream.readBoolean()
        val keyType = if (hasKeyType) {
            stream.readList {
                readCaosVarSafe()
            }
        } else {
            null
        }
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = type,
                key = name,
                keyType = keyType
        )
    }

    override fun createStub(element: CaosScriptNamedGameVarImpl, parent: StubElement<*>?): CaosScriptNamedGameVarStub {
        return CaosScriptNamedGameVarStubImpl(
                parent = parent,
                type = element.varType,
                key = element.name,
                keyType = element.keyType
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        val text = try {
            node?.text
        } catch (e: Exception) {
            null
        } ?: return false
        return text.length > 2 && text[0] == '"' && text.last() == '"'
    }

    override fun indexStub(stub: CaosScriptNamedGameVarStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosScriptIndexService::class.java).indexNamedGameVar(stub, indexSink)
    }
}