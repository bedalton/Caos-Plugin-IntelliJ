package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefStubIndexService
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefDocCommentHashtagImpl
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefDocCommentHashtagStubImpl
import com.openc2e.plugins.intellij.caos.psi.util.UNDEF
import com.openc2e.plugins.intellij.caos.stubs.types.readStringList
import com.openc2e.plugins.intellij.caos.stubs.types.writeStringList
import com.openc2e.plugins.intellij.caos.utils.readNameAsString


class CaosDefDocCommentHashtagStubType(debugName:String) : CaosDefStubElementType<CaosDefDocCommentHashtagStub, CaosDefDocCommentHashtagImpl>(debugName) {
    override fun createPsi(stub: CaosDefDocCommentHashtagStub): CaosDefDocCommentHashtagImpl {
        return CaosDefDocCommentHashtagImpl(stub, this)
    }

    override fun serialize(stub: CaosDefDocCommentHashtagStub, stream: StubOutputStream) {
        stream.writeName(stub.hashtag)
        stream.writeStringList(stub.variants)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosDefDocCommentHashtagStub {
        val name = stream.readNameAsString() ?: UNDEF
        val variants = stream.readStringList()
        return CaosDefDocCommentHashtagStubImpl(
                parent = parent,
                hashtag = name,
                variants = variants
        )
    }

    override fun createStub(element: CaosDefDocCommentHashtagImpl, parent: StubElement<*>?): CaosDefDocCommentHashtagStub {
        return CaosDefDocCommentHashtagStubImpl(
                parent = parent,
                hashtag = element.name,
                variants = element.variants
        )
    }

    override fun indexStub(stub: CaosDefDocCommentHashtagStub, indexSink: IndexSink) {
        ServiceManager.getService(CaosDefStubIndexService::class.java).indexDocCommentHashtag(stub, indexSink)
    }
}