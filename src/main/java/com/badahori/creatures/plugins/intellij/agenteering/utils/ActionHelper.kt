package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.InputMap
import javax.swing.JComponent
import javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
import javax.swing.KeyStroke

internal fun action(work: (e: ActionEvent?) -> Unit): javax.swing.Action {
    return object: AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            work(e)
        }
    }
}

object ActionHelper {
    internal fun action(work: (e: ActionEvent?) -> Unit): javax.swing.Action {
        return object: AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                work(e)
            }
        }
    }

    @JvmStatic
    fun action(work: ActionListener): javax.swing.Action {
        return object: AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                work.actionPerformed(e)
            }
        }
    }

    @JvmStatic
    fun action(work: SimpleAction): javax.swing.Action {
        return object: AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                work.execute()
            }
        }
    }

    @JvmStatic
    fun action(work: () -> Unit): javax.swing.Action {
        return object: AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                work()
            }
        }
    }

    @JvmStatic
    fun addKeyAction(actionMap: ActionMap, inputMap: InputMap, label: String, keyStroke: KeyStroke, action: () -> Unit) {
        actionMap.put(label, action(action))
        inputMap.put(keyStroke, label)
    }


    @JvmStatic
    fun addKeyAction(condition: Int, component: JComponent, label: String, keyStroke: KeyStroke, action: () -> Unit) {
        val actionMap = component.actionMap
        val inputMap = component.getInputMap(condition)

        addKeyAction(actionMap, inputMap, label, keyStroke, action)

        component.actionMap = actionMap
        component.setInputMap(condition, inputMap)
    }

    @JvmStatic
    fun addKeyAction(component: JComponent, label: String, keyStroke: KeyStroke, action: () -> Unit) {
        addKeyAction(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            component,
            label,
            keyStroke,
            action
        )
    }

}

interface SimpleAction {
    fun execute()
}


internal fun JComponent.addKeyAction(label: String, keystroke: KeyStroke, action: () -> Unit) {
    ActionHelper.addKeyAction(this, label, keystroke, action)
}

internal fun JComponent.addKeyAction(condition: Int, label: String, keystroke: KeyStroke, action: () -> Unit) {
    ActionHelper.addKeyAction(condition, this, label, keystroke, action)
}
