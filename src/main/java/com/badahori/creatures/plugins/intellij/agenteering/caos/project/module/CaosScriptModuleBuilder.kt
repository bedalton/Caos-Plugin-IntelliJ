package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.myModuleFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.warningNotification
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class CaosScriptModuleBuilder : ModuleBuilder(), ModuleBuilderListener {

    private val variantComboBox by lazy {
        val variantComboBox = com.intellij.openapi.ui.ComboBox(arrayOf(
                CaosVariant.C1,
                CaosVariant.C2,
                CaosVariant.CV,
                CaosVariant.C3,
                CaosVariant.DS,
                CaosVariant.SM
        ))
        variantComboBox.toolTipText = "CAOS Variant"
        variantComboBox.setRenderer { _: JList<out CaosVariant>?, value: CaosVariant, _: Int, isSelected: Boolean, _: Boolean ->
            val label = JLabel(value.fullName)
            if (isSelected) {
                label.background = JBColor.lightGray
            }
            label
        }
        variantComboBox
    }

    init {
        addListener(this)
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return object: ModuleWizardStep() {
            override fun getComponent(): JComponent {
                val landingPage = ModuleLandingPage()
                return landingPage.panel
            }

            override fun updateDataModel() {
                // Do nothing
            }

        }
    }

    override fun isTemplateBased(): Boolean {
        return false
    }

    override fun isTemplate(): Boolean {
        return false
    }

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        return emptyArray()
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        settingsStep.addSettingsField("Caos variant", variantComboBox)
        return super.modifySettingsStep(settingsStep)
    }

    override fun getModuleType(): ModuleType<*> {
        return CaosScriptModuleType.INSTANCE
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        /*if (!CaosBundleSourcesRegistrationUtil.addLibrary(modifiableModel = modifiableRootModel)) {
            LOGGER.warning("Failed to add library to root model")
        }*/
        moduleFileDirectory?.let {
            val file = File(it)
            val virtualFile = if (!file.exists()) {
                VfsUtil.createDirectories(it)
            } else {
                VfsUtil.findFileByIoFile(file, true)
            }
            modifiableRootModel.addContentEntry("file://$it")
            VfsUtil.markDirtyAndRefresh(true, true, true, virtualFile)
            true
        } ?: modifiableRootModel.addContentEntry(moduleFilePath)
    }

    override fun moduleCreated(module: Module) {
        ApplicationManager.getApplication().runWriteAction {
            val modifiableModel: ModifiableRootModel = ModifiableModelsProvider.SERVICE.getInstance().getModuleModifiableModel(module)
            val variant = (variantComboBox.selectedItem as? CaosVariant) ?: module.project.settings.let { it.lastVariant ?: it.defaultVariant }
            variant?.let {
                module.putUserData(CaosScriptFile.ExplicitVariantUserDataKey, variant)
                module.variant = variant
            }
            module.rootManager.modifiableModel.apply {
                addModuleToModifiableModel(project, this, module, moduleFileDirectory)
                commit()
            }
            ModifiableModelsProvider.SERVICE.getInstance().commitModuleModifiableModel(modifiableModel)
        }
    }
}


private fun addModuleToModifiableModel(project: Project, modifiableModel: ModifiableRootModel, module: Module, moduleFileDirectory: String?) {
    modifiableModel.inheritSdk()
    moduleFileDirectory?.let {
        try {
            val baseDir = File(it)
            if (!baseDir.exists()) {
                VfsUtil.createDirectories(it)
            }
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            errorNotification(project, "Failed to create root directory.")
        }
    }
    (modifiableModel.contentEntries.firstOrNull { it.file != null} ?: modifiableModel.contentEntries.firstOrNull()) ?.apply setupRoot@{
        setupContentEntries(project, module, this, url)
    }
}

private fun setupContentEntries(project: Project, module: Module, contentEntries: ContentEntry, url: String) {
    val baseDir = module.myModuleFile
        ?: contentEntries.sourceFolders.firstOrNull { it.file != null }?.file
        ?: VfsUtil.findFileByIoFile(File(url), true)
    ?: run {
            invokeLater {
                val temp = module.myModuleFile
                    ?: contentEntries
                        .sourceFolders
                        .firstOrNull {
                            it.file != null
                        }
                        ?.file
                    ?: VfsUtil.findFileByIoFile(File(url), true)
                if (temp == null) {
                    warningNotification(project, "Module root initialization delayed. Wait or refresh root")
                }
            }
            return
        }
    try {
        if (!baseDir.exists()) {
            val file = VfsUtil.virtualToIoFile(baseDir)
            VfsUtil.createDirectories(file.path)
            VfsUtil.findFileByIoFile(file, true)?.let { directoryFile ->
                VfsUtil.markDirtyAndRefresh(true, true, false, directoryFile)
            }
        }
        contentEntries.addSourceFolder(baseDir, false)
    } catch(e:Exception) {
        errorNotification(project, "Failed to create root directory.")
    }
}