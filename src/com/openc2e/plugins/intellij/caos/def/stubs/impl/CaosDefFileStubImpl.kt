package com.openc2e.plugins.intellij.caos.def.stubs.impl

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefFileStub
import com.openc2e.plugins.intellij.caos.def.stubs.types.CaosDefStubTypes

class CaosDefFileStubImpl(caosFile:CaosDefFile?, override val fileName:String) : PsiFileStubImpl<CaosDefFile>(caosFile), CaosDefFileStub{
    override fun getType(): IStubFileElementType<out CaosDefFileStub> {
        return CaosDefStubTypes.FILE
    }
}
