package com.openc2e.plugins.intellij.caos.def.stubs.types

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.StringRef
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefFileStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefFileStubImpl
import com.openc2e.plugins.intellij.caos.utils.readList
import com.openc2e.plugins.intellij.caos.utils.readNameAsString
import com.openc2e.plugins.intellij.caos.utils.writeList
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
        stream.writeList(stub.variants) {
            writeName(it)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CaosDefFileStub {
        super.deserialize(stream, parentStub)
        val fileName = StringRef.toString(stream.readName())
        val variants = stream.readList { readNameAsString() }.filterNotNull()
        return CaosDefFileStubImpl(null, fileName, variants)
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        //ServiceManager.getService(StubIndexService::class.java).indexFile(stub as? CaosFileStub, sink)
    }

    companion object {
        private const val NAME = "caosDef.FILE"
    }
}

private class CaosDefFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        return if (file !is CaosDefFile) {
            super.createStubForFile(file)
        } else {
            val fileName = file.name
            return CaosDefFileStubImpl(file, fileName, file.variants)
        }
    }
}