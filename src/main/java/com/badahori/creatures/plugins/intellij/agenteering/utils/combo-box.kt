package com.badahori.creatures.plugins.intellij.agenteering.utils

import javax.swing.ComboBoxModel
import javax.swing.JComboBox
import javax.swing.JList

val <T> JComboBox<T>.items: List<T>? get() {
    val model = model
        ?: return null
    val size = model.size
    return (0 until size).map { i ->
        model.getElementAt(i)
    }
}

val <T> ComboBoxModel<T>.items: List<T> get() {
    val size = size
    return (0 until size).map { i ->
        getElementAt(i)
    }
}


inline fun <T> JComboBox<T>.selectItem(evenIfNull: Boolean = true, check: (item:T) -> Boolean): T? {
    val items = items
        ?: return null
    val item =  items.firstOrNull(check)
    if (item == null && !evenIfNull)
        return null
    selectedItem = item
    return item
}

val <T> JList<T>.items: List<T> get() {
    val model = model
    val size = model.size
    return (0 until size).map { i ->
        model.getElementAt(i)
    }
}