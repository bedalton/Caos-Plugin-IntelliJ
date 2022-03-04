package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.asWritable
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SmartPointerManager

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class ExpandCaosCommandsAction : AnAction(), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
            ?: return

        val caosFiles = event.files.flatMap { file ->
            getCaosFiles(project, file)
        }
        caosFiles.forEach each@{ rawFile ->
            // Store pointer to file for use in lambda
            val pointer = SmartPointerManager.createPointer(rawFile)
            val virtualFile = rawFile.virtualFile

            // Temporarily make virtual file writable
            virtualFile.asWritable {
                // Make sure project has not been disposed during loop
                if (project.isDisposed) {
                    return
                }

                // Retrieve pointer to file in case anything may have broken original reference
                val file = pointer.element
                    ?: return@each

                // Ensure file is still valid from pointer
                if (!file.isValid ) {
                    return@each
                }
                // Apply the expand-commas action
                CaosScriptExpandCommasIntentionAction.invoke(project, file)
            }
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
    }
}