package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel

class CaosScriptModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> {
        return CaosScriptModuleType.INSTANCE
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        CaosBundleSourcesRegistrationUtil.addLibrary(modifiableModel = modifiableRootModel)
        moduleFileDirectory?.let {
            modifiableRootModel.addContentEntry("file://$it")
            true
        } ?: modifiableRootModel.addContentEntry(moduleFilePath)
    }

}