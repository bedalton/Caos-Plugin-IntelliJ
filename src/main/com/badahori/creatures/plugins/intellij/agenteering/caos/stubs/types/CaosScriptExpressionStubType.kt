package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptLiteralImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptExpressionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptExpressionStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptExpressionStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpressionStub, CaosScriptLiteralImpl>(debugName) {
    override fun createPsi(parent: CaosScriptExpressionStub): CaosScriptLiteralImpl {
        return CaosScriptLiteralImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptExpressionStub, stream: StubOutputStream) {
        stream.writeBoolean(stub.enclosingScope != null)
        if (stub.enclosingScope != null)
            stream.writeScope(stub.enclosingScope!!)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpressionStub {
        val enclosingScope = if (stream.readBoolean()) stream.readScope() else null
        val caosVar = stream.readCaosVar()
        return CaosScriptExpressionStubImpl(parent, enclosingScope, caosVar)
    }

    override fun createStub(element: CaosScriptLiteralImpl, parent: StubElement<*>?): CaosScriptExpressionStub {
        return CaosScriptExpressionStubImpl(parent, element.scope, element.toCaosVar())
    }

}