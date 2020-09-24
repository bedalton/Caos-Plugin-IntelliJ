package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.io.IOException
import javax.swing.JComponent
import javax.swing.TransferHandler

internal abstract class VirtualFileBasedNode<VfsT:VirtualFile>(project:Project, protected val myVirtualFile:VfsT)
    : AbstractTreeNode<VfsT>(project, myVirtualFile)

private fun createTransferable(virtualFile: VirtualFile) : Transferable {
    try {
        VfsUtil.virtualToIoFile(virtualFile).let { file ->
            if (file.exists())
                return FileTransferable(file)
        }
    } catch (e: Exception) {
    }
    val directory = FileUtil.createTempDirectory(virtualFile.name, null, true).apply {
        createNewFile()
    }
    val file = File(directory, virtualFile.name).apply {
        createNewFile()
        writeBytes(virtualFile.contentsToByteArray())
    }
    return FileTransferable(file)
}

internal class VfsTransferHandler(virtualFile: VirtualFile): TransferHandler() {

    private val transferable by lazy {
        createTransferable(virtualFile)
    }

    /**
     * Bundle up the data for export.
     */
    public override fun createTransferable(c: JComponent): Transferable? {
        return transferable
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
}

internal class FileTransferable(ioFile:File) : Transferable {
    val fileList = listOf(ioFile)
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor == DataFlavor.javaFileListFlavor
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor != DataFlavor.javaFileListFlavor)
            throw IOException("Invalid data flavor requested")
        return fileList
    }

}