package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

/**
 * Transfer handle to allow copying files outside of the Sprite image list view
 */
internal object SpriteListTransferHandler : TransferHandler() {
    /**
     * Bundle up the data for export.
     */
    public override fun createTransferable(c: JComponent): Transferable? {
        val list = c as JList<*>
        val selectedValues = list.selectedValuesList.mapNotNull {
            it as? ImageTransferItem
        }
        if (selectedValues.isEmpty())
            return null
        if (selectedValues.size == 1) {
            return ImageTransferable(selectedValues.first())
        }
        return ImageListTransferable(selectedValues)
    }

    /**
     * List cannot import images
     */
    override fun canImport(support: TransferSupport?): Boolean {
        return false
    }

    /**
     * Actions for this list include copying only
     */
    override fun getSourceActions(c: JComponent?): Int {
        return COPY
    }
}