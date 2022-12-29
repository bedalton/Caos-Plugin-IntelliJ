package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.impl

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api.CatalogueFileStub
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.types.CatalogueStubTypes
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType

class CatalogueFileStubImpl(catalogueFile:CatalogueFile?, override val fileName:String, override val entryNames: List<String>) : PsiFileStubImpl<CatalogueFile>(catalogueFile), CatalogueFileStub {
    override fun getType(): IStubFileElementType<out CatalogueFileStub> {
        return CatalogueStubTypes.FILE
    }
}
