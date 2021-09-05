package com.badahori.creatures.plugins.intellij.agenteering.utils

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class DocumentChangeListener(private val callback: (type: Int) -> Unit): DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
        callback(INSERT)
    }

    override fun removeUpdate(e: DocumentEvent?) {
        callback(REMOVE)
    }

    override fun changedUpdate(e: DocumentEvent?) {
        callback(CHANGE)
    }

    companion object {
        const val CHANGE = 0
        const val INSERT = 1
        const val REMOVE = 2
    }
}


fun JTextComponent.addChangeListener(callback: (type: Int) -> Unit) {
    document.addDocumentListener(DocumentChangeListener(callback))
}