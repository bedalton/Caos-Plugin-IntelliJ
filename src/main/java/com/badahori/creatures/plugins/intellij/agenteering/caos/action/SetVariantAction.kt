package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.IS_OR_HAS_CAOS_FILES_DATA_KEY
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isOrHasCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.common.updatePresentation
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.collectChildren
import com.bedalton.common.util.formatted
import com.bedalton.log.Log
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JLabel

class SetVariantAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!e.presentation.isEnabled) {
            return
        }
        val files = e.files
        val project = e.project

        if (project != null && !project.isDisposed) {
            Log.e("Project is not disposed. Showing variant select popup")
            setVariant(project, files)
        } else {
            Log.i("Project is null or disposed. Showing error")
            showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        var builder = DialogBuilder()

        // Set label
        builder.setCenterPanel(JLabel("Failed to set variant for files\nProject has been disposed"))

        LOGGER.severe("Failed to set variant. Project was null")

        // Set okay action
        builder = builder.okActionEnabled(true)
        builder.setOkOperation {
            builder.dialogWrapper.close(0)
        }

        // Show dialog
        builder.showAndGet()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        Log.i("Updating set variant action")
        invokeLater {
            val valid = try {
                isVisible(e)
            } catch (e: Throwable) {
                e.rethrowAnyCancellationException()
                Log.e { "Failed check to see if SetVariantAction should run; ${e.formatted()}" }
                true
            }
            Log.i("IsVisible: $valid")
            updatePresentation(e) {
                this.isEnabled = valid
            }
        }
    }

    private fun isVisible(e: AnActionEvent): Boolean {
        if (e.getData(IS_OR_HAS_CAOS_FILES_DATA_KEY) == true) {
            return true
        }

        val project = e.project
            ?: return true

        if (project.isDisposed) {
            LOGGER.severe("Project ${e.project?.name} is already disposed; ProjectNotNull: ${e.project != null}")
            return false
        }

        if (DumbService.isDumb(project)) {
            return true
        }

        if (e.files.isEmpty()) {
            return true
        }

        return runReadAction caosFile@{
            for (file in e.files) {
                try {
                    if (isOrHasCaosFile(file)) {
                        return@caosFile true
                    }
                } catch (e: Throwable) {
                    e.rethrowAnyCancellationException()
                    return@caosFile true
                }
            }
            return@caosFile false
        }
    }
}

private fun setVariant(project: Project, files: Array<VirtualFile>): Boolean {

    val variant = askUserForVariant(project)
        ?: return false

    for (file in files) {
        file.setCachedVariant(variant, true)
        if (file is CaosVirtualFile) {
            file.setVariant(variant, true)
        }
        if (file.cachedVariantExplicitOrImplicit != variant) {
            Log.e { "Failed to set variant to $variant; Found: ${file.cachedVariantExplicitOrImplicit}" }
        }
        when {
            file.fileType == CaosScriptFileType.INSTANCE -> {
                val psiFile = file.getPsiFile(project)
                (psiFile as? CaosScriptFile)?.setVariant(variant, true)
            }

            file.fileType == AttFileType -> {
                val psiFile = file.getPsiFile(project)
                (psiFile as? AttFile)?.setVariant(variant, true)
            }

            file.isDirectory -> {
                file.collectChildren { child ->
                    child.extension like "cos" || child.extension like "att"
                }.forEach { child ->
                    child.setCachedVariant(variant, false)
                }
            }
        }
    }
    return true
}