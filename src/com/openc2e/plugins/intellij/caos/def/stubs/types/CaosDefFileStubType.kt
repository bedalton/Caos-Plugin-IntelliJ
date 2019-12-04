package com.openc2e.plugins.intellij.caos.def.stubs.types


import brightscript.intellij.lang.BrightScriptLanguage
import brightscript.intellij.lang.BrsFile
import brightscript.intellij.stubs.impl.BrsFileStubImpl
import brightscript.intellij.stubs.interfaces.BrsFileStub
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.StringRef
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.stubs.interfaces.CaosDefFileStub
import java.io.IOException

class CaosDefFileStubType : IStubFileElementType<CaosDefFileStub>(NAME, CaosDefLanguage.instance) {

    override fun getBuilder(): StubBuilder {
        return CaosDefFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return CaosDefStubVersions.STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: CaosDefFileStub, stream: StubOutputStream) {
        super.serialize(stub, stream)
        stream.writeName(stub.fileName)
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CaosDefFileStub {
        super.deserialize(stream, parentStub)
        val fileName = StringRef.toString(stream.readName())
        return CaosDefFileStubImpl(null, fileName)
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        //ServiceManager.getService(StubIndexService::class.java).indexFile(stub as? BrsFileStub, sink)
    }

    companion object {
        private const val NAME = "brs.FILE"
    }
}

private class CaosDefFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        return if (file !is BrsFile) {
            super.createStubForFile(file)
        } else {
            val fileName = file.name
            return CaosDefFileStubImpl(file, fileName)
        }
    }
}