package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import bedalton.creatures.sprite.parsers.BlkSpriteFile
import bedalton.creatures.sprite.util.SpriteType
import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.editor.SpriteEditorImpl.Companion.cache
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.parse
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
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
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap32
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
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.floor


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

    override fun getComponent(): JScrollPane {
        if (this::mComponent.isInitialized)
            return mComponent
        val component = JBScrollPane()
        loadingLabel = object : JLabel("Stitching BLK...", SwingConstants.CENTER) {
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
            isLoading.set(false)
            return
        }

        SpriteEditorImpl.fromCacheAsAwt(file)?.getOrNull(0)?.let { image ->
            setImage(image)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            stitchActual()
        }
    }

    private fun stitchActual() {
        val rawImages = try {
            getRawImages()
        } catch (e: Exception) {
            val error = if (e.message?.length.orElse(0) > 0) {
                "${e::className}: ${e.message}"
            } else {
                "${e::className}"
            }
            showError(myProject, "Failed to parse raw BLK frames. $error")
            return
        }

        try {
            val stitched = stitch(rawImages)
            cache(file, listOf(stitched))
            setImage(stitched.toAwt())
        } catch (e: Exception) {
            val error = if (e.message?.length.orElse(0) > 0) {
                "${e::className}: ${e.message}"
            } else {
                "${e::className}"
            }
            LOGGER.severe(error)
            e.printStackTrace()
            showError(myProject, error)
        } finally {
            isLoading.set(false)
        }
    }

    private fun stitch(rawImages: List<Bitmap32>): Bitmap32 {
        val stream = VirtualFileStreamReader(file)
        val (cellsWide, cellsHigh) = try {
            stream.position(0)
            BlkSpriteFile.sizeInCells(stream)
        } finally {
            if (!stream.closed) {
                stream.close()
            }
        }
        val mod5 = AtomicInteger(0)
        return BlkSpriteFile.getStitched(cellsWide, cellsHigh, rawImages) { i, total ->
            val progress = ceil(i * 100.0 / total)
            if (progress > mod5.toDouble() * 5) {
                mod5.set(floor(progress / 5.0).toInt())
                invokeLater {
                    loadingLabel.text = "Stitching BLK... " + progress.toInt() + "%"
                }
            }
        }
    }

    private fun getRawImages(): List<Bitmap32> {
        val stream = VirtualFileStreamReader(file)
        try {
            val holder = parse(SpriteType.BLK, stream, 5) { i: Int, total: Int ->
                val progress = ceil((i * 100.0) / total)
                invokeLater {
                    loadingLabel.text = "Parsing BLK... " + progress.toInt() + "%      $i/$total"
                }
                null
            }
            return holder.bitmaps
        } finally {
            if (!stream.closed) {
                stream.close()
            }
        }
    }

    private fun showError(myProject: Project?, error: String) {
        ApplicationManager.getApplication().invokeLater {
            if (myProject == null || myProject.isDisposed) {
                return@invokeLater
            }
            CaosNotifications.showWarning(
                myProject,
                "BLK Preview",
                "Failed to draw all cells in BLK preview pane"
            )
            component.setViewportView(JLabel("Failed to decompile sprite for stitching. $error"))
            isLoading.set(false)
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
        val extension = FileNameUtil.getExtension(outputFileTemp.name)
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
                    builder.setErrorText("Failed to create stitched BLK file '" + outputFile.name + "' for writing")
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
                        builder.setErrorText("Failed to prepare stitched BLK for writing")
                        builder.show()
                        return@runWriteAction
                    }
                    outputStream.write(bytes)
                }
            } catch (e: IOException) {
                val builder = DialogBuilder()
                builder.setTitle("Pose save error")
                builder.setErrorText("Failed to save stitched BLK image to '" + outputFile.path + "'")
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