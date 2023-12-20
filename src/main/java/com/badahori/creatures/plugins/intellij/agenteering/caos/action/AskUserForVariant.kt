package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import javax.swing.JLabel
import javax.swing.JPanel

internal fun askUserForVariant(project: Project): CaosVariant? {
    val variantSelect = ComboBox(
        arrayOf(
            "C1",
            "C2",
            "CV",
            "C3",
            "DS"
        )
    )
    var selectedVariant: CaosVariant? = null
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
        selectedVariant = CaosVariant
            .fromVal(variantString)
            .nullIfNotConcrete()
            ?: return@ok
        builder.dialogWrapper.close(0)
    }
    builder.setCancelOperation {
        builder.dialogWrapper.close(1)
    }
    return if (builder.showAndGet()) {
        selectedVariant
    } else {
        null
    }
}