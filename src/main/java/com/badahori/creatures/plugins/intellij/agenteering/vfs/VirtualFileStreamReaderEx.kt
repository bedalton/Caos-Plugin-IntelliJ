package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.common.util.className
import com.bedalton.io.bytes.AbstractByteStreamReader
import com.bedalton.io.bytes.ByteStreamReaderEx
import com.bedalton.io.bytes.internal.InputStreamByteReaderEx
import com.bedalton.io.bytes.internal.MemoryByteStreamReaderEx
import com.intellij.openapi.vfs.VirtualFile

class VirtualFileStreamReader(
    private val virtualFile: VirtualFile,
    private val start: Long? = null,
    private val end: Long? = null
) : AbstractByteStreamReader() {
    override suspend fun createReader(): ByteStreamReaderEx {
        return VirtualFileStreamReaderEx(virtualFile, start, end)
    }

}

class VirtualFileStreamReaderEx(
    private val virtualFile: VirtualFile,
    private val start: Long?,
    private val end: Long?
) : ByteStreamReaderEx {

    constructor(virtualFile: VirtualFile) : this(virtualFile, null, null)
    constructor(virtualFile: VirtualFile, start: Long?) : this(virtualFile, start, null)

    private var mClosed = false

    private val mReader: ByteStreamReaderEx by lazy {
        if (virtualFile !is CaosVirtualFile && virtualFile.length > MAX_IN_MEMORY_STREAM_LENGTH) {
            try {
                    return@lazy InputStreamByteReaderEx(start, end) {
                        try {
                            virtualFile.inputStream
                        } catch (e: Exception) {
                            LOGGER.severe("VirtualByteStreamReader getInputStream failed; ${e.className}: ${e.message}\n${e.stackTraceToString()}")
                            null
                        }
                    }
            } catch (_: Exception) {
            }
        }
        var rawBytes = virtualFile.contentsToByteArray()
        if (start != null || end != null) {
            rawBytes = rawBytes.sliceArray((start?.toInt() ?: 0)..(end?.toInt() ?: rawBytes.lastIndex))
        }
        MemoryByteStreamReaderEx(rawBytes)
    }

    override val closed: Boolean
        get() = mClosed

    override val size: Long
        get() = mReader.size

    override suspend fun close(): Boolean {
        val didClose = mReader.close()
        mClosed = mClosed || didClose
        return mClosed
    }

    override suspend fun copyOfBytes(start: Long, end: Long): ByteArray {
        return mReader.copyOfBytes(start, end)
    }

    override suspend fun copyOfRange(start: Long, end: Long): ByteStreamReaderEx {
        return VirtualFileStreamReaderEx(virtualFile, (this.start ?: 0) + start, (this.start ?: 0) + end)
    }

    override suspend fun duplicate(): ByteStreamReaderEx {
        return VirtualFileStreamReaderEx(virtualFile, start, end).apply {
            this.setPosition(position())
        }
    }

    override suspend fun get(): Byte {
        return mReader.get()
    }

    override fun position(): Long {
        return mReader.position().let {
            if (it > 0L)
                it
            else
                0L
        }
    }

    override suspend fun setPosition(newPosition: Long): ByteStreamReaderEx {
        return mReader.setPosition(newPosition)
    }

    override suspend fun toByteArray(): ByteArray {
        return mReader.toByteArray()
    }

    override fun canReopen(): Boolean {
        return virtualFile.isValid && !virtualFile.isDirectory && virtualFile.exists()
    }

    override suspend fun copyAsOpened(): VirtualFileStreamReaderEx? {
        return if (canReopen()) {
            VirtualFileStreamReaderEx(virtualFile, start, end)
        } else {
            null
        }
    }

    companion object {
        internal const val MAX_IN_MEMORY_STREAM_LENGTH = 30 * 1000 * 1000 // 30 megabytes
    }
}