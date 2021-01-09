package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.copyForBobCob
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CopyAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class ExpandCaosCommands : CopyAction(), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
            ?: return

        val caosFiles = event.files.flatMap { file ->
            getCaosFiles(project, file)
        }
        caosFiles.forEach { file ->
            CaosScriptExpandCommasIntentionAction.invoke(project, file)
        }
    }

    private fun getCaosFiles(project:Project, file:VirtualFile) : List<CaosScriptFile> {
        if (file.isDirectory) {
            return file.children.flatMap { child -> getCaosFiles(project, child) }
        }
        return (file.getPsiFile(project) as? CaosScriptFile)?.let { script ->
            listOf(script)
        } ?: emptyList()
    }


    private fun hasCaos(file:VirtualFile) : Boolean {
        return if (file.isDirectory) {
            file.children.any { child ->
                hasCaos(child)
            }
        } else {
            file.fileType == CaosScriptFileType.INSTANCE
        }
    }


    override fun update(event: AnActionEvent) {
        val enabled = event.files.any { file ->
            hasCaos(file)
        }
        val presentation = event.presentation
        presentation.isEnabled = enabled
        presentation.isVisible = enabled
        presentation.text = CaosBundle.message("caos.actions.expand-caos-commands-in-file.title")
        presentation.description = CaosBundle.message("caos.actions.expand-caos-commands-in-file.description")
        presentation.icon = AllIcons.Actions.Copy
    }
}

val AnActionEvent.files get() = dataContext.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: emptyArray()