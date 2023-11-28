package com.badahori.creatures.plugins.intellij.agenteering.att.psi.api

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

interface AttCompositeElement : PsiElement, NavigationItem {
    fun <PsiT: PsiElement> getChildOfType(childType:Class<PsiT>):PsiT? = PsiTreeUtil.getChildOfType(this, childType)
    fun <PsiT: PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT> = PsiTreeUtil.getChildrenOfTypeAsList(this, childType)
    fun <PsiT: PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT? = PsiTreeUtil.getParentOfType(this, parentClass)

    val descriptiveText:String? get() = null

    val locationString:String? get() = null

    val icon:Icon? get() = null
}


class AttPresentation(
        private val descriptiveText:String?,
        private val locationString:String? = null,
        private val icon: Icon? = null
) : ItemPresentation {
    override fun getPresentableText(): String? = descriptiveText
    override fun getLocationString(): String? = locationString
    override fun getIcon(unused: Boolean): Icon? = icon

}