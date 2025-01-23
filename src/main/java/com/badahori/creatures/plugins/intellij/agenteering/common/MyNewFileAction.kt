package com.badahori.creatures.plugins.intellij.agenteering.common


import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.className
import com.bedalton.common.util.ensureEndsWith
import com.bedalton.common.util.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CAOS_SCRIPT_MODULE_INSTANCE
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
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
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import java.util.*
import javax.swing.Icon

/**
 * Creates a file
 */
abstract class MyNewFileAction(
    protected val title: String,
    protected val kind: String,
    protected val description: String,
    protected val templateName: String,
    protected val extension: String,
    protected val icon: Icon?
) : CreateFileFromTemplateAction(title, description, icon), DumbAware {

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
            ModuleType.`is`(it, CAOS_SCRIPT_MODULE_INSTANCE)
        } || FilenameIndex.getAllFilesByExt(project, "cos").isNotEmpty()
        e.presentation.isVisible = hasCaosModule
    }
    /**
     * Gets the menu name
     */
    override fun getActionName(p0: PsiDirectory?, p1: String, p2: String?): String = title

    /**
     * Builds the dialog object
     */
    abstract override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder)
    /**
     * Creates the file given a filename and template name
     * @todo implement more than one file type
     */
    override fun createFileFromTemplate(fileName: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        return  try {
            val actualFileName = PathUtil.getLastPathComponent(fileName) ?: fileName
            val type = when (template.name) {
                else -> "File"
            }
            val project = dir.project
            var theDir = dir
            val relativePath = PathUtil.getWithoutLastPathComponent(fileName)
                ?.nullIfEmpty()
                ?.ensureEndsWith('/')
            if (relativePath != null) {
                val parts = relativePath.split('/')
                for (part in parts) {
                    val partTrimmed = part.trim()
                        .nullIfEmpty()
                        ?: continue
                    try {
                        theDir = theDir.findSubdirectory(partTrimmed)
                            ?: theDir.createSubdirectory(partTrimmed)
                    } catch (e: Exception) {
                        e.rethrowAnyCancellationException()
                        CaosNotifications.showError(project, title, "Invalid path component \"$partTrimmed\" for parent directory\n${e.className}::${e.message}")
                        return null
                    }
                }
            }
            val properties = createProperties(project, fileName, type)
            val attributes = AttributesDefaults(actualFileName).withFixedName(true)
            CreateFromTemplateDialog(project, theDir, template, attributes, properties)
                .create()
                .containingFile
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            LOG.error("Error while creating new $kind", e)
            null
        }
    }

    /**
     * Creates a properties object containing properties passed to the template.
     */
    companion object {
        fun createProperties(project: Project, fileName: String, type: String): Properties {
            val properties = FileTemplateManager.getInstance(project).defaultProperties
            properties += "NAME" to fileName
            properties += "FILE_NAME" to fileName
            properties += "TYPE" to type
            return properties
        }
    }
}