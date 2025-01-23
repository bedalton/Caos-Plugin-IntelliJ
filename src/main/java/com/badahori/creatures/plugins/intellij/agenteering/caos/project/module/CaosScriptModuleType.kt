package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.intellij.openapi.module.ModuleType
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
        const val GROUP_NAME = "Caos Script"
    }
}

internal val CAOS_SCRIPT_MODULE_INSTANCE by lazy {
    CaosScriptModuleType()
}