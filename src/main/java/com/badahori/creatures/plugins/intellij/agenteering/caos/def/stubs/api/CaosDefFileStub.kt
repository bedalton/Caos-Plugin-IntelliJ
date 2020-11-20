package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariants
import com.intellij.psi.stubs.PsiFileStub

interface CaosDefFileStub :  PsiFileStub<CaosDefFile> {
    val fileName: String
    val variants:List<CaosVariant>
}

fun HasVariants.isVariant(variant: CaosVariant, strict:Boolean = false) : Boolean {
    val variants = variants
    if (variants.isEmpty())
        return !strict
    return variant in variants
}