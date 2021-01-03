package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariants
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.VariantsFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.now
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.io.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * Virtual File for Caos Scripts Plugin
 */
open class CaosVirtualFile private constructor(
    private var fileName: String,
    private var stringContents: String? = null,
    private var byteArrayContents: ByteArray? = null,
    private val isDirectory: Boolean,
    private val allowSubdirectories: Boolean = isDirectory
) : VirtualFile(), ModificationTracker, HasVariant, HasVariants {

    constructor(name: String, content: String?) : this(name, content, null, false)

    constructor(name: String, content: String?, isDirectory: Boolean) : this(name, content, null, isDirectory)

    constructor(name: String, content: ByteArray) : this(name, null, content, false)

    constructor(name: String, content: ByteArray, isDirectory: Boolean) : this(name, null, content, isDirectory)

    private var timestamp = now
    private var modificationStamp: Long = timestamp

    /**
     * Gets a child file by name, if this virtual file is a directory
     */
    operator fun get(fileName: String): CaosVirtualFile? {
        return findChild(fileName)
    }

    /** {@inheritDoc}  */
    //override fun getId():Int = myId

    /** Allows Quick access to CAOS Variant */
    override var variant: CaosVariant? = null
    private var _variants: List<CaosVariant>? = null
    override var variants: List<CaosVariant>
        get() = _variants
            ?: VariantsFilePropertyPusher.readFromStorageCatching(this).nullIfEmpty()
            ?: listOfNotNull(variant)
        set(list) {
            _variants = list
        }
    private var _fileType: FileType? = null

    fun setFileType(newFileType: FileType) {
        _fileType = newFileType
    }

    override fun getFileType(): FileType {
        return _fileType ?: super.getFileType()
    }

    /** {@inheritDoc}  */
    override fun getName(): String = fileName

    fun setName(name: String) {
        fileName = name
    }

    fun setContent(contents: ByteArray?) {
        stringContents = null
        byteArrayContents = contents
    }

    fun setContent(contents: String?) {
        stringContents = contents
        byteArrayContents = null
    }

    /** {@inheritDoc}  */
    override fun getNameWithoutExtension(): String = FileUtil.getNameWithoutExtension(name)

    /** {@inheritDoc}  */
    override fun isDirectory(): Boolean = isDirectory

    /**
     * The children of this file, if the file is a directory.
     */
    private val children: CaseInsensitiveHashMap<CaosVirtualFile> = CaseInsensitiveHashMap()

    /** Parent Virtual file */
    internal var parent: CaosVirtualFile? = null

    /** {@inheritDoc}  */
    override fun getParent(): CaosVirtualFile? = parent

    /**
     * Immutability flag
     */
    private var isWritable = false

    /** {@inheritDoc}  */
    override fun isWritable(): Boolean = isWritable

    /** {@inheritDoc}  */
    override fun setWritable(writable: Boolean) {
        isWritable = writable
    }

    fun hasChild(fileName: String): Boolean {
        return children.containsKey(fileName)
    }

    /** {@inheritDoc}  */
    override fun getFileSystem(): VirtualFileSystem = CaosVirtualFileSystem.instance

    /** {@inheritDoc}  */
    @Suppress("RecursivePropertyAccessor")
    override fun getPath(): String = (parent?.path?.let { "$it/" } ?: "/") + name

    /** {@inheritDoc}  */
    override fun isValid(): Boolean = true

    /** {@inheritDoc}  */
    @Throws(IllegalStateException::class)
    fun addChild(file: CaosVirtualFile) {
        if (!isDirectory)
            throw IllegalStateException("Cannot add files to non-directory parent")
        if (file.isDirectory && !allowSubdirectories)
            throw IllegalStateException("Cannot add directory to directory with 'no subdirectories' set to true")
        file.parent = this
        children[file.name] = file
    }

    /** {@inheritDoc}  */
    override fun getChildren(): Array<CaosVirtualFile> {
        return children.values.toTypedArray()
    }

    internal fun childrenAsList(): List<CaosVirtualFile> = listOf(*children.values.toTypedArray())

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun getOutputStream(
        requestor: Any?,
        l: Long,
        l1: Long
    ): OutputStream {
        return ByteArrayOutputStream()
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun contentsToByteArray(): ByteArray {
        return byteArrayContents ?: stringContents?.toByteArray() ?: ByteArray(0)
    }

    /** {@inheritDoc}  */
    override fun getTimeStamp(): Long = 0L

    /** {@inheritDoc}  */
    override fun getLength(): Long = byteArrayContents?.size?.toLong() ?: stringContents?.length?.toLong() ?: 0L

    /** {@inheritDoc}  */
    override fun refresh(
        b: Boolean,
        b1: Boolean,
        runnable: Runnable?
    ) {
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())

    /** {@inheritDoc}  */
    override fun findChild(name: String): CaosVirtualFile? {
        if (!this.isDirectory)
            throw IOException("Cannot get child file from non-directory CAOS virtual file")
        return children[name]
    }

    /** {@inheritDoc}  */
    override fun getModificationStamp(): Long = modificationStamp

    /** {@inheritDoc}  */
    override fun getUrl(): String = CAOS_VFS_SCHEMA + path

    /**
     * Deletes the specified file, if it is a child of this file
     */
    fun delete(file: CaosVirtualFile) {
        val fileName = file.name
        children.let {
            if (it[fileName] == file)
                it.remove(fileName)
        }
    }

    fun delete() {
        parent?.delete(this)
    }

    override fun toString(): String {
        return name
    }

    /** {@inheritDoc}  */
    override fun getModificationCount(): Long {
        return children.values.map { it.modificationCount }.sum()
    }

    fun createChildCaosScript(
        project: Project,
        caosVariant: CaosVariant,
        fileName: String,
        code: String
    ): CaosScriptFile {
        if (!isDirectory) {
            throw IOException("Cannot add child caos script to non-directory virtual file")
        }
        val file = CaosVirtualFile("$fileName.cos", code, false).apply {
            this@CaosVirtualFile.addChild(this)
            this.variant = caosVariant
            isWritable = true
        }
        val psiFile = (PsiManager.getInstance(project).findFile(file) as? CaosScriptFile)
            ?: PsiFileFactory.getInstance(project)
                .createFileFromText("$fileName.cos", CaosScriptLanguage, code) as CaosScriptFile

        psiFile.variant = caosVariant
        return psiFile
    }


    fun createChildWithContent(name: String, content: String?, overwrite: Boolean = true): CaosVirtualFile {
        if (hasChild(name) && !overwrite)
            throw IOException("Child with name '$name' already exists in $name")
        return CaosVirtualFile(name, content, false).let {
            it.setContent(content)
            this.addChild(it)
            if (!this.hasChild(name))
                throw IOException("Failed to properly add child to filesystem")
            it
        }
    }

    fun createChildWithContent(name: String, content: ByteArray, overwrite: Boolean = true): CaosVirtualFile {
        if (hasChild(name) && !overwrite)
            throw IOException("Child with name '$name' already exists $name")
        return CaosVirtualFile(name, content, false).let {
            it.setContent(content)
            this.addChild(it)
            if (!this.hasChild(name))
                throw IOException("Failed to properly add child to filesystem")
            it
        }
    }

    fun createChildDirectory(name: String): CaosVirtualFile {
        if (!isDirectory)
            throw NotSupportedException("Cannot create child directory in non-directory virtual file")
        val file = CaosVirtualFile(name, null, true)
        addChild(file)
        return file
    }

    override fun setBinaryContent(content: ByteArray, newModificationStamp: Long, newTimeStamp: Long) {
        setBinaryContent(content, newModificationStamp, newTimeStamp, null)
    }

    override fun setBinaryContent(content: ByteArray, newModificationStamp: Long, newTimeStamp: Long, requestor: Any?) {
        timestamp = newTimeStamp
        modificationStamp = newModificationStamp
        setContent(content)
    }

    companion object {
        /** The next id for use in VirtualFileWithId */
        val nextId = AtomicInteger(1)
    }
}