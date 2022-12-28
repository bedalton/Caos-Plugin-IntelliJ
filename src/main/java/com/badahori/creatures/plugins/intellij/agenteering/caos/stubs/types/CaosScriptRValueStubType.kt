package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalueLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptTokenRvalue
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvalueImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptRValueStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptRValueStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.ASTNode

class CaosScriptRValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptRValueStub, CaosScriptRvalueImpl>(debugName) {
    override fun createPsi(parent: CaosScriptRValueStub): CaosScriptRvalueImpl {
        return CaosScriptRvalueImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptRValueStub, stream: StubOutputStream) {
        stream.writeName(stub.commandString)
        val types = stub.type
        stream.writeList(types) {
            stream.writeExpressionValueType(it)
        }
        stream.writeList(stub.argumentValues) {
            writeExpressionValueType(it)
        }
        stream.writeName(stub.stringStubKind?.name)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptRValueStub {
        val commandString = stream.readNameAsString()
        val returnType = stream.readList {
            stream.readExpressionValueType()
        }
        val arguments = stream.readList {
            readExpressionValueType()
        }
        val stringStubKind = StringStubKind.fromString(stream.readNameAsString())
        return CaosScriptRValueStubImpl(parent, commandString, returnType, arguments, stringStubKind)
    }

    override fun createStub(element: CaosScriptRvalueImpl, parent: StubElement<*>?): CaosScriptRValueStub {
        return CaosScriptRValueStubImpl(
            parent = parent,
            commandString = element.commandString,
            type = element.inferredType,
            argumentValues = element.argumentValues,
            stringStubKind = element.stringStubKind
        )
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        val psi = node?.psi as? CaosScriptRvalue
            ?: return false
        return psi.quoteStringLiteral != null &&
                psi.token != null
    }

}