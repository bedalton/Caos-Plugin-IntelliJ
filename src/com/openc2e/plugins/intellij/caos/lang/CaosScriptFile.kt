package com.openc2e.plugins.intellij.caos.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.FileContentUtil
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptFileStub

class CaosScriptFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, CaosScriptLanguage.instance) {

    var variant:String get () {
        return getUserData(VariantUserDataKey) ?: ""
    } set(newVariant) {
        putUserData(VariantUserDataKey, newVariant)
        FileContentUtil.reparseFiles(virtualFile)
    }

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub():CaosScriptFileStub? {
        return super.getStub() as? CaosScriptFileStub
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Script"
    }

    companion object {
        @JvmStatic
        val VariantUserDataKey = Key<String> ("com.openc2e.plugins.intellij.caos.SCRIPT_VARIANT_KEY")

    }
}

