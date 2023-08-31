package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.common.saveImageWithDialog
import com.badahori.creatures.plugins.intellij.agenteering.utils.copyToClipboard
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.logging.Logger
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import kotlin.math.max


/**
 * Panel to draw the rendered pose with
 */
class PoseRenderedImagePanel(private val project: Project, defaultDirectory: String?) : JPanel() {
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
        if (project.isDisposed) {
            return
        }
        val image = image
        if (image == null) {
            val builder = DialogBuilder()
            builder.setTitle("Pose save error")
            builder.setErrorText("Cannot save un-rendered image")
            builder.show()
            return
        }
        var targetDirectory: File? = null
        val lastDirectory = lastDirectory
        if (lastDirectory != null && lastDirectory.length > 3) {
            targetDirectory = File(lastDirectory)
        }
        val defaultDirectory = defaultDirectory
        if ((targetDirectory == null || !targetDirectory.exists()) && defaultDirectory != null) {
            targetDirectory = File(defaultDirectory)
        }
        if (targetDirectory?.exists() != true) {
            targetDirectory = null
        }
        saveImageWithDialog(
            project,
            "Pose",
            targetDirectory?.path,
            null,
            image
        )
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

}