package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.intellij.openapi.module.ModuleConfigurationEditor
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState
import javax.swing.JComponent


class CaosModuleConfigurationEditorProvider : ModuleConfigurationEditorProvider {
    override fun createEditors(state: ModuleConfigurationState): Array<ModuleConfigurationEditor> {
        val module = state.rootModel?.module ?: return emptyArray()
        if (ModuleType.get(module) !is CaosScriptModuleType)
            return ModuleConfigurationEditor.EMPTY
        return arrayOf(//ContentEntriesEditor(module.name, state),
                CaosContentEntiesEditor(module.name, state),
                CaosModuleConfigurationEditor(state))
    }
}


class CaosModuleConfigurationEditor(private val state: ModuleConfigurationState) : ModuleConfigurationEditor {
    private val panel by lazy { CaosProjectGeneratorPeerImpl() }

    override fun isModified(): Boolean {
        return state.rootModel.module.variant != panel.selectedVariant
    }

    override fun getDisplayName(): String = "CAOS Script Settings"

    override fun apply() {
        state.rootModel.module.variant = panel.selectedVariant
    }

    override fun createComponent(): JComponent? {
        val variant = state.rootModel.module.variant
        panel.selectedVariant = variant
        return panel.`$$$getRootComponent$$$`()
    }

}