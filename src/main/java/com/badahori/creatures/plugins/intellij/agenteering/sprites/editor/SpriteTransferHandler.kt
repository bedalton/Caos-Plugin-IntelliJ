package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import java.awt.Image
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.TransferHandler


/**
 * Transfer handle to allow copying files outside of the Sprite image list view
 */
internal class SpriteTransferHandler(private val fileName: String, private val image: BufferedImage?) : TransferHandler() {
    constructor(imageTransferItem: ImageTransferItem) : this(imageTransferItem.fileName, imageTransferItem.image)
    val imageTransferable by lazy {
        ImageTransferable(fileName, image)
    }
    /**
     * Bundle up the data for export.
     */
    override fun createTransferable(c: JComponent): Transferable? {
        return imageTransferable
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
        return COPY_OR_MOVE
    }

    override fun getDragImage(): Image? {
        return image
    }
}