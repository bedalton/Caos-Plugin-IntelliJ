package com.openc2e.plugins.intellij.caos.def.stubs.api

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefHeader
import com.openc2e.plugins.intellij.caos.lang.CaosVariant

interface CaosDefFileStub :  PsiFileStub<CaosDefFile> {
    val fileName: String
    val variants:List<CaosVariant>
}

val CaosDefFile.variants:List<CaosVariant> get() {
    var variants = stub?.variants
    if (variants == null)
        variants = PsiTreeUtil.findChildOfType(this, CaosDefHeader::class.java)?.variants
    return variants ?: emptyList()
}

fun CaosDefFile.isVariant(variant:CaosVariant, strict:Boolean = false) : Boolean {
    val variants = variants
    if (variants.isEmpty())
        return !strict
    return variant in variants
}