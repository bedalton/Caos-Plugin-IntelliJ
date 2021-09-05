package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.impl.PrayAgentBlockImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.Language
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

object PrayAgentBlockStubType
    : CaosScriptStubElementType<PrayAgentBlockStub, PrayAgentBlockImpl>("Pray_AGENT_BLOCK") {

    override fun getLanguage(): Language {
        return PrayLanguage
    }

    override fun getExternalId(): String {
        return "pray." + super.toString()
    }

    override fun serialize(stub: PrayAgentBlockStub, stream: StubOutputStream) {
        stream.writeName(stub.blockTag)
        stream.writeName(stub.blockName)
        stream.writeList(stub.tags) { tag ->
            writeTag(tag)
        }
    }

    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): PrayAgentBlockStub {
        val blockTag = stream.readNameAsString()
        val blockName = stream.readNameAsString()
        val tags = stream.readList {
            readTag()
        }
        return PrayAgentBlockStubImpl(
            parentStub,
            blockTag = blockTag,
            blockName = blockName,
            tags = tags
        )
    }

    override fun createPsi(stub: PrayAgentBlockStub): PrayAgentBlockImpl {
        return PrayAgentBlockImpl(stub, this)
    }

    override fun createStub(psi: PrayAgentBlockImpl, parentStub: StubElement<*>?): PrayAgentBlockStub {
        return PrayAgentBlockStubImpl(
            parentStub,
            blockTag = psi.blockTagString,
            blockName = psi.blockNameString,
            tags = psi.tagStructs
        )
    }

}