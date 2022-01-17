package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.impl.PsiElementBase
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

class GenericPsiNode<T:PsiElement>(
    private val nonNullProject:Project,
    element:T,
    private val presentableText:String? = null,
    private val elementIcon:Icon? = null,
    private val locationString:String? = null,
    private val locationPrefix:String? = null,
    private val sortWeight:Int? = null
) : AbstractTreeNode<T>(nonNullProject, element) {

    fun isValid(): Boolean {
        return !nonNullProject.isDisposed && pointer.virtualFile?.isValid == true// && pointer.element?.isValid == true
    }

    val pointer = SmartPointerManager.createPointer(element)

    override fun canNavigate(): Boolean {
        return isValid() && (pointer.element as? com.intellij.pom.Navigatable)?.canNavigate() == true
    }

    override fun navigate(requestFocus: Boolean) {
        if (!isValid()) {
            return
        }
        (pointer.element as? com.intellij.pom.Navigatable)?.navigate(requestFocus)
    }

    override fun update(presentation: PresentationData) {
        if (!isValid()) {
            return
        }
        val element = pointer.element
        val presentationData = (element as? PsiElementBase)?.presentation
        val text = presentableText
            ?: presentationData?.presentableText
            ?: (element as? PsiNamedElement)?.name
            ?: element?.text
        val locationSuffix = locationString ?: presentationData?.locationString
        if (locationPrefix.isNotNullOrBlank()) {
            presentation.clearText()
            presentation.addText(ColoredFragment("$locationPrefix ", SimpleTextAttributes.GRAYED_ATTRIBUTES))
            presentation.addText(text,SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (locationSuffix != null)
                presentation.addText(ColoredFragment(locationSuffix, SimpleTextAttributes.GRAYED_ATTRIBUTES))
        } else {
            presentation.presentableText = text
            presentation.locationString = locationSuffix ?: ""
        }
        val icon = elementIcon ?: presentationData?.getIcon(false)
        if (icon != null)
            presentation.setIcon(icon)

    }

    override fun getWeight(): Int {
        return sortWeight ?: super.getWeight()
    }

    override fun getChildren(): Collection<AbstractTreeNode<Any>> {
        return EMPTY_COLLECTION
    }

    companion object {
        private val EMPTY_COLLECTION:Collection<AbstractTreeNode<Any>> = emptyList()
    }
}