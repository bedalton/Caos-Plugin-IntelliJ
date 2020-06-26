package com.openc2e.plugins.intellij.caos.sprites.spr

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import javafx.scene.control.Label
import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.*
import javax.swing.text.FlowView


class SprImageView(file:VirtualFile) : JPanel() {
    private val images:List<Image?> by lazy {
        SprParser.parseSprite(file)
    }

    init {
        val scrollList = JBScrollPane()
        val listView = JBList<Image>(images)
        listView.cellRenderer = SpriteCellRenderer
        listView.layoutOrientation = JList.VERTICAL
        listView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    }

}

object SpriteCellRenderer : ListCellRenderer<Image> {
    override fun getListCellRendererComponent(list: JList<out Image>?, value: Image?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        if (isSelected) {
            panel.background = SELECTED_COLOR
        } else {
            panel.background = DEFAULT_COLOR
        }
        val textBox = JLabel("$index.")
        panel.add(textBox)
        if (value != null) {
            val imageView = JLabel(ImageIcon(value).apply {
                description = "Sprite image $index"
            })
            textBox.labelFor = imageView
            panel.add(imageView)
        }
        return panel
    }
}


private val DEFAULT_COLOR by lazy { Color(255,255,255) }
private val SELECTED_COLOR by lazy { Color(100,100,100) }
