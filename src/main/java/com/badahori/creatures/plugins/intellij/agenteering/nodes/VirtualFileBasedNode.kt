package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.io.IOException
import javax.swing.JComponent
import javax.swing.TransferHandler

internal abstract class VirtualFileBasedNode<VfsT:VirtualFile>(project:Project, protected val myVirtualFile:VfsT, viewSettings:ViewSettings?)
    : ProjectViewNode<VfsT>(project, myVirtualFile, viewSettings) {

    override fun getVirtualFile(): VirtualFile {
        return myVirtualFile
    }

    override fun getWeight(): Int {
        return SORT_WEIGHT
    }

    override fun getName(): String? {
        return myVirtualFile.name
    }

    override fun toString(): String {
        return myVirtualFile.name
    }

    override fun getTypeSortKey(): PsiFileNode.ExtensionSortKey? {
        val extension = myVirtualFile.extension
        return if (extension == null) null else PsiFileNode.ExtensionSortKey(extension)
    }


    override fun canRepresent(element: Any?): Boolean {
        if (super.canRepresent(element)) return true
        return value != null && element != null && element == myVirtualFile
    }

    override fun contains(file: VirtualFile): Boolean {
        return myVirtualFile == file
    }

}

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