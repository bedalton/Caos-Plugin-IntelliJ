package com.openc2e.plugins.intellij.caos.sprites.spr.def

import com.intellij.openapi.vfs.VfsUtilCore
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

internal val SprImageDataFlavor = DataFlavor(java.awt.datatransfer.DataFlavor.javaJVMLocalObjectMimeType +
";class=\"" + SprDefImageData::class.java.name + "\"");


class SprImageDataTransferable(val data: SprDefImageData) : Transferable {
    val file:File? by lazy {
        data.virtualFile?.let {
            VfsUtilCore.virtualToIoFile(it)
        }
    }
    override fun getTransferData(flavor: DataFlavor?): Any {
        when (flavor) {
            SprImageDataFlavor -> data
            DataFlavor.javaFileListFlavor,
            DataFlavor.imageFlavor -> if (data is SprDefImageData.SprDefImage)
                data.bufferedImage
            DataFlavor.javaFileListFlavor -> file
            DataFlavor.stringFlavor -> file?.absolutePath ?: "::${data.relativePath}"
            else -> throw UnsupportedFlavorException(flavor)
        }
        return data
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor == SprImageDataFlavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        val flavors = mutableListOf<DataFlavor>(
                SprImageDataFlavor,
                DataFlavor.javaFileListFlavor,
                DataFlavor.stringFlavor
        )
        if (data is SprDefImageData.SprDefImage)
            flavors += DataFlavor.imageFlavor
        return flavors.toTypedArray()
    }
}