package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.StringRef
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.stubs.CAOS_SCRIPT_STUB_VERSION
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptFileStub
import com.openc2e.plugins.intellij.caos.stubs.impl.CaosScriptFileStubImpl
import com.openc2e.plugins.intellij.caos.utils.readNameAsString
import java.io.IOException

class CaosScriptFileStubType : IStubFileElementType<CaosScriptFileStub>(NAME, CaosScriptLanguage.instance) {

    override fun getBuilder(): StubBuilder {
        return CaosScriptFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return CAOS_SCRIPT_STUB_VERSION;
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: CaosScriptFileStub, stream: StubOutputStream) {
        super.serialize(stub, stream)
        stream.writeName(stub.fileName)
        stream.writeName(stub.variant)
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CaosScriptFileStub {
        super.deserialize(stream, parentStub)
        val fileName = StringRef.toString(stream.readName())
        val variant = stream.readNameAsString()
        return CaosScriptFileStubImpl(null, fileName, variant ?: "")
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        //ServiceManager.getService(StubIndexService::class.java).indexFile(stub as? CaosFileStub, sink)
    }

    companion object {
        private const val NAME = "caos.FILE"
    }
}

private class CaosScriptFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        return if (file !is CaosScriptFile) {
            super.createStubForFile(file)
        } else {
            val fileName = file.name
            return CaosScriptFileStubImpl(file, fileName, file.variant)
        }
    }
}