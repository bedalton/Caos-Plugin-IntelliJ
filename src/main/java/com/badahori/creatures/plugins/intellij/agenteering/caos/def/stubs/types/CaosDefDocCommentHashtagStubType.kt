package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefStubIndexService
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefDocCommentHashtagImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefDocCommentHashtagStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream


class CaosDefDocCommentHashtagStubType(debugName:String) : CaosDefStubElementType<CaosDefDocCommentHashtagStub, CaosDefDocCommentHashtagImpl>(debugName) {
    override fun createPsi(stub: CaosDefDocCommentHashtagStub): CaosDefDocCommentHashtagImpl {
        return CaosDefDocCommentHashtagImpl(stub, this)
    }

    override fun serialize(stub: CaosDefDocCommentHashtagStub, stream: StubOutputStream) {
        stream.writeName(stub.hashtag)
        stream.writeStringList(stub.variants.map { it.code })
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>?): CaosDefDocCommentHashtagStub {
        val name = stream.readNameAsString() ?: UNDEF
        val variants = stream.readStringList().map { CaosVariant.fromVal(it) }
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
        CaosDefStubIndexService.indexDocCommentHashtag(stub, indexSink)
    }
}