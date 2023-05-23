package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import java.awt.Component
import javax.swing.*

object WineExecutableSelector {
    private val RECENTS_KEY = "com.badahori.creatures.plugins.intellij.agenteering.injector.RECENT_WINE_FOLDERS"

    fun create(
        project: Project,
        initialLocation: String,
        isWin32: Boolean,
        folderChangeListener: (path: String?) -> Unit
    ): DialogBuilder {
        val wine = if (isWin32) "Wine 32" else "Wine 64"
        val browseButton = TextFieldWithBrowseButton().apply {
            this.alignmentX = Component.LEFT_ALIGNMENT
            this.addBrowseFolderListener(
                "Choose $wine Executable",
                "Choose the wine executable which is running Creatures",
                project,
                FileChooserDescriptor(
                    false,
                    true,
                    false,
                    false,
                    false,
                    false
                ),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            )
            this.textField.text = initialLocation
        }

        return object : DialogBuilder() {
            override fun setOkOperation(runnable: Runnable?) {
                super.setOkOperation {
                    folderChangeListener(browseButton.textField.text)
                    runnable?.run()
                    this.dialogWrapper.close(0)
                }
            }
        }.apply {
            setCenterPanel(JPanel().apply {
                this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
                this.alignmentX = Component.LEFT_ALIGNMENT
                this.add(JLabel("Choose $wine Executable").apply {
                    this.alignmentX = Component.LEFT_ALIGNMENT
                })
                this.add(browseButton)
            })
            setOkOperation(null)
        }
    }
}