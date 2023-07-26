package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.bedalton.common.util.ensureEndsWith
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

class CaosVirtualDirectory(
    parent: VirtualFile?,
    name: String,
    children: List<VirtualFile>
): VirtualFile(), ModificationTracker {

    private val mName = name
    private val mParent: VirtualFile? = parent

    private val  mTimeStamp = Date().time

    val children = children.associateBy { it.name }.toMutableMap()

    override fun getName(): String {
        return mName
    }

    override fun getFileSystem(): VirtualFileSystem {
        return mParent?.fileSystem ?: CaosVirtualFileSystem.instance
    }

    override fun getPath(): String {
        return mParent?.path.orEmpty().ensureEndsWith('/') + name
    }

    override fun isWritable(): Boolean {
        return false
    }

    override fun isDirectory(): Boolean {
        return true
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return mParent
    }

    override fun getChildren(): Array<VirtualFile> {
        return children.values.toTypedArray()
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw IllegalStateException("CaosVirtualFolder does not have an output stream")
    }

    override fun contentsToByteArray(): ByteArray {
        throw IllegalStateException("CaosVirtualFolder does not byte contents")
    }

    override fun getTimeStamp(): Long {
        return children.values.maxOfOrNull { it.timeStamp } ?: mTimeStamp
    }

    override fun getLength(): Long {
        return 0
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        return children.values.forEach { it.refresh(asynchronous, recursive, postRunnable) }
    }

    override fun getInputStream(): InputStream {
        throw IllegalStateException("CaosVirtualFolder does not have an input stream")
    }

    override fun move(requestor: Any?, newParent: VirtualFile) {
        if (newParent is CaosVirtualFile) {
            throw IllegalStateException("Cannot move files into non-physical file")
        }
        val fileSystem = newParent.fileSystem as? LocalFileSystem
            ?: throw IllegalStateException("Cannot move files into non-local filesystem")
        for (child in children.values) {
            fileSystem.moveFile(requestor, child, newParent)
        }
        try {
            fileSystem.moveFile(requestor, this, newParent)
        } catch (_: Exception) {

        }
    }
}