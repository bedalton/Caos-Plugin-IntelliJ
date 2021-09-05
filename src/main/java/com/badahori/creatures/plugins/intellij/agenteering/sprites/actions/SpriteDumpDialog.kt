package com.badahori.creatures.plugins.intellij.agenteering.sprites.actions

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import java.awt.Component
import javax.swing.*

object SpriteDumpDialog {
    private val RECENTS_KEY = "com.badahori.creatures.plugins.intellij.agenteering.sprites.RECENT_DUMP_FOLDERS"

    fun create(
        project: Project,
        initialLocation: String,
        multiFile: Boolean = false,
        folderChangeListener: (path: String?, useChildDirectories: Boolean) -> Unit
    ): DialogBuilder {
        val useChildDirectories = JCheckBox("Use child directories based on file name", true).apply {
            this.alignmentX = Component.LEFT_ALIGNMENT
        }

        val browseButton = TextFieldWithBrowseButton().apply {
            this.alignmentX = Component.LEFT_ALIGNMENT
            this.addBrowseFolderListener(
                "Sprite Dump Destination Directory",
                "Choose the sprite dump image destination folder",
                project,
                FileChooserDescriptor(
                    false,
                    true,
                    false,
                    false,
                    false,
                    multiFile
                ),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            )
            this.textField.text = initialLocation
        }

        return object : DialogBuilder() {
            override fun setOkOperation(runnable: Runnable?) {
                super.setOkOperation {
                    folderChangeListener(browseButton.textField.text, useChildDirectories.isSelected)
                    runnable?.run()
                    this.dialogWrapper.close(0)
                }
            }
        }.apply {
            setCenterPanel(JPanel().apply {
                this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
                this.alignmentX = Component.LEFT_ALIGNMENT
                this.add(JLabel("Choose a sprite dump destination").apply {
                    this.alignmentX = Component.LEFT_ALIGNMENT
                })
                this.add(browseButton)
                if (multiFile) {
                    this.add(useChildDirectories)
                }
            })
            setOkOperation(null)
        }
    }
}