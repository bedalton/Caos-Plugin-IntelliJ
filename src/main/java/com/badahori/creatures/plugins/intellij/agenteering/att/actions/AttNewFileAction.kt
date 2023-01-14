package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttAutoFill
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosScriptModuleType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.inferVariantHard
import com.badahori.creatures.plugins.intellij.agenteering.common.MyNewFileAction
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.SpriteLocator
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.EditorUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.bedalton.common.util.FileNameUtil
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FilenameIndex
import icons.CaosScriptIcons

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class AttNewFileAction :  MyNewFileAction(
    CaosBundle.message("att.actions.new-file.title"),
    "ATT file",
    CaosBundle.message("att.actions.new-file.description"),
    "creatures-blank",
    "att",
    CaosScriptIcons.ATT_FILE_ICON
), DumbAware {

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
        }

        val showAction = hasCaosModule ||
                FilenameIndex.getAllFilesByExt(project, "cos").isNotEmpty() ||
                FilenameIndex.getAllFilesByExt(project, "att").isNotEmpty() ||
                BreedSpriteIndex.allFiles(project).isNotEmpty()
        e.presentation.isVisible = showAction
    }

    /**
     * Builds the dialog object
     */
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(CaosBundle.message("caos.actions.new-file.title"))
            .setValidator(object : InputValidatorEx {
                override fun canClose(inputString: String?) = checkInput(inputString)
                override fun getErrorText(inputString: String?) =
                    CaosBundle.message("att.actions.new-file.invalid", inputString.orEmpty())

                override fun checkInput(inputString: String?): Boolean {
                    if (inputString == null) {
                        return false
                    }
                    val fileName = FileNameUtil.getFileNameWithoutExtension(inputString)
                        ?: return false
                    if (!BreedPartKey.isPartName(fileName)) {
                        return false
                    }
                    val extension = FileNameUtil.getExtension(inputString)
                        ?.lowercase()
                        ?: return true
                    return extension == "att"
                }
            })
            .addKind(kind, icon, templateName)
    }

    /**
     * Creates the file given a filename and template name
     */
    override fun createFileFromTemplate(fileName: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        val file =  try {
            super.createFileFromTemplate(fileName, template, dir)
                ?: return null
        } catch (e: Exception) {
            LOG.error("Error while creating new ATT file", e)
            return null
        }
        fill(file)
        return file
    }

    /**
     * Creates a properties object containing properties passed to the template.
     */
    companion object {
        fun fill(file: PsiFile): Boolean {
            val project = file.project
            if (project.isDisposed) {
                return false
            }
            if (!file.isValid) {
                return false
            }
            if (DumbService.isDumb(project)) {
                val pointer = SmartPointerManager.createPointer(file)
                DumbService.getInstance(project).runWhenSmart run@{
                    if (project.isDisposed) {
                        return@run
                    }
                    val element = pointer.element
                        ?: return@run
                    if (element.isValid) {
                        fill(file)
                    }
                }
                return false
            }
            val virtualFile = file.virtualFile
                ?: file.originalFile.virtualFile
                ?: return false
            val document = file.document
                ?: return false
            val variant = file.variant
                ?: file.module?.inferVariantHard()
                ?: project.inferVariantHard()
                ?: return false
            val text = getAutoFill(variant, file.name, virtualFile.parent)
                ?: return false
            return try {
                EditorUtil.insertText(document, text, 0)
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                true
            } catch (e: Exception) {
                LOG.error("Failed to set initial ATT text. ${e.message}", e)
                e.printStackTrace()
                false
            }
        }

        private fun getAutoFill(variant: CaosVariant, fileName: String, directory: VirtualFile): String? {
            val fileNameWithoutExtension = FileNameUtil
                .getFileNameWithoutExtension(fileName)
                ?: fileName
            val spriteFile = SpriteLocator.findClosest(variant, fileNameWithoutExtension, directory)
            val trueVariant = if (spriteFile != null && BreedPartKey.isPartName(spriteFile.nameWithoutExtension)) {
                SpriteParser.getBodySpriteVariant(spriteFile, variant)
            } else {
                variant
            }
            return AttAutoFill.blankAttText(fileName, trueVariant)
        }
    }
}