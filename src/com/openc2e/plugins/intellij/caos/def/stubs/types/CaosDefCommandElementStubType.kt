package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.stubs.interfaces.CaosDefCommandElementStub

class CaosDefCommandElementStubType(debugName:String) : CaosDefStubElementType<CaosDefCommandElementStub, CaosDefCommandDefElementImpl>(debugName) {
    override fun createPsi(p0: CaosDefCommandElementStub): CaosDefCommandDefElementImpl {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serialize(p0: CaosDefCommandElementStub, p1: StubOutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deserialize(p0: StubInputStream, p1: StubElement<*>?): CaosDefCommandElementStub {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createStub(p0: CaosDefCommandDefElementImpl, p1: StubElement<*>?): CaosDefCommandElementStub {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun indexStub(p0: CaosDefCommandElementStub, p1: IndexSink) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}