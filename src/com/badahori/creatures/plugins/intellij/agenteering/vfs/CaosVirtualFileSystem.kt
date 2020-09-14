package com.badahori.creatures.plugins.intellij.agenteering.vfs
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.intellij.openapi.vfs.*
import java.io.IOException
import java.util.*


/**
 * A virtual file system
 */
class CaosVirtualFileSystem : DeprecatedVirtualFileSystem() {

    private val root = CaosVirtualFile(CAOS_VFS_ROOT, null, true)

    /**
     * Listeners for file system events.
     */
    private val listeners: MutableList<VirtualFileListener> = ArrayList()

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

    /**
     * Add a file to the file system.
     * @param file the file to add
     */
    fun addFile(file: CaosVirtualFile) {
        root.addChild(file)
        fireFileCreated(file)
    }

    /**
     * Notifies listeners
     * @param file newly created file
     */
    private fun fireFileCreated(file: CaosVirtualFile) {
        val e = VirtualFileEvent(this,
                file,
                file.name,
                file.getParent())
        for (listener in listeners) {
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
        //fireBeforeFileDeletion(requestor, virtualFile)
        (virtualFile.parent ?: root).let {
            it.deleteChild(virtualFile)
        }
        //fireBeforeFileDeletion(requestor, virtualFile)
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun moveFile(requestor: Any?,
                                 virtualFile: VirtualFile,
                                 newParentIn: VirtualFile) {
        if (virtualFile !is CaosVirtualFile)
            throw Exception("Cannot move non-CaosVirtualFile to CAOS virtual file parent")
        val oldParent = virtualFile.parent ?: root
        val newParent = newParentIn as? CaosVirtualFile
                ?: throw Exception("Cannot move file. Parent file is not CAOS Virtual file")
        //fireBeforeFileMovement(requestor, virtualFile, newParent)
        oldParent.deleteChild(virtualFile)
        newParent.addChild(virtualFile)
        //fireFileMoved(requestor, virtualFile, oldParent)
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
        //fireFileCreated(requestor, file)
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
        //fireFileCreated(requestor, file)
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
    override fun findFileByPath(filePath: String) : CaosVirtualFile? {
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
            currentFile = currentFile.findChild(name) as? CaosVirtualFile
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
            //fireFileCopied(requestor, virtualFile, this)
        }
    }

    /** {@inheritDoc}  */
    override fun isReadOnly(): Boolean {
        return false
    }

    /** {@inheritDoc}  */
    fun isDirectory(virtualFile: VirtualFile): Boolean {
        return virtualFile.isDirectory
    }

    companion object {
        val instance: CaosVirtualFileSystem by lazy {
            VirtualFileManager.getInstance().getFileSystem(CAOS_VFS_PROTOCOL) as CaosVirtualFileSystem
        }
    }
}
