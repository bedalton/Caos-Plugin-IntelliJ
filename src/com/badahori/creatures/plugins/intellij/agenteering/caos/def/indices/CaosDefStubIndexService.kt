package com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices

import com.intellij.psi.stubs.IndexSink
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefDocCommentHashtagStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefTypeDefinitionStub

interface CaosDefStubIndexService {

    fun indexCommand(stub:CaosDefCommandDefinitionStub, indexSink: IndexSink)
    fun indexTypeDef(stub:CaosDefTypeDefinitionStub, indexSink: IndexSink)
    fun indexDocCommentHashtag(stub:CaosDefDocCommentHashtagStub, indexSink: IndexSink)
}