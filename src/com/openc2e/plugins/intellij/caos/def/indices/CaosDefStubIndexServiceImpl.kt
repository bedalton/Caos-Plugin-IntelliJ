package com.openc2e.plugins.intellij.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefTypeDefinitionStub
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank

class CaosDefStubIndexServiceImpl : CaosDefStubIndexService {


    override fun indexCommand(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefCommandElementsByNameIndex.KEY, stub.command)
        if (stub.namespace.isNotNullOrBlank())
            indexSink.occurrence(CaosDefCommandElementsByNamespaceIndex.KEY, stub.namespace!!)
    }

    override fun indexTypeDef(stub: CaosDefTypeDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefTypeDefinitionElementsByNameIndex.KEY, stub.typeName)
    }

    override fun indexDocCommentHashtag(stub: CaosDefDocCommentHashtagStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefHashTagsIndex.KEY, stub.hashtag)
    }

}