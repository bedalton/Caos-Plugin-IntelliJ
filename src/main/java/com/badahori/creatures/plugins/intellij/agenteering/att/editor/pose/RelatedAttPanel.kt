package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.utils.DisposablePopupMenu
import com.badahori.creatures.plugins.intellij.agenteering.utils.action
import com.badahori.creatures.plugins.intellij.agenteering.utils.addClickListener
import com.bedalton.log.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.JBMenuItem
import java.awt.KeyboardFocusManager
import javax.swing.*


object AttEditorSupport {

    @JvmStatic
    fun initOpenRelatedPopup(relatedFileParts: List<Char>, disposable: Disposable? = null, onSelect: (part: Char?) -> Unit): JPopupMenu? {
        Log.i { "Opening dialog to show related; With ${relatedFileParts.size}" }

        if (relatedFileParts.isEmpty()) {
            Log.w { "getRelatedPartWithDialog was passed an empty list of related parts" }
            return null
        }

        val menu = object: DisposablePopupMenu(disposable) {
            var focused: JComponent? = null

            override fun setVisible(visible: Boolean) {

                // Set visibility as needed
                if (visible) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                }
                // Actually show this panel
                super.setVisible(visible)

                // Set focused element based on visibilty
                if (visible) {
                    // Menu is showing, request focus
                    this.requestFocus()
                } else {
                    // Return focus to previously focused item
                    focused?.requestFocus()
                    focused = null
                }
            }
        }
        menu.add(JLabel("Open Related Part"))
        menu.addSeparator()
        val actionMap = menu.actionMap
        val inputMap = menu.inputMap

        for (part in relatedFileParts) {
            val partText = PoseEditorSupport.getPartName(part)
            val actionKey = partText ?: "Part $part"
            val labelText = if (partText != null) {
                "${part.uppercaseChar()} - $partText"
            } else {
                "${part.uppercaseChar()} - Part ${part.uppercaseChar()}"
            }
            val openPartButton = JBMenuItem(labelText)
            val action = action {
                onSelect(part)
                menu.isVisible = false
            }

            actionMap.put(actionKey, action)
            inputMap.put(KeyStroke.getKeyStroke(part), actionKey)

            // Add click listener to label
            openPartButton.addClickListener {
                onSelect(part)
                menu.isVisible = false
            }
            menu.add(openPartButton)
        }
        val cancel = JPopupMenu("Cancel")
        cancel.addClickListener { menu.isVisible = false; menu.dispose() }
        menu.add(cancel)

        // Allow focusing on the panel
        menu.setInputMap(JComponent.WHEN_FOCUSED, inputMap)
        menu.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap)
        menu.setActionMap(actionMap)
        return menu
    }

    @JvmStatic
    fun initOpenRelatedPopup(relatedFileParts: List<Char>, disposable: Disposable?, actionHandler: PartSelectedAction): JPopupMenu? {
        return initOpenRelatedPopup(relatedFileParts, disposable) { part ->
            actionHandler.onPartSelected(part)
        }
    }
}


interface PartSelectedAction {
    fun onPartSelected(part: Char?)
}