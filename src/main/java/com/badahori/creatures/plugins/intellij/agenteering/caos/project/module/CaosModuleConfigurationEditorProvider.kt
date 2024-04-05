package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.module.ModuleConfigurationEditor
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState
import javax.swing.JComponent


class CaosModuleConfigurationEditorProvider : ModuleConfigurationEditorProvider {
    override fun createEditors(state: ModuleConfigurationState): Array<ModuleConfigurationEditor> {
        val module = state.currentRootModel?.module ?: return emptyArray()
        if (ModuleType.get(module) !is CaosScriptModuleType) {
            return ModuleConfigurationEditor.EMPTY
        }
        return arrayOf(//ContentEntriesEditor(module.name, state),
                CaosContentEntitiesEditor(module.name, state),
                CaosModuleConfigurationEditor(state)
        )
    }
}


class CaosModuleConfigurationEditor(private val state: ModuleConfigurationState) : ModuleConfigurationEditor {
    private val panel by lazy { CaosProjectGeneratorPeerImpl() }

    override fun isModified(): Boolean {
        // Check if is modified
        val settings = state
            .currentRootModel
            ?.module
            ?.settings
            ?.getState()
            ?: return true

        // Check if variant has changed
        if (settings.variant != panel.selectedVariant)
            return true

        // Check if any files have been added to or removed from ignored file names
        if (settings.ignoredFiles != panel.ignoredFileNames) {
            return true
        }
        return false
    }

    override fun getDisplayName(): String = "CAOS Script Settings"

    override fun apply() {
        state.currentRootModel.module.variant = panel.selectedVariant
        state.currentRootModel.module.settings.ignoredFiles = panel.ignoredFileNames
            .filter {
                it.isNotBlank()
            }
            .map { it.trim() }
    }

    override fun createComponent(): JComponent? {
        val settings = state
            .currentRootModel
            .module
            .settings
            .getState()
        panel.selectedVariant = settings.variant
        panel.setIgnoredFileNames(settings.ignoredFiles)
        return panel.component
    }

    companion object {
        @JvmStatic
        fun invalidLines(textAreaContents: String): List<Int> {
            val lines = textAreaContents.split("\n")
            return lines.indices.filter { i ->
                val line = lines[i]
                line.isBlank() || GameInterfaceName.fromString(line) == null
            }
        }
    }

}