package com.openc2e.plugins.intellij.caos.def.references

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class CaosDefUsagesProvider : FindUsagesProvider{
    override fun getNodeText(element: PsiElement, partial: Boolean): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDescriptiveName(p0: PsiElement): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(p0: PsiElement): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHelpId(p0: PsiElement): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canFindUsagesFor(p0: PsiElement): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}