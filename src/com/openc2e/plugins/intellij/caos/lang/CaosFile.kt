package com.openc2e.plugins.intellij.caos.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.stubs.interfaces.CaosFileStub

class CaosFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosLanguage.instance) {

    override fun getFileType(): FileType {
        return CaosFileType.INSTANCE
    }

    override fun getStub():CaosFileStub? {
        return super.getStub() as? CaosFileStub
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Script"
    }
}
