package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosScriptModuleType
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import icons.CaosScriptIcons
import java.util.*

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class CaosScriptNewFileAction : CreateFileFromTemplateAction(
    /* text = */ CaosBundle.message("caos.actions.new-file.title"),
    /* description = */ CaosBundle.message("caos.actions.new-file.description"),
    /* icon = */ CaosScriptIcons.CAOS_FILE_ICON), DumbAware {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
            ?: return
        if (project.isDisposed) {
            return
        }
        if (DumbService.isDumb(project)) {
            return
        }
        val hasCaosModule = ModuleManager.getInstance(project).modules.any {
            ModuleType.`is`(it, CaosScriptModuleType.INSTANCE)
        } || FilenameIndex.getAllFilesByExt(project, "cos").isNotEmpty()
        e.presentation.isVisible = hasCaosModule
    }
    /**
     * Gets the menu name
     */
    override fun getActionName(p0: PsiDirectory?, p1: String, p2: String?): String =
            CaosBundle.message("caos.actions.new-file.title")

    /**
     * Builds the dialog object
     */
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(CaosBundle.message("caos.actions.new-file.title"))
                .setValidator(object : InputValidatorEx {
                    override fun canClose(inputString: String?) = checkInput(inputString)
                    override fun getErrorText(inputString: String?) = CaosBundle.message("caos.actions.new-file.invalid", inputString.orEmpty())
                    override fun checkInput(inputString: String?) = inputString != null
                })
                .addKind("CAOS File", CaosScriptIcons.CAOS_FILE_ICON, "cos-macro")
    }

    /**
     * Creates the file given a filename and template name
     * @todo implement more than one file type
     */
    override fun createFileFromTemplate(fileName: String, template: FileTemplate, dir: PsiDirectory): PsiFile? = try {
        val className = FileUtilRt.getNameWithoutExtension(fileName)
        val type = when (template.name) {
            else -> "File"
        }
        val project = dir.project
        val properties = createProperties(project, fileName, className, type)
        val attributes = AttributesDefaults(className).withFixedName(true)
        CreateFromTemplateDialog(project, dir, template, attributes, properties)
                .create()
                .containingFile
    } catch (e: Exception) {
        LOG.error("Error while creating new CAOS file", e)
        null
    }

    /**
     * Creates a properties object containing properties passed to the template.
     */
    companion object {
        fun createProperties(project: Project, fileName: String, className: String, type: String): Properties {
            val properties = FileTemplateManager.getInstance(project).defaultProperties
            properties += "NAME" to className
            properties += "FILE_NAME" to fileName
            properties += "TYPE" to type
            return properties
        }
    }
}