package com.badahori.creatures.plugins.intellij.agenteering.vfs

/*
 * Copyright 2007 Steve Chaloner
 * Modified by Bedalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileWithId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * Virtual File for Caos Scripts Plugin
 */
class CaosVirtualFile internal constructor(
        private var fileName:String,
        private var content: String?,
        private val isDirectory: Boolean) : VirtualFile(), ModificationTracker, VirtualFileWithId, HasVariant {
    private val myId:Int = nextId.getAndIncrement()

    override fun getId():Int = myId

    override var variant:CaosVariant? = null

    /** {@inheritDoc}  */
    override fun getName(): String = fileName

    /** {@inheritDoc}  */
    override fun getNameWithoutExtension() : String = FileUtil.getNameWithoutExtension(name)

    /** {@inheritDoc}  */
    override fun isDirectory(): Boolean = isDirectory

    /**
     * The children of this file, if the file is a directory.
     */
    private val children: MutableMap<String, CaosVirtualFile> = HashMap()

    internal var parent: VirtualFile? = null

    /** {@inheritDoc}  */
    override fun getParent() = parent

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

    /**
     * Initialises a new instance of this class.
     *
     * @param name the name of the file
     * @param content the content of the file
     */
    constructor(name: String, content: String?) : this(name, content,false)

    /** {@inheritDoc}  */
    override fun getFileSystem(): VirtualFileSystem = VirtualFileManager.getInstance().getFileSystem(CAOS_VFS_PROTOCOL)

    /** {@inheritDoc}  */
    override fun getPath(): String {
            val parent = parent
            return if (parent == null) name else parent.path + '/' + name
        }

    /** {@inheritDoc}  */
    override fun isValid(): Boolean = true

    /**
     * Add the given file to the child list of this directory.
     *
     * @param file the file to add to the list of children
     * @throws java.lang.IllegalStateException if this file is not a directory
     */
    @Throws(java.lang.IllegalStateException::class)
    fun addChild(file: CaosVirtualFile) {
        if (isDirectory) {
            file.parent = this
            children[file.name] = file
        } else {
            throw IllegalStateException("files can only be added to a directory")
        }
    }

    /** {@inheritDoc}  */
    override fun getChildren(): Array<VirtualFile> {
        return children.values.toTypedArray()
    }

    /** {@inheritDoc}  */
    @Throws(java.io.IOException::class)
    override fun getOutputStream(requestor: Any?,
                        l: Long,
                        l1: Long): OutputStream {
        return ByteArrayOutputStream()
    }

    /** {@inheritDoc}  */
    @Throws(java.io.IOException::class)
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
    @Throws(java.io.IOException::class)
    override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())

    /**
     * Gets the file from this directory's children.
     *
     * @param name the name of the child to retrieve
     * @return the file, or null if it cannot be found
     */
    fun getChild(name: String?): CaosVirtualFile? {
        return children[name]
    }

    /** {@inheritDoc}  */
    override fun getModificationStamp(): Long = 0

    /** {@inheritDoc}  */
    override fun getUrl(): String = CAOS_VFS_SCHEMA + path

    /**
     * Deletes the specified file.
     *
     * @param file the file to delete
     */
    fun deleteChild(file: CaosVirtualFile) {
        children.remove(file.name)
    }

    override fun toString(): String {
        return name
    }

    /** {@inheritDoc}  */
    override fun getModificationCount(): Long {
        TODO("Not yet implemented")
    }

    companion object {
        val nextId = AtomicInteger(1)
    }
}
