package com.openc2e.plugins.intellij.caos.def.stubs.interfaces

import com.intellij.psi.stubs.PsiFileStub
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile

interface CaosDefFileStub :  PsiFileStub<CaosDefFile> {
    val fileName: String
}