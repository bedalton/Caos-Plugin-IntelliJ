package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.common.util.className
import com.bedalton.io.bytes.ByteStreamReader
import com.bedalton.io.bytes.InputStreamByteReader
import com.bedalton.io.bytes.MemoryByteStreamReader
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vfs.VirtualFile

internal class VirtualFileStreamReader(
    private val virtualFile: VirtualFile,
    private val start: Long?,
    private val end: Long?
) : ByteStreamReader() {

    constructor(virtualFile: VirtualFile) : this(virtualFile, null, null)

    private var mClosed = false

    private val mReader: ByteStreamReader by lazy {
        if (virtualFile !is CaosVirtualFile && virtualFile.length > MAX_IN_MEMORY_STREAM_LENGTH) {
            try {
                    return@lazy InputStreamByteReader(start, end) {
                        try {
                            virtualFile.inputStream
                        } catch (e: Exception) {
                            if (e is ProcessCanceledException) {
                                throw e
                            }
                            LOGGER.severe("VirtualByteStreamReader getInputStream failed; ${e.className}: ${e.message}\n${e.stackTraceToString()}")
                            null
                        }
                    }
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
            }
        }
        var rawBytes = virtualFile.contentsToByteArray()
        if (start != null || end != null) {
            rawBytes = rawBytes.sliceArray((start?.toInt() ?: 0)until(end?.toInt() ?: rawBytes.size))
        }
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

    override fun copyOfBytes(start: Long, end: Long): ByteArray {
        return mReader.copyOfBytes(start, end)
    }

    override fun copyOfRange(start: Long, end: Long): ByteStreamReader {
        return mReader.copyOfRange(start, end)
    }

    override fun copy(): ByteStreamReader {
        return mReader.copy()
    }

    override fun get(): Byte {
        return mReader.get()
    }

    override fun position(): Long {
        return mReader.position().let {
            if (it > 0L) {
                it
            } else {
                0L
            }
        }
    }

    override fun setPosition(newPosition: Long): ByteStreamReader {
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
            VirtualFileStreamReader(virtualFile, start, end)
        } else {
            null
        }
    }

    companion object {
        internal const val MAX_IN_MEMORY_STREAM_LENGTH = 30 * 1000 * 1000 // 30 megabytes
    }
}