package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage


class CaosScriptCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun createCustomSettings(settings: CodeStyleSettings?): CustomCodeStyleSettings? {
        return CaosScriptCodeStyleSettings(settings)
    }

    override fun getConfigurableDisplayName(): String = "Caos Script"

    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(settings, modelSettings, this.configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
                return CaosScriptCodeStyleMainPanel(currentSettings, settings)
            }
        }
    }


    private class CaosScriptCodeStyleMainPanel(currentSettings: CodeStyleSettings?, settings: CodeStyleSettings?) : TabbedLanguageCodeStylePanel(CaosScriptLanguage, currentSettings, settings)
}