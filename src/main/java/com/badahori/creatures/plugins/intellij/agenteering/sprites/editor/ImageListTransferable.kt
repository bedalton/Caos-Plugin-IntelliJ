package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

internal class ImageListTransferable(private val items: List<ImageTransferItem>) : Transferable {

    @Suppress("IMPLICIT_CAST_TO_ANY")
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any? {
        return when (flavor) {
            DataFlavor.javaFileListFlavor -> fileList
            DataFlavor.imageFlavor -> items.first().image
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    val fileList: List<File> by lazy {
        items.map { it.file }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor == DataFlavor.javaFileListFlavor
                || (items.size == 1 && flavor == DataFlavor.imageFlavor)
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        val base = arrayOf(
                DataFlavor.javaFileListFlavor
        )
        return if (items.size == 1)
            base + DataFlavor.imageFlavor
        else
            base
    }
}