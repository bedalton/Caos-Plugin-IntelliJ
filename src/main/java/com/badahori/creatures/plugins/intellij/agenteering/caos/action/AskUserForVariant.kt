package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.inferVariantHard
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.panels.VerticalBox
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

internal fun askUserForVariant(project: Project, files: Array<VirtualFile>): CaosVariant? {
    return askUserForVariant(project, files, false)?.first
}

internal fun askUserForVariant(project: Project, files: Array<VirtualFile>, withClear: Boolean): Pair<CaosVariant, Boolean?>? {
    val variantSelect = ComboBox(
        arrayOf(
            "C1",
            "C2",
            "CV",
            "C3",
            "DS",
            "Inherit"
        )
    )

    var selectedVariant: CaosVariant? = null
    var panel: JComponent = JPanel()
    panel.add(JLabel("Set CAOS variant for files: "))
    panel.add(variantSelect)
    variantSelect.selectedItem = findFilesVariant(project, files)?.code?.uppercase() ?: "DS"

    val clear = JBCheckBox("Clear manual file variants", true)
    if (withClear) {
        val component = VerticalBox()
        component.add(panel)
        panel = component
        panel.add(clear)
    }
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
        selectedVariant = if ("Inherit" like variantString) {
            CaosVariant.UNKNOWN
        } else {
            CaosVariant
                .fromVal(variantString)
                .nullIfNotConcrete()
                ?: return@ok
        }
        builder.dialogWrapper.close(0)
    }
    builder.setCancelOperation {
        builder.dialogWrapper.close(1)
    }
    return if (builder.showAndGet() && selectedVariant != null) {
        if (withClear) {
            Pair(selectedVariant!!, clear.isSelected)
        } else {
            Pair(selectedVariant!!, null)
        }
    } else {
        null
    }
}

private fun findFilesVariant(project: Project, files: Array<VirtualFile>): CaosVariant? {
    files.mapNotNull { ModuleUtilCore.findModuleForFile(it, project)?.inferVariantHard() }
        .singleOrNull()?.let {
            return it
        }

    if (files.size == 1) {
        return files[0].inferVariantHard(project, true)
    }

    val filesAndVariants = files.mapNotNull { file ->
        file.inferVariantHard(project, false)?.let { variant ->
            Pair(file, variant)
        }
    }

    val filesByVariant = filesAndVariants.map { it.second }
        .distinct()
        .map { variant ->
            val files = filesAndVariants.filter { variant == it.second }
            variant to files
        }
    return filesByVariant
        .minByOrNull {
            it.second.count()
        }
        ?.first
}