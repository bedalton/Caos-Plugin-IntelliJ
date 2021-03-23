package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrBlank
import com.intellij.application.options.colors.TextAttributesDescription
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.impl.PsiElementBase
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

class GenericPsiNode<T:PsiElement>(
    project:Project,
    element:T,
    private val presentableText:String? = null,
    private val elementIcon:Icon? = null,
    private val locationString:String? = null,
    private val locationPrefix:String? = null,
    private val sortWeight:Int? = null
) : AbstractTreeNode<T>(project, element) {

    val pointer = SmartPointerManager.createPointer(element)

    override fun canNavigate(): Boolean {
        return (pointer.element as? com.intellij.pom.Navigatable)?.canNavigate() ?: false
    }

    override fun navigate(requestFocus: Boolean) {
        (pointer.element as? com.intellij.pom.Navigatable)?.navigate(requestFocus)
    }

    override fun update(presentation: PresentationData) {
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