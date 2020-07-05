package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefTypeDefinitionStub

class CaosDefStubIndexServiceImpl : CaosDefStubIndexService {


    override fun indexCommand(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefCommandElementsByNameIndex.KEY, stub.command)
    }

    override fun indexTypeDef(stub: CaosDefTypeDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefTypeDefinitionElementsByNameIndex.KEY, stub.typeName)
    }

    override fun indexDocCommentHashtag(stub: CaosDefDocCommentHashtagStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefHashTagsIndex.KEY, stub.hashtag)
    }

}