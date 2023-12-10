package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.collectChildren
import com.bedalton.common.structs.Pointer
import com.bedalton.log.Log
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.max

class SetVariantAction : AnAction(
    CaosBundle.message("caos.actions.set-variant"),
    CaosBundle.message("caos.actions.set-variant.description"),
    CaosScriptIcons.SDK_ICON
) {

    override fun actionPerformed(e: AnActionEvent) {
        val files = e.files
        val project = e.project

        if (project != null && !project.isDisposed) {
            setVariant(project, files)
        } else {
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
        e.presentation.isEnabledAndVisible = isVisible(e)
    }

    private fun isVisible(e:AnActionEvent): Boolean {
        if (e.project?.isDisposed != false) {
            return false
        }
        for (file in e.files) {
            if (isOrHasCaosFile(file)) {
                return true
            }
        }
        return false
    }
}

private val expectedExtensions = listOf(
    "ps",
    "cos",
    "att",
    "spr",
    "s16",
    "c16",
    "blk"
)

private const val MAX_FILES_CHECKED = 15
private const val MAX_LEVELS = 2
private const val LEVEL_CHECK_MOD = MAX_FILES_CHECKED / (MAX_LEVELS + 1)
private fun isOrHasCaosFile(file: VirtualFile): Boolean {
    return isOrHasCaosFile(file, 0, Pointer(0))
}
private fun isOrHasCaosFile(file: VirtualFile, level: Int, count: Pointer<Int>): Boolean {
    count.value += 1
    if (count.value > MAX_FILES_CHECKED) {
        return true
    }
    if (!file.isDirectory) {
        return file.extension?.lowercase() in expectedExtensions
    }

    // Ensure not searching too deep
    val childLevel = level + 1
    if (childLevel > MAX_LEVELS) {
        return false
    }

    // Do not search children for as long
    val childStartCount = childLevel * LEVEL_CHECK_MOD
    val thisCounter = Pointer(childStartCount)

    for (child in file.children.orEmpty()) {
        if (isOrHasCaosFile(file, level = childLevel, thisCounter)) {
            return true
        }
        if (thisCounter.value > MAX_FILES_CHECKED) {
            break
        }
    }
    return false
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
            if (file.cachedVariantExplicitOrImplicit != variant) {
                Log.e { "Failed to set variant to $variant; Found: ${file.cachedVariantExplicitOrImplicit}" }
            }
            if (file.fileType == CaosScriptFileType.INSTANCE) {
                val psiFile = file.getPsiFile(project)
                (psiFile as? CaosScriptFile)?.setVariant(variant, true)
            } else if (file.fileType == AttFileType) {
                val psiFile = file.getPsiFile(project)
                (psiFile as? AttFile)?.setVariant(variant, true)
            } else if (file.isDirectory) {
                file.collectChildren() { child ->
                    child.extension like "cos" || child.extension like "att"
                }.forEach { child ->
                    child.setCachedVariant(variant, false)
                }
            }
        }
        builder.dialogWrapper.close(0)
    }
    builder.setCancelOperation {
        builder.dialogWrapper.close(0)
    }
    return builder.showAndGet()
}