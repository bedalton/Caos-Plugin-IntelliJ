package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptMacroImpl
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptMacroStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptConstantAssignmentStruct
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptMacroStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

class CaosScriptMacroStubType(debugName:String) : CaosScriptStubElementType<CaosScriptMacroStub, CaosScriptMacroImpl>(debugName) {
    override fun createPsi(parent: CaosScriptMacroStub): CaosScriptMacroImpl {
        return CaosScriptMacroImpl(parent, this)
    }

    override fun serialize(stub: CaosScriptMacroStub, stream: StubOutputStream) {
        // nothing to serialize
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosScriptMacroStub {
        return CaosScriptMacroStubImpl(parent = parent)
    }

    override fun createStub(element: CaosScriptMacroImpl, parent: StubElement<*>?): CaosScriptMacroStub {
        return CaosScriptMacroStubImpl(parent)
    }

}