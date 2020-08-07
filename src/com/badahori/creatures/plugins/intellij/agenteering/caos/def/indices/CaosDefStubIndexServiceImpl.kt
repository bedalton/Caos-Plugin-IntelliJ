package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefValuesListStub

class CaosDefStubIndexServiceImpl : CaosDefStubIndexService {


    override fun indexCommand(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefCommandElementsByNameIndex.KEY, stub.command)
    }

    override fun indexValuesList(stub: CaosDefValuesListStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefValuesListDefinitionElementsByNameIndex.KEY, stub.typeName)
    }

    override fun indexDocCommentHashtag(stub: CaosDefDocCommentHashtagStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefHashTagsIndex.KEY, stub.hashtag)
    }

}