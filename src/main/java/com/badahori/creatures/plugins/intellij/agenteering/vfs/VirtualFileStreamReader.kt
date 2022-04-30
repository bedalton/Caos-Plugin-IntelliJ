package com.badahori.creatures.plugins.intellij.agenteering.vfs

import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.FileStreamByteReader
import bedalton.creatures.bytes.MemoryByteStreamReader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class VirtualFileStreamReader(private val virtualFile: VirtualFile): ByteStreamReader {

    private var mClosed = false

    private val mReader: ByteStreamReader by lazy {
        if (virtualFile !is CaosVirtualFile) {
            try {
                val ioFile = VfsUtil.virtualToIoFile(virtualFile)
                if (ioFile.exists()) {
                    return@lazy FileStreamByteReader(ioFile.path)
                }
            } catch (_: Exception) {
            }
        }
        val rawBytes = virtualFile.contentsToByteArray()
        MemoryByteStreamReader(rawBytes)
    }

    override val closed: Boolean
        get() = mClosed

    override val size: Long
        get() = mReader.size

    override fun close(): Boolean {
        val didClose = mReader.close()
        mClosed = mClosed || didClose
        return mClosed
    }

    override fun copyOfBytes(start: Int, end: Int): ByteArray {
        return mReader.copyOfBytes(start, end)
    }

    override fun copyOfRange(start: Int, end: Int): ByteStreamReader {
        return mReader.copyOfRange(start, end)
    }

    override fun duplicate(): ByteStreamReader {
        return mReader.duplicate()
    }

    override fun get(): Byte {
        return mReader.get()
    }

    override fun position(): Long {
        return mReader.position().let {
            if (it > 0)
                it
            else
                0
        }
    }

    override fun position(newPosition: Long): ByteStreamReader {
        return mReader.position(newPosition)
    }

    override fun toByteArray(): ByteArray {
        return mReader.toByteArray()
    }

    override fun canReopen(): Boolean {
        return virtualFile.isValid && !virtualFile.isDirectory && virtualFile.exists()
    }

    override fun copyAsOpened(): VirtualFileStreamReader? {
        return if (canReopen()) {
            VirtualFileStreamReader(virtualFile)
        } else {
            null
        }
    }
}