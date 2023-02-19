package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.bedalton.common.exceptions.IOException
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType


private const val NAME = "pray.FILE"



interface PrayFileStub :  PsiFileStub<PrayFile>  {
    val language: String?
    val agentBlocks: List<AgentBlockStruct>
    val inlineFiles: List<InlineFileStruct>

}

class PrayFileStubImpl(
    prayFile: PrayFile?,
    override val language: String?,
    override val agentBlocks: List<AgentBlockStruct>,
    override val inlineFiles: List<InlineFileStruct>
) : PsiFileStubImpl<PrayFile>(prayFile), PrayFileStub {
    override fun getType(): IStubFileElementType<out PrayFileStub> {
        return PrayFileStubType
    }
}
object PrayFileStubType : IStubFileElementType<PrayFileStub>(NAME, PrayLanguage) {


    override fun getBuilder(): StubBuilder {
        return PrayFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return PRAY_STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: PrayFileStub, stream: StubOutputStream) {
        super.serialize(stub, stream)
        stream.writeName(stub.language)
        stream.writeList(stub.agentBlocks) {
            writeAgentBlock(it)
        }
        stream.writeList(stub.inlineFiles) {
            writeInlineFile(it)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): PrayFileStub {
        super.deserialize(stream, parentStub)
        val language = stream.readNameAsString()
        val agentBlocks = stream.readList {
            readAgentBlock()
        }
        val inlineFiles = stream.readList { readInlineFile() }
        return PrayFileStubImpl(
            null,
            language,
            agentBlocks,
            inlineFiles
        )
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        //Ignore
    }
}

private class PrayFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        return if (file !is PrayFile) {
            super.createStubForFile(file)
        } else {
            return PrayFileStubImpl(
                file,
                file.languageString,
                file.agentBlocks,
                file.inlineFiles
            )
        }
    }
}