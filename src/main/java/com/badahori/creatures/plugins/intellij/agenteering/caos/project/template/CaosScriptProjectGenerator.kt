package com.badahori.creatures.plugins.intellij.agenteering.caos.project.template

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosProjectGeneratorPeer
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosScriptModuleBuilder
import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.platform.ProjectTemplate
import icons.CaosScriptIcons
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

open class CaosScriptProjectGenerator : DirectoryProjectGeneratorBase<CaosProjectGeneratorInfo>(), ProjectTemplate {

    override fun validate(baseDirPath: String): ValidationResult {
        return ValidationResult.OK
    }

    override fun createModuleBuilder(): ModuleBuilder {
        return CaosScriptModuleBuilder()
    }

    /**
     * @return null if ok, error message otherwise
     * @deprecated unused API
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("unused API")
    @Nullable
    override fun validateSettings(): ValidationInfo? {
        return null
    }

    override fun getIcon(): Icon? {
        return CaosScriptIcons.MODULE_ICON
    }

    override fun getName(): String {
        return "CAOS Project"
    }

    override fun getLogo(): Icon? {
        return icon
    }

    override fun createPeer(): ProjectGeneratorPeer<CaosProjectGeneratorInfo> {
        return CaosProjectGeneratorPeer()
    }

    override fun getDescription(): String? = CaosBundle.message("caos.project.description")

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: CaosProjectGeneratorInfo,
        module: Module
    ) {
        ApplicationManager.getApplication().runWriteAction {
            val modifiableModel: ModifiableRootModel =
                ModifiableModelsProvider.SERVICE.getInstance().getModuleModifiableModel(module)
            module.rootManager.modifiableModel.apply {
                contentEntries.firstOrNull()?.apply {
                    addSourceFolder(baseDir, false)
                }
                commit()
            }
            ModifiableModelsProvider.SERVICE.getInstance().commitModuleModifiableModel(modifiableModel)
        }
    }
}