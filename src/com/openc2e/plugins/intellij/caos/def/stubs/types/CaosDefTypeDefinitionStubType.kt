package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefTypeDefinitionElementImpl
import com.openc2e.plugins.intellij.caos.def.psi.util.CaosDefPsiImplUtil
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefTypeDefinitionStub
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefinitionStubImpl
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.intellij.openapi.components.ServiceManager
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefStubIndexService

class CaosDefTypeDefinitionStubType(debugName:String) : CaosDefStubElementType<CaosDefTypeDefinitionStub, CaosDefTypeDefinitionElementImpl>(debugName) {

    override fun createPsi(stub: CaosDefTypeDefinitionStub): CaosDefTypeDefinitionElementImpl {
        return CaosDefTypeDefinitionElementImpl(stub, this)
    }

    override fun serialize(stub: CaosDefTypeDefinitionStub, stream: StubOutputStream) {
        stream.writeName(stub.typeName)
        stream.writeList(stub.keys, StubOutputStream::writeTypeDefValue)
    }

    override fun deserialize(stream: StubInputStream, parent: StubElement<*>): CaosDefTypeDefinitionStub {
        val name = stream.readNameAsString().nullIfEmpty() ?: CaosDefPsiImplUtil.UnknownReturn
        val keys = stream.readList(StubInputStream::readTypeDefValue).filterNotNull()
        return CaosDefTypeDefinitionStubImpl (
                parent = parent,
                typeName = name,
                keys = keys
        )
    }

    override fun createStub(element: CaosDefTypeDefinitionElementImpl, parent: StubElement<*>): CaosDefTypeDefinitionStub {
        return CaosDefTypeDefinitionStubImpl (
                parent = parent,
                typeName = element.typeName,
                keys = element.keys
        )
    }

    override fun indexStub(stub: CaosDefTypeDefinitionStub, indexSink: IndexSink) {
       ServiceManager.getService(CaosDefStubIndexService::class.java).indexTypeDef(stub, indexSink)
    }
}