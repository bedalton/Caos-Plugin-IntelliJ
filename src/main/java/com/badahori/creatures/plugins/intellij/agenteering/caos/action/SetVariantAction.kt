package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.collectChildren
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.JLabel
import javax.swing.JPanel

class SetVariantAction : AnAction(
    CaosBundle.message("caos.actions.set-variant"),
    CaosBundle.message("caos.actions.set-variant.description"),
    CaosScriptIcons.SDK_ICON
) {
    override fun actionPerformed(e: AnActionEvent) {
        val files = e.files
        val project = e.project
        if (project == null) {
            var builder = DialogBuilder()
            builder.setCenterPanel(JLabel("Failed to set variant for files"))
            LOGGER.severe("Failed to set variant. Project was null")
            builder = builder.okActionEnabled(true)
            builder.setOkOperation {
                builder.dialogWrapper.close(0)
            }
            builder.showAndGet()
            return
        }
        setVariant(project, files)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val hasCaosFiles = e.files.any { it.collectChildren { it.fileType == CaosScriptFileType.INSTANCE }.isNotEmpty() }
        e.presentation.isEnabled = hasCaosFiles
        e.presentation.isVisible = hasCaosFiles
    }
}


private fun setVariant(project: Project, files: Array<VirtualFile>): Boolean {
    val variantSelect = ComboBox(
        arrayOf(
            "C1",
            "C2",
            "CV",
            "C3",
            "DS"
        )
    )
    val panel = JPanel()
    panel.add(JLabel("Set CAOS variant for files: "))
    panel.add(variantSelect)
    variantSelect.selectedItem = "DS"
    var builder = DialogBuilder()
    builder = builder.centerPanel(panel)
    builder = builder.okActionEnabled(true)
    builder.addOkAction()
    builder.addCancelAction()
    builder.setOkOperation ok@{
        val variantString = variantSelect.selectedItem as? String
        if (variantString.isNullOrBlank()) {
            return@ok
        }
        val variant = CaosVariant.fromVal(variantString).nullIfNotConcrete()
            ?: return@ok
        for (file in files) {
            file.setCachedVariant(variant, true)
            if (file is CaosVirtualFile) {
                file.setVariant(variant, true)
            }
            if (file.fileType == CaosScriptFileType.INSTANCE) {
                val psiFile = file.getPsiFile(project)
                (psiFile as? CaosScriptFile)?.setVariant(variant, true)
            } else if (file.fileType == AttFileType) {
                val psiFile = file.getPsiFile(project)
                (psiFile as? AttFile)?.setVariant(variant, true)
            }
        }
        builder.dialogWrapper.close(0)
    }
    builder.setCancelOperation {
        builder.dialogWrapper.close(0)
    }
    return builder.showAndGet()
}