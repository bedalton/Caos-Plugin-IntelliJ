package com.openc2e.plugins.intellij.caos.sprites.spr.def

import com.intellij.openapi.vfs.VfsUtilCore
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

internal val SprImageDataFlavor = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
";class=\"" + java.awt.List::class.java.name + "\"");


class SprImageDataTransferable(val data: List<SprDefImageData>) : Transferable {
    val file:List<File?> by lazy {
        data.map {
            it.virtualFile?.let {
                VfsUtilCore.virtualToIoFile(it)
            }
        }
    }
    override fun getTransferData(flavor: DataFlavor?): Any {
        when (flavor) {
            SprImageDataFlavor -> data
            DataFlavor.javaFileListFlavor -> file
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
                DataFlavor.javaFileListFlavor
        )
        return flavors.toTypedArray()
    }
}