package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.copyToClipboard
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.logging.Logger
import javax.swing.JFileChooser
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import kotlin.math.max


/**
 * Panel to draw the rendered pose with
 */
class PoseRenderedImagePanel(defaultDirectory: String?) : JPanel() {
    private val defaultDirectory: String? = (defaultDirectory ?: System.getProperty("user.home")).nullIfEmpty()?.let {
        if (File(it).exists())
            it
        else
            null
    }
    private var mInvalid = false
    private var image: BufferedImage? = null
    private var minSize: Dimension? = null
    var lastDirectory: String? = null
    private val popUp = PopUp()
    private fun initHandlers() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopUp(e)
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3)
                    showPopUp(e)
            }
        })
    }

    fun setInvalid(invalid: Boolean) {
        mInvalid = invalid
        repaint()
    }

    private fun showPopUp(e: MouseEvent) {
        if (e.isPopupTrigger || e.modifiersEx or KeyEvent.CTRL_DOWN_MASK == KeyEvent.CTRL_DOWN_MASK && e.button == 1) {
            popUp.show(e.component, e.x, e.y)
        }
    }

    fun saveImageAs() {
        val image = image
        if (image == null) {
            val builder = DialogBuilder()
            builder.setTitle("Pose save error")
            builder.setErrorText("Cannot save un-rendered image")
            builder.show()
            return
        }
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Specify a file to save"
        var targetDirectory: File? = null
        val lastDirectory = lastDirectory
        if (lastDirectory != null && lastDirectory.length > 3) {
            targetDirectory = File(lastDirectory)
        }
        val defaultDirectory = defaultDirectory
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
        if (image == null) {
            return
        }
        image!!.copyToClipboard()
    }

    fun clear() {
        image = null
        revalidate()
        repaint()
    }

    fun updateImage(
        image: BufferedImage,
    ) {
        mInvalid = false
        this.image = image
        if (minSize == null) {
            minSize = size
        }
        val size = Dimension(
            max(minSize!!.width, image.width), max(
                minSize!!.width, image.height
            )
        )
        preferredSize = size
        minimumSize = minSize
        invokeLater {
            revalidate()
            repaint()
        }
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.clearRect(0, 0, width, height)
        val image = this.image
            ?: return
        g2d.translate(width / 2, height / 2)
        g2d.translate(-image.getWidth(null) / 2, -image.getHeight(null) / 2)
        g2d.drawImage(image, 0, 0, null)
    }

    internal inner class PopUp : JPopupMenu() {
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

    companion object {
        private val LOGGER = Logger.getLogger("#PoseRenderedImagePanel")
    }
}