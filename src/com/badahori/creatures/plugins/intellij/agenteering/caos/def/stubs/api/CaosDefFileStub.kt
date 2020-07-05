package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefHeader
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant

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