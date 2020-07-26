package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.errorNotification
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilderListener
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import javafx.scene.control.ComboBox
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class CaosScriptModuleBuilder : ModuleBuilder(), ModuleBuilderListener {

    private val variantComboBox by lazy {
        val variantComboBox = com.intellij.openapi.ui.ComboBox(arrayOf(
                CaosVariant.C1,
                CaosVariant.C2,
                CaosVariant.CV,
                CaosVariant.C3,
                CaosVariant.DS
        ))
        variantComboBox.setToolTipText("CAOS Variant")
        variantComboBox.setRenderer(ListCellRenderer { list: JList<out CaosVariant>?, value: CaosVariant, index: Int, isSelected: Boolean, cellHasFocus: Boolean ->
            val label = JLabel(value.fullName)
            if (isSelected) {
                label.background = Color.lightGray
            }
            label
        })
        variantComboBox
    }

    init {
        addListener(this)
    }

    override fun getModuleType(): ModuleType<*> {
        return CaosScriptModuleType.INSTANCE
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        settingsStep.addSettingsField("Caos Variant", variantComboBox)
        return super.modifySettingsStep(settingsStep)
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        CaosBundleSourcesRegistrationUtil.addLibrary(modifiableModel = modifiableRootModel)
        moduleFileDirectory?.let {
            modifiableRootModel.addContentEntry("file://$it")
            true
        } ?: modifiableRootModel.addContentEntry(moduleFilePath)
    }

    override fun moduleCreated(module: Module) {
        ApplicationManager.getApplication().runWriteAction {
            val manager = ModuleRootManager.getInstance(module)
            val modifiableModel: ModifiableRootModel = ModifiableModelsProvider.SERVICE.getInstance().getModuleModifiableModel(module)
            val variant = (variantComboBox.selectedItem as? CaosVariant) ?: CaosScriptProjectSettings.variant
            module.putUserData(CaosScriptFile.VariantUserDataKey, variant)
            module.variant = variant
            module.rootManager.modifiableModel.apply {
                inheritSdk()
                contentEntries.firstOrNull()?.apply setupRoot@{
                    val project = module.project
                    val baseDir = file
                    ?: run {
                        errorNotification(project, "Created project does not have a root directory.")
                        return@setupRoot
                    }
                    addSourceFolder(baseDir, false)
                }
                commit()
            }
            ModifiableModelsProvider.SERVICE.getInstance().commitModuleModifiableModel(modifiableModel)
        }
    }
}