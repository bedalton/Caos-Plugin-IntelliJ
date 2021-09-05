package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes

class CaosScriptFileStubImpl(
    caosFile: CaosScriptFile?,
    override val fileName:String,
    override val variant: CaosVariant?,
    override val prayTags: List<PrayTagStruct<*>>,
) : PsiFileStubImpl<CaosScriptFile>(caosFile), CaosScriptFileStub {
    override fun getType(): IStubFileElementType<out CaosScriptFileStub> {
        return CaosScriptStubTypes.FILE
    }
}
