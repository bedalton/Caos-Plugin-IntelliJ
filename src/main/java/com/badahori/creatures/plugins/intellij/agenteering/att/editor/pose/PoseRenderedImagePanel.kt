package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.common.saveImageWithDialog
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosBalloonNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.PANEL_TRANSPARENT_BLACK
import com.badahori.creatures.plugins.intellij.agenteering.utils.copyToClipboard
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.trim
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
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
    private val popUp by lazy {
        PopUp()
    }

    init {
        initHandlers()
        isOpaque = false
        background = PANEL_TRANSPARENT_BLACK
    }

    private fun initHandlers() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    showPopUp(e)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
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

    fun setInvalid(invalid: Boolean) {
        mInvalid = invalid
        repaint()
    }

    private fun showPopUp(e: MouseEvent) {
        if (e.isPopupTrigger || ((e.modifiersEx and KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK && e.button == MouseEvent.BUTTON1)) {
            popUp.show(e.component, e.x, e.y)
        }
    }

    fun saveImageAs() {
        if (project.isDisposed) {
            return
        }
        val image = cropped() ?: image

        if (image == null) {
            CaosBalloonNotifications.showError(
                project,
                "Save Image Error",
                "Failed to get image to save"
            )
            return
        }

        val lastDirectory = lastDirectory
        val defaultDirectory = defaultDirectory

        var targetDirectory: File? = null

        if (lastDirectory != null && lastDirectory.length > 3) {
            targetDirectory = File(lastDirectory)
        }

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
        val image = cropped() ?: image
        if (image == null) {
            CaosBalloonNotifications.showError(
                project,
                "Copy Image Error",
                "Failed to copy image to clipboard"
            )
            return
        }
        image.copyToClipboard()
    }


    @Suppress("UndesirableClassUsage", "UseJBColor")
    private fun cropped(): BufferedImage? {
        return image?.trim()
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
            max(minSize!!.width, image.width),
            max(minSize!!.width, image.height)
        )
        preferredSize = size
        minimumSize = minSize
        invokeLater {
            revalidate()
            repaint()
        }
    }

    override fun getPreferredSize(): Dimension {
        val preferredSize = super.getPreferredSize()
        val image = image
            ?: return preferredSize
        val size = Dimension(
            max(minSize!!.width, image.width),
            max(minSize!!.width, image.height)
        )
        return size
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.color = IMAGE_BACKGROUND
        g2d.clearRect(0, 0, width, height)
        g2d.fillRect(0,0, width, height)
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
    companion object {
        val IMAGE_BACKGROUND = JBColor.PanelBackground
    }

}