package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.intellij.openapi.editor.colors.EditorColorsManager
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Image
import java.awt.Toolkit.getDefaultToolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


/**
 * Cell renderer for individual sprite image in Sprite file list
 */
internal class SpriteCellRenderer : ListCellRenderer<ImageTransferItem> {
    var color: Color = EditorColorsManager.getInstance().globalScheme.defaultBackground
    var fontColor = EditorColorsManager.getInstance().globalScheme.defaultForeground
    var scale = 1.0
    override fun getListCellRendererComponent(list: JList<out ImageTransferItem>?, value: ImageTransferItem?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        if (value == null) {
            return JPanel().apply {
                isVisible = false
            }
        }
        val panel = JPanel()
        panel.addMouseListener(DragMouseAdapter)

        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        val textBox = JLabel("$index.")
        panel.border = BorderFactory.createEmptyBorder(5, 16, 10, 5)
        if (isSelected) {
            panel.background = EditorColorsManager.getInstance().globalScheme.defaultForeground
            panel.foreground = EditorColorsManager.getInstance().globalScheme.defaultBackground
            textBox.foreground = EditorColorsManager.getInstance().globalScheme.defaultBackground
        } else {
            panel.background = EditorColorsManager.getInstance().globalScheme.defaultBackground
            panel.foreground = EditorColorsManager.getInstance().globalScheme.defaultForeground
            textBox.foreground = EditorColorsManager.getInstance().globalScheme.defaultForeground
        }
        panel.actionMap.apply {
            put(TransferHandler.getCopyAction().getValue(Action.NAME),
                    TransferHandler.getCopyAction())
        }

        panel.add(textBox)
        val imageValue = value.image
        val image = if (scale > 1.01) {
            val width = imageValue.width * scale
            val height = imageValue.height * scale
            imageValue.getScaledInstance(width.toInt(), height.toInt(), Image.SCALE_AREA_AVERAGING) // scale it the smooth way
        } else {
            imageValue
        }
        val imageIcon = ImageIcon(image).apply {
            description = "Sprite image $index"
        }
        val imageView = JLabel(imageIcon)
        //imageView.setBounds(0,0,((value as BufferedImage) * scale).width, (value as BufferedImage).height)
        //imageView.minimumSize = Dimension((value as BufferedImage).width, (value as BufferedImage).height)
        textBox.labelFor = imageView
        imageView.border = BorderFactory.createLineBorder(fontColor.brighter().brighter(), 1)
        panel.add(imageView)
        if (color.alpha > 0) {
            imageView.background = color
            imageView.isOpaque = true
        }

        return panel
    }
}

private object DragMouseAdapter : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
        val c = e.source as JComponent
        val handler = c.transferHandler
        handler.exportAsDrag(c, e, TransferHandler.COPY)
    }
}