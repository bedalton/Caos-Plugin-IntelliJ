package com.badahori.creatures.plugins.intellij.agenteering.components

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariantChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JLabel

private val variantStringsList by lazy {
    arrayOf(
        "",
        "C1",
        "C2",
        "CV",
        "C3",
        "DS",
        "SM"
    )
}

class VariantComboBox(
    selected: CaosVariant?,
    parentDisposable: Disposable?
): ComboBox<String>(variantStringsList), Disposable {

    constructor(): this(selected = null, parentDisposable = null)
    constructor(selected: CaosVariant?): this(selected = selected, parentDisposable = null)
    constructor(parentDisposable: Disposable?): this(selected = null, parentDisposable = parentDisposable)

    private val listeners: MutableList<CaosVariantChangeListener> = mutableListOf()


    val variant: CaosVariant? get() = (selectedItem as String?).let {
        if (it.isNullOrBlank()) {
            return@let null
        }
        CaosVariant.fromVal(it)
            .nullIfNotConcrete()
    }


    init {
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this)
        }

        this.selectedItem = selected?.code ?: ""

        addActionListener(this::onChange)
    }

    private fun onChange(e: ActionEvent?) {
        val variant = variant
        listeners.forEach {
            it.invoke(variant)
        }
    }

    fun addChangeListener(listener: CaosVariantChangeListener) {
        listeners.add(listener)
        listener(variant)
    }

    fun removeChangeListener(listener: CaosVariantChangeListener) {
        listeners.remove(listener)
    }

    override fun dispose() {
        listeners.clear()
        removeActionListener(this::onChange)
        this.action = null
    }

}


class CaosVariantComboBoxPanel(
    selected: CaosVariant? = null,
    private val stackVertical: Boolean = false,
    labelText: String? = null,
    parentDisposable: Disposable? = null,
    private val labelForeground: Color = UIUtil.getLabelForeground(),
    private val labelBackground: Color = UIUtil.getLabelBackground()
): Disposable {
    val label by lazy {
        val variantLabel = labelText ?: CaosBundle.message("caos.variant.select")
        JLabel(variantLabel)
    }

    private val comboBox by lazy {
        VariantComboBox(selected, this)
    }

    val component: JComponent by lazy {
        val parent = if (stackVertical) {
            VerticalBox()
        } else {
            HorizontalBox()
        }

        parent.add(label)
        parent.add(comboBox)
        parent
    }

    init {
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this)
        }
    }

    fun addChangeListener(listener: CaosVariantChangeListener) {
        comboBox.addChangeListener(listener)
    }

    fun removeChangeListener(listener: CaosVariantChangeListener) {
        comboBox.removeChangeListener(listener)
    }

    override fun dispose() {
        if (!Disposer.isDisposed(comboBox)) {
            comboBox.dispose()
        }
    }
}


