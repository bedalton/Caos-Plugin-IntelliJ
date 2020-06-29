package com.openc2e.plugins.intellij.agenteering.caos.def.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.CaosDefFileStub
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosScriptFileType

class CaosDefFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosDefLanguage.instance) {

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub():CaosDefFileStub? {
        return super.getStub() as? CaosDefFileStub
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Script"
    }
}
