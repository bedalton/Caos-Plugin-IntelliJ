package com.badahori.creatures.plugins.intellij.agenteering.catalogue.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.intellij.psi.stubs.PsiFileStub

interface CatalogueFileStub :  PsiFileStub<CatalogueFile> {
    val fileName: String
    val entryNames: List<String>
}