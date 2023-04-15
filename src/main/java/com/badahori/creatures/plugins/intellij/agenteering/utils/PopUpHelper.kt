package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import javax.swing.JPopupMenu

object PopUpHelper {

}

open class DisposablePopupMenu(parentDisposable: Disposable?): JPopupMenu(), Disposable {

    constructor(): this(null) {}

    init {
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this)
        }
    }


    override fun dispose() {
        isVisible = false
    }

}


