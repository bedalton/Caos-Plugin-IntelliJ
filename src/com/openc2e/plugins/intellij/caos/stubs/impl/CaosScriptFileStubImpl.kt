package com.openc2e.plugins.intellij.caos.stubs.impl

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptFileStub
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

class CaosScriptFileStubImpl(caosFile: CaosScriptFile?, override val fileName:String, override val variant: String) : PsiFileStubImpl<CaosScriptFile>(caosFile), CaosScriptFileStub {
    override fun getType(): IStubFileElementType<out CaosScriptFileStub> {
        return CaosScriptStubTypes.FILE
    }
}
