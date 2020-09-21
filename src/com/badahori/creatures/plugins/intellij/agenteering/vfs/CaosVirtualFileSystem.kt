package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.intellij.openapi.vfs.*
import java.io.IOException


/**
 * A virtual file system
 */
class CaosVirtualFileSystem : DeprecatedVirtualFileSystem() {

    private val root = CaosVirtualFile(CAOS_VFS_ROOT, null, true)

    /**
     * Listeners for file system events.
     */
    private val listeners: MutableList<VirtualFileListener> = mutableListOf()

    private val saveListeners:MutableList<CaosVirtualFileSaveListener> = mutableListOf()

    /** {@inheritDoc}  */
    override fun addVirtualFileListener(virtualFileListener: VirtualFileListener) {
        super.addVirtualFileListener(virtualFileListener)
        listeners.add(virtualFileListener)
    }

    /** {@inheritDoc}  */
    override fun removeVirtualFileListener(virtualFileListener: VirtualFileListener) {
        super.removeVirtualFileListener(virtualFileListener)
        listeners.remove(virtualFileListener)
    }

    fun addOnSaveListener(listener:CaosVirtualFileSaveListener) {
        saveListeners.add(listener)
    }

    fun removeOnSaveListener(listener:CaosVirtualFileSaveListener) {
        saveListeners.remove(listener)
    }

    /**
     * Add a file to the file system.
     * @param file the file to add
     */
    fun addFile(file: CaosVirtualFile) {
        root.addChild(file)
        fireFileCreated(null, file)
    }

    /**
     * Notifies listeners
     * @param file newly created file
     */
    override fun fireFileCreated(requestor: Any?, file: VirtualFile) {
        val e = VirtualFileEvent(this,
                file,
                file.name,
                file.parent)
        listeners.forEach { listener ->
            listener.fileCreated(e)
        }
    }

    /** {@inheritDoc}  */
    override fun getProtocol(): String {
        return CAOS_VFS_PROTOCOL
    }

    /** {@inheritDoc}  */
    override fun refresh(b: Boolean) {}

    /** {@inheritDoc}  */
    override fun refreshAndFindFileByPath(string: String): VirtualFile? {
        return root.findChild(string)
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun deleteFile(requestor: Any?,
                                   virtualFile: VirtualFile) {
        if (virtualFile !is CaosVirtualFile)
            throw IOException("Cannot delete non-CAOSVirtualFile from CAOS VFS")
        fireBeforeFileDeletion(requestor, virtualFile)
        (virtualFile.parent ?: root).deleteChild(virtualFile)
        fireFileDeleted(requestor, virtualFile, virtualFile.name, virtualFile.parent)
    }

    /**
     * Notifies listeners
     * @param file newly created file
     */
    override fun fireBeforeFileDeletion(requestor: Any?, file: VirtualFile) {
        val e = VirtualFileEvent(this,
                file,
                file.name,
                file.parent)
        listeners.forEach { listener ->
            listener.beforeFileDeletion(e)
        }
    }

    override fun fireFileDeleted(requestor: Any?, file: VirtualFile, fileName: String, parent: VirtualFile?) {
        val e = VirtualFileEvent(this,
                file,
                file.name,
                parent)
        listeners.forEach { listener ->
            listener.fileDeleted(e)
        }
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun moveFile(requestor: Any?,
                                 virtualFileIn: VirtualFile,
                                 newParentIn: VirtualFile) {
        val newParent = newParentIn as? CaosVirtualFile
                ?: throw Exception("Cannot move file. Parent file is not CAOS Virtual file")
        // Ensure is CAOS file or copy to allow use in Caos VFS
        val virtualFile = if (virtualFileIn !is CaosVirtualFile) {
            val temp = CaosVirtualFile(virtualFileIn.name, virtualFileIn.contents, virtualFileIn.isDirectory)
            virtualFileIn.delete(requestor)
            temp
        } else
            virtualFileIn
        val oldParent = virtualFile.parent ?: root
        fireBeforeFileMovement(requestor, virtualFile, newParent)
        oldParent.deleteChild(virtualFile)
        newParent.addChild(virtualFile)
        fireFileMoved(requestor, virtualFile, oldParent)
    }

    override fun fireBeforeFileMovement(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {
        val e = VirtualFileMoveEvent(this,
                file,
                file.parent,
                newParent)
        listeners.forEach{  listener ->
            listener.beforeFileMovement(e)
        }
    }

    override fun fireFileMoved(requestor: Any?, file: VirtualFile, oldParent: VirtualFile) {
        val e = VirtualFileMoveEvent(this,
                file,
                oldParent,
                file.parent)
        listeners.forEach{  listener ->
            listener.fileMoved(e)
        }
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun renameFile(requestor: Any?,
                                   virtualFile: VirtualFile,
                                   name: String) {
        if (virtualFile !is CaosVirtualFile) {
            throw IOException("Cannot rename non-CaosVirtualFile")
        }
        (virtualFile.parent ?: root).let {
            it.deleteChild(virtualFile)
            virtualFile.name = name
            root.addChild(virtualFile)
        }
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun createChildFile(requestor: Any?,
                                 parentIn: VirtualFile,
                                 name: String): CaosVirtualFile {
        val file = CaosVirtualFile(name, null)
        file.parent = parentIn as? CaosVirtualFile
        addFile(file)
        fireFileCreated(requestor, file)
        return file
    }


    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun createChildDirectory(requestor: Any?,
                                      parent: VirtualFile,
                                      name: String): CaosVirtualFile {
        val file = CaosVirtualFile(name, null, true)
        if (!parent.isDirectory)
            throw IOException("Cannot add child directory to non-directory parent: ${parent.name}")
        (parent as CaosVirtualFile).addChild(file)
        fireFileCreated(requestor, file)
        return file
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    fun getOrCreateRootChildDirectory(name: String): CaosVirtualFile {
        root.findChild(name)?.let {
            return it
        }
        val file = CaosVirtualFile(name, null, true)
        addFile(file)
        return file
    }

    /**
     * Get file in file system
     */
    override fun findFileByPath(filePath: String): CaosVirtualFile? {
        return findFileByPath(filePath, false)
    }

    /**
     * Get file in file system
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun findFileByPath(filePathIn: String, createSubFolders: Boolean = false): CaosVirtualFile? {
        val pathWithoutSchema = if (filePathIn.startsWith(CAOS_VFS_SCHEMA))
            filePathIn.substring(CAOS_VFS_SCHEMA.length)
        else
            filePathIn
        val names: MutableList<String> = pathWithoutSchema.split("/").toMutableList()
        val parentName = names.removeAt(0)
        var currentFile: CaosVirtualFile = getOrCreateRootChildDirectory(parentName)
        while (names.isNotEmpty()) {
            val name = names.removeAt(0)
            currentFile = currentFile.findChild(name)
                    ?: if (names.isNotEmpty() && createSubFolders)
                        createChildDirectory(null, currentFile, name)
                    else
                        return null
        }
        return currentFile
    }

    /**
     * Get file in file system
     */
    @Suppress("unused")
    fun getDirectory(filePathIn: String, createSubFolders: Boolean = false): CaosVirtualFile? {
        val pathWithoutSchema = if (filePathIn.startsWith(CAOS_VFS_SCHEMA))
            filePathIn.substring(CAOS_VFS_SCHEMA.length)
        else
            filePathIn
        val names: MutableList<String> = pathWithoutSchema.split("/")
                .filter { it.trim('/', ' ', '\t').isNotBlank() }
                .toMutableList()
        val parentName = names.removeAt(0)
        var currentFile: CaosVirtualFile = getOrCreateRootChildDirectory(parentName)
        while (names.isNotEmpty()) {
            val name = names.removeAt(0)
            currentFile = currentFile.findChild(name)
                    ?: if (createSubFolders)
                        createChildDirectory(null, currentFile, name)
                    else
                        return null
        }
        return currentFile
    }

    fun exists(path: String): Boolean {
        return findFileByPath(path, false) != null
    }

    /** {@inheritDoc}  */
    fun exists(virtualFile: VirtualFile?): Boolean {
        return virtualFile in root.children
    }

    override fun isCaseSensitive(): Boolean {
        return false
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun copyFile(requestor: Any?,
                          virtualFile: VirtualFile,
                          newParent: VirtualFile,
                          copyName: String): VirtualFile {
        if (newParent !is CaosVirtualFile)
            throw IOException("Cannot copy Caos Virtual file to non-Caos Virtual file directory")
        return CaosVirtualFile(copyName, virtualFile.contents, virtualFile.isDirectory).apply {
            parent = newParent
            fireFileCopied(requestor, virtualFile, this)
        }
    }

    override fun fireFileCopied(requestor: Any?, originalFile: VirtualFile, createdFile: VirtualFile) {
        val event = VirtualFileCopyEvent(
                requestor,
                originalFile,
                createdFile
        )
        listeners.forEach{  listener ->
            listener.fileCopied(event)
        }
        super.fireFileCopied(requestor, originalFile, createdFile)
    }

    /** {@inheritDoc}  */
    override fun isReadOnly(): Boolean {
        return false
    }

    /** {@inheritDoc}  */
    fun isDirectory(virtualFile: VirtualFile): Boolean {
        return virtualFile.isDirectory
    }

    fun fireOnSaveEvent(virtualFile: CaosVirtualFile) {
        val event = CaosVirtualFileEvent(
                null,
                virtualFile,
                virtualFile.name,
                virtualFile.parent
        )
        saveListeners.forEach { listener ->
            listener(event)
        }
        listeners.filterIsInstance<CaosVirtualFileListener>().forEach{ listener ->
            listener.onFileSave(event)
        }
    }

    companion object {
        val instance: CaosVirtualFileSystem by lazy {
            VirtualFileManager.getInstance().getFileSystem(CAOS_VFS_PROTOCOL) as CaosVirtualFileSystem
        }
    }
}

class CaosVirtualFileEvent(requestor:Any?, file:CaosVirtualFile, fileName:String, parent:VirtualFile?)
    : VirtualFileEvent(requestor, file, fileName, parent)

typealias CaosVirtualFileSaveListener = (e: CaosVirtualFileEvent) -> Unit

interface CaosVirtualFileListener : VirtualFileListener {
    @JvmDefault
    fun onFileSave(e: CaosVirtualFileEvent) {

    }
}