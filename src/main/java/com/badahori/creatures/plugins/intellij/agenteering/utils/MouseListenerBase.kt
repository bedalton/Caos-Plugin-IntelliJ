package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent

open class MouseListenerBase : MouseListener {
    override fun mouseClicked(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
}


fun JComponent.addClickListener(listener: (MouseEvent) -> Unit) {
    addMouseListener(object: MouseListenerBase() {
        override fun mouseClicked(e: MouseEvent) {
            listener(e)
        }
    })
}

val MouseEvent.isButton1 get() = button == MouseEvent.BUTTON1
val MouseEvent.isButton2 get() = button == MouseEvent.BUTTON2
val MouseEvent.isButton3 get() = button == MouseEvent.BUTTON3
val MouseEvent.button1IsDown get() = this.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK != 0
val MouseEvent.button2IsDown get() = this.modifiersEx and MouseEvent.BUTTON2_DOWN_MASK != 0
val MouseEvent.button3IsDown get() = this.modifiersEx and MouseEvent.BUTTON3_DOWN_MASK != 0