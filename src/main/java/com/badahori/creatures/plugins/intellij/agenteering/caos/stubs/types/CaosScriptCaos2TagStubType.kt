package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Tag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCaos2TagImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptCaos2TagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptCaos2TagStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptCaos2TagStubType(debugName: String) :
    CaosScriptStubElementType<CaosScriptCaos2TagStub, CaosScriptCaos2TagImpl>(debugName) {

    override fun createPsi(stub: CaosScriptCaos2TagStub): CaosScriptCaos2TagImpl {
        return CaosScriptCaos2TagImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptCaos2TagStub, stream: StubOutputStream) {
        stream.writeName(stub.tagName)
        stream.writeName(stub.rawValue)
        stream.writeName(stub.value)
        stream.writeBoolean(stub.isStringValue)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosScriptCaos2TagStub {
        val tagName = stream.readNameAsString()!!
        val rawValue = stream.readNameAsString()!!
        val value = stream.readNameAsString()
        val isStringValue = stream.readBoolean()
        return CaosScriptCaos2TagStubImpl(
            parent,
            tagName = tagName,
            rawValue = rawValue,
            value = value,
            isStringValue = isStringValue
        )
    }

    override fun createStub(element: CaosScriptCaos2TagImpl, parent: StubElement<*>): CaosScriptCaos2TagStub {
        return CaosScriptCaos2TagStubImpl(
            parent = parent,
            tagName = element.tagName,
            rawValue = element.valueRaw,
            value = element.valueAsString,
            isStringValue = element.isStringValue
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return (node?.psi as? CaosScriptCaos2Tag)?.tagName?.isNotBlank() ?: false
    }

}
