package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueLanguage
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueFileStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.impl.CatalogueFileStubImpl
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import java.io.IOException

class CatalogueFileStubType : IStubFileElementType<CatalogueFileStub>(NAME, CatalogueLanguage) {

    override fun getBuilder(): StubBuilder {
        return CatalogueFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return CatalogueStubVersions.STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: CatalogueFileStub, stream: StubOutputStream) {
        super.serialize(stub, stream)
        stream.writeName(stub.fileName)
        stream.writeStringList(stub.entryNames)
    }

    @Throws(IOException::class)
    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): CatalogueFileStub {
        super.deserialize(stream, parentStub)
        val fileName = stream.readNameAsString()!!
        val entryNames = stream.readStringList()
        return CatalogueFileStubImpl(null, fileName, entryNames)
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        //ServiceManager.getService(CatalogueStubIndexService::class.java).indexFile(stub as? CatalogueFileStub, sink)
    }

    companion object {
        private const val NAME = "catalogue.FILE"
    }
}

private class CatalogueFileStubBuilder : DefaultStubBuilder() {
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        return if (file !is CatalogueFile) {
            super.createStubForFile(file)
        } else {
            val fileName = file.name
            return CatalogueFileStubImpl(file, fileName, file.getEntryNames())
        }
    }
}