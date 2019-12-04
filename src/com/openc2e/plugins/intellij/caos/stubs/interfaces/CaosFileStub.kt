package com.openc2e.plugins.intellij.caos.stubs.interfaces

import com.openc2e.plugins.intellij.caos.lang.CaosFile
import com.intellij.psi.stubs.PsiFileStub

interface CaosFileStub :  PsiFileStub<CaosFile> {
    val fileName: String
}