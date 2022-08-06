package com.badahori.creatures.plugins.intellij.agenteering.vfs

import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.FileStreamByteReader
import bedalton.creatures.bytes.InputStreamByteReader
import bedalton.creatures.bytes.MemoryByteStreamReader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class VirtualFileStreamReader(private val virtualFile: VirtualFile, start: Int? = null, end: Int? = null): ByteStreamReader {

    constructor(virtualFile: VirtualFile): this (virtualFile, null, null)

    private var mClosed = false

    private val mReader: ByteStreamReader by lazy {
        if (virtualFile !is CaosVirtualFile) {
            try {
                    return@lazy InputStreamByteReader(start, end) {
                        virtualFile.inputStream
                    }
            } catch (_: Exception) {
            }
        }
        val rawBytes = virtualFile.contentsToByteArray()
        MemoryByteStreamReader(rawBytes)
    }

    override val closed: Boolean
        get() = mClosed

    override val size: Int
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

    override fun position(): Int {
        return mReader.position().let {
            if (it > 0)
                it
            else
                0
        }
    }

    override fun setPosition(newPosition: Int): ByteStreamReader {
        return mReader.setPosition(newPosition)
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