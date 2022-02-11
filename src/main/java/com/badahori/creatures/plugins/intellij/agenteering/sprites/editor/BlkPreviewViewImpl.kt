package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkSpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.copyToClipboard
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*


/**
 * Sprite viewer (eventually will be editor) for various Creatures file types
 */
internal class BlkPreviewViewImpl(project: Project?, file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val myFile: VirtualFile = file
    private val myProject: Project? = project

    private lateinit var mComponent: JScrollPane
    private lateinit var mImage: BufferedImage
    private var isLoading = AtomicBoolean()
    private lateinit var loadingLabel: JLabel


    init {

    }


    override fun getComponent(): JScrollPane {
        if (this::mComponent.isInitialized)
            return mComponent
        val component = JBScrollPane()
        loadingLabel = object: JLabel("Stitching BLK...", SwingConstants.CENTER) {
            override fun getPreferredSize(): Dimension {
                return component.size
            }
        }
        component.setViewportView(loadingLabel)

        mComponent = component
        return component
    }

    private fun setImage(image: BufferedImage) {
        mImage = image
        val size = Dimension(image.width, image.height)
        val component = component
        component.verticalScrollBar.unitIncrement = 16
        component.horizontalScrollBar.unitIncrement = 16
        val label = ImagePanel(image, myFile.path)
        label.size = size
        label.minimumSize = size
        label.preferredSize = size
        component.remove(loadingLabel)
        component.setViewportView(label)
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return NAME
    }

    override fun getFile(): VirtualFile {
        return myFile
    }

    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return myFile.isValid
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun deselectNotify() {

    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun dispose() {}

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }

    override fun selectNotify() {
        stitchImage()
    }

    private fun stitchImage() {
        if (isLoading.getAndSet(true)) {
            return
        }

        if (this::mImage.isInitialized) {
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val (stitched, didDrawAll) = BlkSpriteFile(myFile).getStitched().get()
                if (!didDrawAll && myProject != null) {
                    ApplicationManager.getApplication().invokeLater {
                        CaosNotifications.showWarning(
                            myProject,
                            "BLK Preview",
                            "Failed to draw all cells in BLK preview pane"
                        )
                    }
                }
                setImage(stitched)
            } catch (e:Exception) {
                component.setViewportView(JLabel("Failed to decompile sprite for stitching"))
            }
        }
    }

    companion object {
        private const val NAME = "Stitched Preview"
    }
}


private class ImagePanel(val mImage: BufferedImage, defaultDirectory: String?) : JLabel(ImageIcon(mImage)) {
    private val defaultDirectory: String? = (defaultDirectory ?: System.getProperty("user.home")).nullIfEmpty()?.let {
        if (File(it).exists())
            it
        else
            null
    }
    var lastDirectory: String? = null
    private val popUp = PopUp()

    private fun initHandlers() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    showPopUp(e)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    showPopUp(e)
                }
            }
        })
    }

    private fun showPopUp(e: MouseEvent) {
        if (e.isPopupTrigger || e.modifiersEx or KeyEvent.CTRL_DOWN_MASK == KeyEvent.CTRL_DOWN_MASK && e.button == 1) {
            popUp.show(e.component, e.x, e.y)
        }
    }

    fun saveImageAs() {
        val image = mImage
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Specify a file to save"
        var targetDirectory: File? = null
        val lastDirectory = lastDirectory
        if (lastDirectory != null && lastDirectory.length > 3) {
            targetDirectory = File(lastDirectory)
        }

        if ((targetDirectory == null || !targetDirectory.exists()) && defaultDirectory != null) {
            targetDirectory = File(defaultDirectory)
        }
        if (targetDirectory?.exists() == true) {
            fileChooser.currentDirectory = targetDirectory
        }
        val userSelection = fileChooser.showSaveDialog(this)
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return
        }
        var outputFileTemp = fileChooser.selectedFile
        val extension = FileNameUtils.getExtension(outputFileTemp.name)
        if (extension == null || !extension.equals("png", ignoreCase = true)) {
            outputFileTemp = File(outputFileTemp.path + ".png")
        }
        this.lastDirectory = outputFileTemp.parent
        val outputFile = outputFileTemp
        ApplicationManager.getApplication().runWriteAction {
            if (!outputFile.exists()) {
                var didCreate = false
                try {
                    didCreate = outputFile.createNewFile()
                } catch (ignored: IOException) {
                }
                if (!didCreate) {
                    val builder = DialogBuilder()
                    builder.setTitle("Pose save error")
                    builder.setErrorText("Failed to create pose file '" + outputFile.name + "' for writing")
                    builder.show()
                    return@runWriteAction
                }
            }
            try {
                FileOutputStream(outputFile).use { outputStream ->
                    var bytes: ByteArray? = null
                    try {
                        bytes = image.toPngByteArray()
                    } catch (e: AssertionError) {
                        LOGGER.severe(e.localizedMessage)
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (bytes == null || bytes.size < 20) {
                        val builder = DialogBuilder()
                        builder.setTitle("Pose save error")
                        builder.setErrorText("Failed to prepare rendered pose for writing")
                        builder.show()
                        return@runWriteAction
                    }
                    outputStream.write(bytes)
                }
            } catch (e: IOException) {
                val builder = DialogBuilder()
                builder.setTitle("Pose save error")
                builder.setErrorText("Failed to save pose image to '" + outputFile.path + "'")
                builder.show()
            }
            val thisFile =
                VfsUtil.findFileByIoFile(outputFile.parentFile, true)
            if (thisFile != null && thisFile.parent != null) {
                thisFile.parent.refresh(false, true)
            }
        }
    }


    fun copyToClipboard() {
        mImage.copyToClipboard()
    }


    inner class PopUp : JPopupMenu() {
        init {
            var item = JMenuItem("Save image as..")
            item.addActionListener { saveImageAs() }
            add(item)
            item = JMenuItem("Copy image to clipboard")
            item.addActionListener { copyToClipboard() }
            add(item)
        }
    }

    init {
        initHandlers()
    }
}