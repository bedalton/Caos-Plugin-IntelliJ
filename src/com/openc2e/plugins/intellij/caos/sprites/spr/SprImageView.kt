package com.openc2e.plugins.intellij.caos.sprites.spr

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*


class SprImageView(file:VirtualFile) : JPanel() {
    private val images:List<Image?> by lazy {
        SprParser.parseSprite(file)
    }

    init {
        layout = GridLayoutManager(1, 1, Insets(0, 0, 0, 0), -1, -1)
        background = Color(255,0,0)
        val scrollList = JBScrollPane()
        scrollList.background = Color(0,255,0)
        val listView = JBList<Image>(images)
        listView.background = Color(0,0,255)
        listView.cellRenderer = SpriteCellRenderer
        listView.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        scrollList.add(listView)
        add(scrollList, GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
    }

}

object SpriteCellRenderer : ListCellRenderer<Image> {
    var color:Color = DEFAULT_COLOR
    var fontColor = Color.BLACK
    var scale = 2.0
    override fun getListCellRendererComponent(list: JList<out Image>?, value: Image?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {

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


private val DEFAULT_COLOR by lazy { Color(255,255,255) }
private val SELECTED_COLOR by lazy { Color(100,100,100) }
