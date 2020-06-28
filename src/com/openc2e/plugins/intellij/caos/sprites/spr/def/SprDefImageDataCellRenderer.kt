package com.openc2e.plugins.intellij.caos.sprites.spr.def

import java.awt.Color
import java.awt.Component
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.*

val SELECTED_COLOR = Color.DARK_GRAY
val DEFAULT_COLOR = Color.WHITE

object SprDefImageDataCellRenderer : ListCellRenderer<SprDefImageData> {
    var color: Color = DEFAULT_COLOR
    var colorName:String = "Transparent"
    var fontColor = Color.BLACK
    var scale = 2.0
    override fun getListCellRendererComponent(list: JList<out SprDefImageData>?, value: SprDefImageData?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        val textBox = JLabel("$index.")
        panel.border = BorderFactory.createEmptyBorder(5, 16, 10, 5)
        if (isSelected) {
            panel.background = SELECTED_COLOR
            panel.foreground = DEFAULT_COLOR
            textBox.foreground = DEFAULT_COLOR
        } else {
            panel.background = DEFAULT_COLOR
            panel.foreground = SELECTED_COLOR
            textBox.foreground = SELECTED_COLOR
        }

        panel.add(textBox)
        val component = when (value) {
            is SprDefImageData.SprDefImage -> render(index, value)
            is SprDefImageData.SprDefImageError -> JLabel("Error for image ${value.relativePath}. ${value.error}")
            is SprDefImageData.SprDefMissingImage -> JLabel("Image is missing at path '${value.relativePath}'")
            null -> JLabel("No data found in holder")
        }
        panel.add(component)
        return panel
    }

    private fun render(index: Int, value: SprDefImageData.SprDefImage): JComponent {
        val image = if (scale > 1.01) {
            val width = (value as BufferedImage).width * scale
            val height = value.height * scale
            value.bufferedImage.getScaledInstance(width.toInt(), height.toInt(), Image.SCALE_AREA_AVERAGING) // scale it the smooth way
        } else {
            value.bufferedImage
        }
        val imageIcon = ImageIcon(image).apply {
            description = "Sprite image $index"
        }
        val imageView = JLabel(imageIcon)
        //imageView.setBounds(0,0,((value as BufferedImage) * scale).width, (value as BufferedImage).height)
        //imageView.minimumSize = Dimension((value as BufferedImage).width, (value as BufferedImage).height)
        imageView.border = BorderFactory.createLineBorder(fontColor.brighter().brighter(), 1)
        if (color.alpha > 0) {
            imageView.background = color
            imageView.isOpaque = true
        }
        return imageView;
    }
}