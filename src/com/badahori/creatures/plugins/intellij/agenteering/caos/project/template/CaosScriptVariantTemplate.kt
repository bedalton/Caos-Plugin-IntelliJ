package com.badahori.creatures.plugins.intellij.agenteering.caos.project.template

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectTemplate
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosScriptModuleBuilder
import icons.CaosScriptIcons
import javax.swing.Icon


class CaosScriptVariantTemplate: CaosScriptProjectGenerator(), ProjectTemplate {


    override fun createModuleBuilder(): ModuleBuilder {
        return CaosScriptModuleBuilder()
    }

    override fun validateSettings(): ValidationInfo? {
        return null
    }

    override fun getIcon(): Icon? {
        return CaosScriptIcons.MODULE_ICON
    }

    override fun getDescription(): String? {
        return null
    }

    override fun generateProject(project: Project, p1: VirtualFile, info: CaosProjectGeneratorInfo, module: Module) {
        // Ignore
    }

    override fun getName(): String {
        return "CAOS Project"
    }

    override fun getLogo(): Icon? {
        return icon
    }
}