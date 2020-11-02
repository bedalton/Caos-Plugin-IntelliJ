package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefHeader
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariants

interface CaosDefFileStub :  PsiFileStub<CaosDefFile> {
    val fileName: String
    val variants:List<CaosVariant>
}

fun HasVariants.isVariant(variant:CaosVariant, strict:Boolean = false) : Boolean {
    val variants = variants
    if (variants.isEmpty())
        return !strict
    return variant in variants
}