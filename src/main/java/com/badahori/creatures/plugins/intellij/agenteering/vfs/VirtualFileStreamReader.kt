package com.badahori.creatures.plugins.intellij.agenteering.vfs

import bedalton.creatures.common.bytes.ByteStreamReader
import bedalton.creatures.common.bytes.FileStreamByteReader
import bedalton.creatures.common.bytes.InputStreamByteReader
import bedalton.creatures.common.bytes.MemoryByteStreamReader
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

    override suspend fun close(): Boolean {
        val didClose = mReader.close()
        mClosed = mClosed || didClose
        return mClosed
    }

    override suspend fun copyOfBytes(start: Int, end: Int): ByteArray {
        return mReader.copyOfBytes(start, end)
    }

    override suspend fun copyOfRange(start: Int, end: Int): ByteStreamReader {
        return mReader.copyOfRange(start, end)
    }

    override suspend fun duplicate(): ByteStreamReader {
        return mReader.duplicate()
    }

    override suspend fun get(): Byte {
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

    override suspend fun setPosition(newPosition: Int): ByteStreamReader {
        return mReader.setPosition(newPosition)
    }

    override suspend fun toByteArray(): ByteArray {
        return mReader.toByteArray()
    }

    override fun canReopen(): Boolean {
        return virtualFile.isValid && !virtualFile.isDirectory && virtualFile.exists()
    }

    override suspend fun copyAsOpened(): VirtualFileStreamReader? {
        return if (canReopen()) {
            VirtualFileStreamReader(virtualFile)
        } else {
            null
        }
    }
}