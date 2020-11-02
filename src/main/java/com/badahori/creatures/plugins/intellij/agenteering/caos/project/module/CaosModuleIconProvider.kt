package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.intellij.ide.IconProvider
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import javax.swing.Icon

class CaosModuleIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flag: Int): Icon? {
        val directory = element as? PsiDirectory
                ?: return null
        val virtualFile = directory.virtualFile
        if (!ProjectRootsUtil.isModuleContentRoot(virtualFile, element.project)) {
            return null
        }
        return virtualFile.getModule(element.project)?.variant
                ?.icon
    }
}