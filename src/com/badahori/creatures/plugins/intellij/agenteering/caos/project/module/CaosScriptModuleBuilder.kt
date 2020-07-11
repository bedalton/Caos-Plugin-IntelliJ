package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType

class CaosScriptModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> {
        return MODULE_TYPE
    }

    companion object {
        private val MODULE_TYPE = CaosScriptModuleType()
    }

}