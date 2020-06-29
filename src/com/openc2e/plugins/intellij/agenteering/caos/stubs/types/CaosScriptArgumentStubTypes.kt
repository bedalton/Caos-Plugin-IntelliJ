package com.openc2e.plugins.intellij.agenteering.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.agenteering.caos.stubs.api.*
import com.openc2e.plugins.intellij.agenteering.caos.stubs.impl.*

/**
 * Stub type for caos expects int
 */
class CaosScriptExpectsIntStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsIntStub, CaosScriptExpectsIntImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsIntStub): CaosScriptExpectsIntImpl {
        return CaosScriptExpectsIntImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsIntStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsIntStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsIntStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsIntImpl, parent: StubElement<*>?): CaosScriptExpectsIntStub {
        return CaosScriptExpectsIntStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects float
 */
class CaosScriptExpectsFloatStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsFloatStub, CaosScriptExpectsFloatImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsFloatStub): CaosScriptExpectsFloatImpl {
        return CaosScriptExpectsFloatImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsFloatStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsFloatStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsFloatStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsFloatImpl, parent: StubElement<*>?): CaosScriptExpectsFloatStub {
        return CaosScriptExpectsFloatStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects decimal
 */
class CaosScriptExpectsDecimalStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsDecimalStub, CaosScriptExpectsDecimalImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsDecimalStub): CaosScriptExpectsDecimalImpl {
        return CaosScriptExpectsDecimalImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsDecimalStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsDecimalStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsDecimalStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsDecimalImpl, parent: StubElement<*>?): CaosScriptExpectsDecimalStub {
        return CaosScriptExpectsDecimalStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}


/**
 * Stub type for caos expects string
 */
class CaosScriptExpectsQuoteStringStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsQuoteStringStub, CaosScriptExpectsQuoteStringImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsQuoteStringStub): CaosScriptExpectsQuoteStringImpl {
        return CaosScriptExpectsQuoteStringImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsQuoteStringStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsQuoteStringStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsQuoteStringStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsQuoteStringImpl, parent: StubElement<*>?): CaosScriptExpectsQuoteStringStub {
        return CaosScriptExpectsQuoteStringStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects c1String
 */
class CaosScriptExpectsC1StringStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsC1StringStub, CaosScriptExpectsC1StringImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsC1StringStub): CaosScriptExpectsC1StringImpl {
        return CaosScriptExpectsC1StringImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsC1StringStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsC1StringStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsC1StringStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsC1StringImpl, parent: StubElement<*>?): CaosScriptExpectsC1StringStub {
        return CaosScriptExpectsC1StringStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects byteString
 */
class CaosScriptExpectsByteStringStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsByteStringStub, CaosScriptExpectsByteStringImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsByteStringStub): CaosScriptExpectsByteStringImpl {
        return CaosScriptExpectsByteStringImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsByteStringStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsByteStringStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsByteStringStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsByteStringImpl, parent: StubElement<*>?): CaosScriptExpectsByteStringStub {
        return CaosScriptExpectsByteStringStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects value
 */
class CaosScriptExpectsValueStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsValueStub, CaosScriptExpectsValueImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsValueStub): CaosScriptExpectsValueImpl {
        return CaosScriptExpectsValueImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsValueStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsValueStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsValueStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsValueImpl, parent: StubElement<*>?): CaosScriptExpectsValueStub {
        return CaosScriptExpectsValueStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}

/**
 * Stub type for caos expects token
 */
class CaosScriptExpectsTokenStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsTokenStub, CaosScriptExpectsTokenImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsTokenStub): CaosScriptExpectsTokenImpl {
        return CaosScriptExpectsTokenImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsTokenStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsTokenStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsTokenStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsTokenImpl, parent: StubElement<*>?): CaosScriptExpectsTokenStub {
        return CaosScriptExpectsTokenStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}


/**
 * Stub type for caos expects agent
 */
class CaosScriptExpectsAgentStubType(debugName:String) : CaosScriptStubElementType<CaosScriptExpectsAgentStub, CaosScriptExpectsAgentImpl>(debugName) {
    override fun createPsi(stub: CaosScriptExpectsAgentStub): CaosScriptExpectsAgentImpl {
        return CaosScriptExpectsAgentImpl(stub, this)
    }

    override fun serialize(stub: CaosScriptExpectsAgentStub, stream: StubOutputStream) {
        stream.writeInt(stub.index)
        stream.writeCaosVar(stub.caosVar)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptExpectsAgentStub {
        val index = stream.readInt()
        val caosVar = stream.readCaosVar()
        return CaosScriptExpectsAgentStubImpl(parent, index, caosVar)
    }

    override fun createStub(element: CaosScriptExpectsAgentImpl, parent: StubElement<*>?): CaosScriptExpectsAgentStub {
        return CaosScriptExpectsAgentStubImpl(
                parent = parent,
                caosVar = element.toCaosVar(),
                index = element.index
        )
    }
}