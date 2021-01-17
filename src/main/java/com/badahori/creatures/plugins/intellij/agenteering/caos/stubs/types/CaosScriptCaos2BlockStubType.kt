package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCaos2BlockImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCaos2BlockStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCaos2BlockStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptCaos2BlockStubType(debugName:String) : CaosScriptStubElementType<CaosScriptCaos2BlockStub, CaosScriptCaos2BlockImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCaos2BlockStub): CaosScriptCaos2BlockImpl {
        return CaosScriptCaos2BlockImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCaos2BlockStub, stream: StubOutputStream) {
        stream.writeBoolean(stub.isCaos2Pray)
        stream.writeBoolean(stub.isCaos2Cob)
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
        val commands = stream.readList {
            val command = stream.readNameAsString()
                ?: return@readList null
            val args = stream.readStringList()
            Pair(command, args)
        }
        val tags:Map<String,String> = stream.readList list@{
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
            commands = commands,
            tags = tags
        )
    }

    override fun createStub(element: CaosScriptCaos2BlockImpl, parent: StubElement<*>): CaosScriptCaos2BlockStub {
        return CaosScriptCaos2BlockStubImpl(
                parent = parent,
                isCaos2Pray = element.isCaos2Pray,
                isCaos2Cob = element.isCaos2Cob,
                tags = element.tags,
                commands = element.commands
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return true
    }

}
