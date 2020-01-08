package com.openc2e.plugins.intellij.caos.stubs.api

import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.intellij.psi.stubs.PsiFileStub

interface CaosScriptFileStub :  PsiFileStub<CaosScriptFile> {
    val fileName: String
    val variant:String
}