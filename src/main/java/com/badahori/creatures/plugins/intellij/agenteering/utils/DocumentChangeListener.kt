package com.badahori.creatures.plugins.intellij.agenteering.utils

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document
import javax.swing.text.JTextComponent

class DocumentChangeListener(private val callback: (type: Int, newText: String) -> Unit): DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
            notify(INSERT, e)
    }

    override fun removeUpdate(e: DocumentEvent) {
            notify(REMOVE, e)
    }

    override fun changedUpdate(e: DocumentEvent) {
            notify(CHANGE, e)
    }

    private fun notify(type: Int, e: DocumentEvent) {
        val document = e.document
        val text = document.getText(0, document.length)
        callback(type, text)
    }

    companion object {
        const val CHANGE = 0
        const val INSERT = 1
        const val REMOVE = 2
    }
}


fun JTextComponent.addChangeListener(callback: (type: Int, newText: String) -> Unit) {
    document.addDocumentListener(DocumentChangeListener(callback))
}