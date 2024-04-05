package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefValuesListStub

object CaosDefStubIndexService {


    fun indexCommand(stub: CaosDefCommandDefinitionStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefCommandElementsByNameIndex.KEY, stub.command)
    }

    fun indexValuesList(stub: CaosDefValuesListStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefValuesListElementsByNameIndex.KEY, stub.listName)
    }

    fun indexDocCommentHashtag(stub: CaosDefDocCommentHashtagStub, indexSink: IndexSink) {
        indexSink.occurrence(CaosDefHashTagsIndex.KEY, stub.hashtag)
    }

}