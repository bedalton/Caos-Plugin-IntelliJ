package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.toPngDataUri
import com.badahori.creatures.plugins.intellij.agenteering.utils.canonicalName
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.File


internal class ImageTransferable(private val imageTransferItem: ImageTransferItem) : Transferable {
    /**
     * Alternate constructor for raw parts
     */
    constructor(name:String, image: BufferedImage) : this(ImageTransferItem(name, image))
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return when (flavor) {
            DataFlavor.javaFileListFlavor -> fileList
            DataFlavor.imageFlavor -> imageTransferItem.image
            DataFlavor.allHtmlFlavor -> "<img src=\"${imageTransferItem.image.toPngDataUri()}\" />"
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    val fileList:List<File> by lazy {
        listOf(imageTransferItem.file)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor == DataFlavor.imageFlavor
                || flavor == DataFlavor.allHtmlFlavor
                || flavor == DataFlavor.javaFileListFlavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
                DataFlavor.imageFlavor,
                DataFlavor.allHtmlFlavor,
                DataFlavor.javaFileListFlavor
        )
    }
}