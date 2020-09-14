package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileWithId
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * Virtual File for Caos Scripts Plugin
 */
class CaosVirtualFile internal constructor(
        private var fileName:String,
        private var content: String?,
        private val isDirectory: Boolean,
        private val allowSubdirectories:Boolean = isDirectory
        ) : VirtualFile(), ModificationTracker, VirtualFileWithId, HasVariant {

    constructor(name: String, content: String?) : this(name, content,false)

    // To support VirtualFileWithId
    private val myId:Int = nextId.getAndIncrement()

    /** {@inheritDoc}  */
    override fun getId():Int = myId

    /** Allows Quick access to CAOS Variant */
    override var variant:CaosVariant? = null

    /** {@inheritDoc}  */
    override fun getName(): String = fileName

    fun setName(name:String) {
        fileName = name
    }

    /** {@inheritDoc}  */
    override fun getNameWithoutExtension() : String = FileUtil.getNameWithoutExtension(name)

    /** {@inheritDoc}  */
    override fun isDirectory(): Boolean = isDirectory

    /**
     * The children of this file, if the file is a directory.
     */
    private val children: MutableMap<String, CaosVirtualFile> = HashMap()

    /** Parent Virtual file */
    internal var parent: CaosVirtualFile? = null

    /** {@inheritDoc}  */
    override fun getParent():CaosVirtualFile? = parent

    /**
     * Immutability flag
     */
    private var isWritable = false

    /** {@inheritDoc}  */
    override fun isWritable(): Boolean = isWritable

    /** {@inheritDoc}  */
    override fun setWritable(writable:Boolean) {
        isWritable = writable
    }

    /** {@inheritDoc}  */
    override fun getFileSystem(): VirtualFileSystem = CaosVirtualFileSystem.instance

    /** {@inheritDoc}  */
    @Suppress("RecursivePropertyAccessor")
    override fun getPath(): String = parent?.path?.let { "$it/$name" } ?: name

    /** {@inheritDoc}  */
    override fun isValid(): Boolean = true

    /** {@inheritDoc}  */
    @Throws(java.lang.IllegalStateException::class)
    fun addChild(file: CaosVirtualFile) {
        if (!isDirectory)
            throw IllegalStateException("Cannot add files to non-directory parent")
        if (file.isDirectory && !allowSubdirectories)
            throw IllegalStateException("Cannot add directory to directory with 'no subdirectories' set to true")
        file.parent = this
        children[file.name.toLowerCase()] = file
    }

    /** {@inheritDoc}  */
    override fun getChildren(): Array<VirtualFile> {
        return children.values.toTypedArray()
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun getOutputStream(requestor: Any?,
                        l: Long,
                        l1: Long): OutputStream {
        return ByteArrayOutputStream()
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun contentsToByteArray(): ByteArray {
        return content?.toByteArray() ?: ByteArray(0)
    }

    /** {@inheritDoc}  */
    override fun getTimeStamp(): Long = 0L

    /** {@inheritDoc}  */
    override fun getLength(): Long = content?.length?.toLong() ?: 0L

    /** {@inheritDoc}  */
    override fun refresh(b: Boolean,
                b1: Boolean,
                runnable: Runnable?) {
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())

    /** {@inheritDoc}  */
    override fun findChild(name: String): CaosVirtualFile? {
        return children[name.toLowerCase()]
    }

    /** {@inheritDoc}  */
    override fun getModificationStamp(): Long = 0

    /** {@inheritDoc}  */
    override fun getUrl(): String = CAOS_VFS_SCHEMA + path

    /**
     * Deletes the specified file, if it is a child of this file
     */
    fun deleteChild(file: CaosVirtualFile) {
        val fileName = file.name.toLowerCase()
        children.let {
            if (it[fileName] == file)
                it.remove(fileName)
        }
    }

    override fun toString(): String {
        return name
    }

    /** {@inheritDoc}  */
    override fun getModificationCount(): Long {
        return children.values.map { it.modificationCount }.sum()
    }

    companion object {
        /** The next id for use in VirtualFileWithId */
        val nextId = AtomicInteger(1)
    }
}