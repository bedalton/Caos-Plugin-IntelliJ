package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.intellij.openapi.module.ModuleType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.template.CaosScriptTemplateFactory
import icons.CaosScriptIcons
import javax.swing.Icon

class CaosScriptModuleType : ModuleType<CaosScriptModuleBuilder>("CAOS_MODULE") {

    override fun createModuleBuilder(): CaosScriptModuleBuilder {
        return CaosScriptModuleBuilder()
    }

    override fun getName(): String {
        return GROUP_NAME
    }

    override fun getDescription(): String {
        return ""
    }

    override fun getNodeIcon(p0: Boolean): Icon {
        return CaosScriptIcons.MODULE_ICON
    }

    companion object {
        @JvmStatic
        val INSTANCE = CaosScriptModuleType()

        const val GROUP_NAME = "Caos Script"
    }
}