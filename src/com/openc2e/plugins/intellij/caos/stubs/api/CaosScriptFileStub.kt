package com.openc2e.plugins.intellij.caos.stubs.api

import com.intellij.psi.stubs.PsiFileStub
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile

interface CaosScriptFileStub :  PsiFileStub<CaosScriptFile> {
    val fileName: String
    val variant:String
}