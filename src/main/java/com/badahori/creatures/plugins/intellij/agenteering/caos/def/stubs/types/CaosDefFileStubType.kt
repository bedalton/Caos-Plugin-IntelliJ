package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefFileStubImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.writeList
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
            writeName(it.code)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CaosDefFileStub {
        super.deserialize(stream, parentStub)
        val fileName = stream.readNameAsString()!!
        val variants = stream.readList { readNameAsString() }.filterNotNull().map { CaosVariant.fromVal(it) }
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