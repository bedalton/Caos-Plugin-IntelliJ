package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.badahori.creatures.plugins.intellij.agenteering.common.IMessageBundle
import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

internal open class MessageBundleCellRenderer(
    private val bundle: IMessageBundle,
    private val foreground: Color? = null,
    private val background: Color? = null,
) : ListCellRenderer<String> {


    private val pool = mutableListOf<JLabel>()

    override fun getListCellRendererComponent(
        list: JList<out String>?,
        value: String?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val label = get(index)

        label.text = if (value != null) {
            bundle.getMessage(value) ?: value
        } else {
            ""
        }
        return label
    }


    fun get(index: Int): JLabel {
        if (index < 0) {
            return JLabel()
        }
        while (index >= pool.size) {
            val component = JLabel()
            if (background != null) {
                component.background = background
            }
            if (foreground != null) {
                component.foreground = foreground
            }
            pool.add(component)
        }
        return pool[index]
    }
}


internal class AttMessageBundleCellRenderer(
    foreground: Color? = null,
    background: Color? = null,
) : MessageBundleCellRenderer(
    AttMessages,
    foreground,
    background
) {
    constructor(): this(null, null)
}

