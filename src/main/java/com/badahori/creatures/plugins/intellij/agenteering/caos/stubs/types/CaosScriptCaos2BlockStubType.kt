package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCaos2BlockImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCaos2BlockStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCaos2BlockStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptCaos2BlockStubType(debugName: String) :
    CaosScriptStubElementType<CaosScriptCaos2BlockStub, CaosScriptCaos2BlockImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCaos2BlockStub): CaosScriptCaos2BlockImpl {
        return CaosScriptCaos2BlockImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCaos2BlockStub, stream: StubOutputStream) {
        stream.writeBoolean(stub.isCaos2Pray)
        stream.writeBoolean(stub.isCaos2Cob)
        stream.writeStringList(stub.caos2Variants.map { it.code })
        stream.writeList(stub.agentBlockNames) {
            stream.writeName(it.first)
            stream.writeName(it.second)
        }
        stream.writeList(stub.commands) { (command, args) ->
            stream.writeName(command)
            stream.writeStringList(args)
        }
        stream.writeList(stub.tags.entries) { (key, value) ->
            stream.writeName(key)
            stream.writeName(value)
        }
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCaos2BlockStub {
        val isCaos2Pray = stream.readBoolean()
        val isCaos2Cob = stream.readBoolean()
        val caos2Variants = stream.readStringList().mapNotNull { variantCode ->
            val variant = CaosVariant.fromVal(variantCode)
            if (variant == CaosVariant.UNKNOWN)
                null
            else
                variant
        }

        val agentBlockNames = stream.readList read@{
            val name = readNameAsString()
                ?: return@read null
            val value = readNameAsString()
                ?: return@read null
            name to value
        }
        val commands = stream.readList {
            val command = stream.readNameAsString()
                ?: return@readList null
            val args = stream.readStringList()
            Pair(command, args)
        }
        val tags: Map<String, String> = stream.readList list@{
            readNameAsString()?.let { tag ->
                readNameAsString()?.let { value ->
                    return@list tag to value
                }
            }
        }.toMap()
        return CaosScriptCaos2BlockStubImpl(
            parent,
            isCaos2Pray = isCaos2Pray,
            isCaos2Cob = isCaos2Cob,
            agentBlockNames = agentBlockNames,
            commands = commands,
            tags = tags,
            caos2Variants = caos2Variants
        )
    }

    override fun createStub(element: CaosScriptCaos2BlockImpl, parent: StubElement<*>): CaosScriptCaos2BlockStub {
        return CaosScriptCaos2BlockStubImpl(
            parent = parent,
            isCaos2Pray = element.isCaos2Pray,
            isCaos2Cob = element.isCaos2Cob,
            agentBlockNames = element.agentBlockNames,
            tags = element.tags,
            commands = element.commands,
            caos2Variants = element.caos2Variants
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return true
    }

}
