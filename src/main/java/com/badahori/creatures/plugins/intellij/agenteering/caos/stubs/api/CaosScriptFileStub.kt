package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.intellij.psi.stubs.PsiFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant

interface CaosScriptFileStub :  PsiFileStub<CaosScriptFile> {
    val fileName: String
    val variant: CaosVariant?
    val prayTags: List<PrayTagStruct<*>>
}