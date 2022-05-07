@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.template.CaosProjectGeneratorInfo
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.ProjectGeneratorPeer
import java.awt.event.ItemEvent
import javax.swing.JComponent

class CaosProjectGeneratorPeer:ProjectGeneratorPeer<CaosProjectGeneratorInfo>  {

    private val panel by lazy {
        CaosProjectGeneratorPeerImpl()
    }

    init {
        panel.variantComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED)
                settings.variant = e.item as CaosVariant
        }
    }

    private val settings:CaosProjectGeneratorInfo = CaosProjectGeneratorInfo()

    override fun getComponent(): JComponent {
        return panel.component
    }

    override fun getSettings(): CaosProjectGeneratorInfo = settings

    override fun validate(): ValidationInfo? = null

    override fun buildUI(settingsStep: SettingsStep) {
        settingsStep.addSettingsComponent(component)
    }

    override fun isBackgroundJobRunning(): Boolean = false


}