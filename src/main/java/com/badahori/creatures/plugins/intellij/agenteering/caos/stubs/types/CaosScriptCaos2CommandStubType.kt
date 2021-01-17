package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Command
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCaos2CommandImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCaos2CommandStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCaos2CommandStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptCaos2CommandStubType(debugName: String) :
    CaosScriptStubElementType<CaosScriptCaos2CommandStub, CaosScriptCaos2CommandImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCaos2CommandStub): CaosScriptCaos2CommandImpl {
        return CaosScriptCaos2CommandImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCaos2CommandStub, stream: StubOutputStream) {
        stream.writeName(stub.commandName)
        stream.writeStringList(stub.args)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCaos2CommandStub {
        val commandName = stream.readNameAsString()!!
        val args = stream.readStringList()
        return CaosScriptCaos2CommandStubImpl(
            parent,
            commandName = commandName,
            args = args
        )
    }

    override fun createStub(element: CaosScriptCaos2CommandImpl, parent: StubElement<*>): CaosScriptCaos2CommandStub {
        return CaosScriptCaos2CommandStubImpl(
            parent = parent,
            commandName = element.commandName,
            args = element.commandArgs
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as? CaosScriptCaos2Command)?.commandName?.isNotBlank() ?: false
    }

}
