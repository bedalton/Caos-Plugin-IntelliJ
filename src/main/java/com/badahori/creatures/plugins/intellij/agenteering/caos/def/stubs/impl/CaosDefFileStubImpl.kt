package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant

class CaosDefFileStubImpl(caosFile:CaosDefFile?, override val fileName:String, override val variants: List<CaosVariant>) : PsiFileStubImpl<CaosDefFile>(caosFile), CaosDefFileStub {
    override fun getType(): IStubFileElementType<out CaosDefFileStub> {
        return CaosDefStubTypes.FILE
    }
}
