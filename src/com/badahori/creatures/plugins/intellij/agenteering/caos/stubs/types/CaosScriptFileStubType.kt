package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.orDefault
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.CAOS_SCRIPT_STUB_VERSION
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl.CaosScriptFileStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.StringRef
import java.io.IOException

class CaosScriptFileStubType : IStubFileElementType<CaosScriptFileStub>(NAME, CaosScriptLanguage.instance) {

    override fun getBuilder(): StubBuilder {
        return CaosScriptFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return CAOS_SCRIPT_STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: CaosScriptFileStub, stream: StubOutputStream) {
        super.serialize(stub, stream)
        stream.writeName(stub.fileName)
        stream.writeName(stub.variant.code)
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CaosScriptFileStub {
        super.deserialize(stream, parentStub)
        val fileName = StringRef.toString(stream.readName())
        val variant = stream.readNameAsString()?.let { CaosVariant.fromVal(it)} ?: CaosVariant.UNKNOWN
        return CaosScriptFileStubImpl(null, fileName, variant)
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
            return CaosScriptFileStubImpl(file, fileName, file.variant.orDefault())
        }
    }
}