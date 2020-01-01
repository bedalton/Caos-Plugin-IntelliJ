package com.openc2e.plugins.intellij.caos.def.stubs.api

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefHeader

interface CaosDefFileStub :  PsiFileStub<CaosDefFile> {
    val fileName: String
    val variants:List<String>
}

val CaosDefFile.variants:List<String> get() {
    var variants = stub?.variants
    if (variants == null)
        variants = PsiTreeUtil.findChildOfType(this, CaosDefHeader::class.java)?.variants
    return variants ?: emptyList()
}

fun CaosDefFile.isVariant(variant:String, strict:Boolean = false) : Boolean {
    val variants = variants
    if (variants.isEmpty())
        return !strict
    return variant in variants
}