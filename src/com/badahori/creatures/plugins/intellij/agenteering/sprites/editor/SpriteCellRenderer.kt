package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.openapi.editor.colors.EditorColorsManager
import java.awt.Color
import java.awt.Component
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.*

class SpriteCellRenderer : ListCellRenderer<Image> {
    var color:Color = EditorColorsManager.getInstance().globalScheme.defaultBackground
    var fontColor = EditorColorsManager.getInstance().globalScheme.defaultForeground
    var scale = 1.0
    override fun getListCellRendererComponent(list: JList<out Image>?, value: Image?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {

        val panel = JPanel()
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

        panel.add(textBox)
        if (value != null) {
            val image = if (scale > 1.01) {
                val width = (value as BufferedImage).width * scale
                val height = value.height * scale
                value.getScaledInstance(width.toInt(), height.toInt(), Image.SCALE_AREA_AVERAGING) // scale it the smooth way
            } else {
                value
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
        } else {
            LOGGER.warning("Image is null at index $index")
        }
        return panel
    }
}
