package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.scope
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpressionImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptExpressionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptExpressionStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CaosScriptExpressionStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpressionStub, CaosScriptExpressionImpl>(debugName) {
    override fun createPsi(parent: CaosScriptExpressionStub): CaosScriptExpressionImpl {
        return CaosScriptExpressionImpl(parent, this)
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

    override fun createStub(element: CaosScriptExpressionImpl, parent: StubElement<*>?): CaosScriptExpressionStub {
        return CaosScriptExpressionStubImpl(parent, element.scope, element.toCaosVar())
    }

}