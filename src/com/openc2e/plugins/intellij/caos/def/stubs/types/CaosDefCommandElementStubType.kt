package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefCommandDefinitionStub

class CaosDefCommandElementStubType(debugName:String) : CaosDefStubElementType<CaosDefCommandDefinitionStub, CaosDefCommandDefElementImpl>(debugName) {

    override fun createPsi(stub: CaosDefCommandDefinitionStub): CaosDefCommandDefElementImpl {
        return CaosDefCommandDefElementImpl(stub, this)
    }

    override fun serialize(stub: CaosDefCommandDefinitionStub, stream: StubOutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosDefCommandDefinitionStub {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createStub(element: CaosDefCommandDefElementImpl, parent: StubElement<*>?): CaosDefCommandDefinitionStub {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun indexStub(stub: CaosDefCommandDefinitionStub, indexSing: IndexSink) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}