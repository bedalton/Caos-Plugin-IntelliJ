package com.badahori.creatures.plugins.intellij.agenteering.sprites.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.GuiUtils
import java.awt.Component
import javax.swing.*

object SpriteDumpDialog {
    private val RECENTS_KEY = "com.badahori.creatures.plugins.intellij.agenteering.sprites.RECENT_DUMP_FOLDERS"

    fun create(project:Project, initialLocation:String, multiFile:Boolean = false, folderChangeListener:(path:String?, useChildDirectories:Boolean) -> Unit) : DialogBuilder {
        return object : DialogBuilder(){
            val textField = JTextField()?.apply {
                this.toolTipText = "Sprite dump image destination"
                this.text = initialLocation
                this.alignmentX = Component.LEFT_ALIGNMENT
            }
            val useChildDirectories = JCheckBox("Use child directories based on file name", true).apply {
                this.alignmentX = Component.LEFT_ALIGNMENT
            }
            val comboBox = GuiUtils.constructDirectoryBrowserField(textField, "Choose the sprite dump image destination folder").apply {
                this.alignmentX = Component.LEFT_ALIGNMENT
            }
            override fun setOkOperation(runnable: Runnable?) {
                super.setOkOperation {
                    folderChangeListener(textField.text, useChildDirectories.isSelected)
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
                if (multiFile) {
                    this.add(useChildDirectories)
                }
                this.add(comboBox)
            })
            setOkOperation(null)
        }
    }
}