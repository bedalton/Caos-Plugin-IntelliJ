package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.errorNotification
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.warningNotification
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilderListener
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VfsUtil
import java.awt.Color
import java.io.File
import javax.swing.JLabel
import javax.swing.JList

class CaosScriptModuleBuilder : ModuleBuilder(), ModuleBuilderListener {

    private val variantComboBox by lazy {
        val variantComboBox = com.intellij.openapi.ui.ComboBox(arrayOf(
                CaosVariant.C1,
                CaosVariant.C2,
                CaosVariant.CV,
                CaosVariant.C3,
                CaosVariant.DS
        ))
        variantComboBox.toolTipText = "CAOS Variant"
        variantComboBox.setRenderer { _: JList<out CaosVariant>?, value: CaosVariant, _: Int, isSelected: Boolean, _: Boolean ->
            val label = JLabel(value.fullName)
            if (isSelected) {
                label.background = Color.lightGray
            }
            label
        }
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
        /*if (!CaosBundleSourcesRegistrationUtil.addLibrary(modifiableModel = modifiableRootModel)) {
            LOGGER.warning("Failed to add library to root model")
        }*/
        moduleFileDirectory?.let {
            val file = File(it)
            if (!file.exists()) {
                VfsUtil.createDirectories(it)
            }
            modifiableRootModel.addContentEntry("file://$it")
            true
        } ?: modifiableRootModel.addContentEntry(moduleFilePath)
    }

    override fun moduleCreated(module: Module) {
        ApplicationManager.getApplication().runWriteAction {
            val modifiableModel: ModifiableRootModel = ModifiableModelsProvider.SERVICE.getInstance().getModuleModifiableModel(module)
            val variant = (variantComboBox.selectedItem as? CaosVariant) ?: CaosScriptProjectSettings.variant
            variant?.let {
                module.putUserData(CaosScriptFile.VariantUserDataKey, variant)
                module.variant = variant
            }
            module.rootManager.modifiableModel.apply {
                inheritSdk()
                moduleFileDirectory?.let {
                    try {
                        val baseDir = File(it)
                        if (!baseDir.exists()) {
                            VfsUtil.createDirectories(it)
                        }
                    } catch (e: Exception) {
                        errorNotification(project, "Failed to create root directory.")
                    }
                }
                (contentEntries.firstOrNull { it.file != null} ?: contentEntries.firstOrNull()) ?.apply setupRoot@{
                    val project = module.project
                    val baseDir = file ?: sourceFolders.firstOrNull { it.file != null }?.file ?: VfsUtil.findFileByIoFile(File(url), true)
                    ?: run {
                        warningNotification(project, "Module root initialization delayed. Wait or refresh root")
                        return@setupRoot
                    }
                    try {
                        if (!baseDir.exists()) {
                            val file = VfsUtil.virtualToIoFile(baseDir)
                            VfsUtil.createDirectories(file.path)
                        }
                        addSourceFolder(baseDir, false)
                    } catch(e:Exception) {
                        errorNotification(project, "Failed to create root directory.")
                    }
                }
                commit()
            }
            ModifiableModelsProvider.SERVICE.getInstance().commitModuleModifiableModel(modifiableModel)
        }
    }
}