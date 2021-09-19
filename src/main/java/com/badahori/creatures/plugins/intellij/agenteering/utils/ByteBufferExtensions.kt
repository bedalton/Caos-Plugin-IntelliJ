@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.vfs.VirtualFile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.InflaterOutputStream


private fun byteToUInt(b: Byte): Int {
    return if (b < 0) b + 256 else b.toInt()
}

//
//val ByteBuffer.byte :Int get() {
//    return get().toInt()
//}
//
//val ByteBuffer.int8:Int get() = get().toInt()
//
//val ByteBuffer.uInt8: Int
//    get() = byte and 0xFF
//
//fun ByteBuffer.peakUInt8(): Int {
//    return uInt8.apply {
//        seek(-1)
//    }
//}
//
//val ByteBuffer.int16: Int
//    get() {
//        return short.toInt()
//    }
//
//val ByteBuffer.int16BE: Int get() {
//    val currentEncoding = order()
//    order(ByteOrder.BIG_ENDIAN)
//    val value = short.toInt()
//    order(currentEncoding)
//    return value
//}
//
//val ByteBuffer.int16LE: Int get() {
//    val currentEncoding = order()
//    order(ByteOrder.LITTLE_ENDIAN)
//    val value = short.toInt()
//    order(currentEncoding)
//    return value
//}
//
//val ByteBuffer.uInt16: Int
//    get() {
//        return byteToUInt(get()) + (byteToUInt(get()) shl 8)
//    }
//
//val ByteBuffer.uInt16LE: Int get() {
//    val currentEncoding = order()
//    order(ByteOrder.LITTLE_ENDIAN)
//    val value = (get().toInt() shl 8) + get()
//    order(currentEncoding)
//    return value
//}
//
//val ByteBuffer.uInt16BE: Int get() {
//    val currentEncoding = order()
//    order(ByteOrder.BIG_ENDIAN)
//    val value = short.toInt() and 0xffff
//    order(currentEncoding)
//    return value
//}
//
//val ByteBuffer.int32:Int get() = int
//
//val ByteBuffer.uInt32: Long
//    get() {
//        val currentEncoding = order()
//        order(ByteOrder.LITTLE_ENDIAN)
//        val value = int.toLong() and 0xffffffffL
//        order(currentEncoding)
//        return value
//    }
//
//fun ByteBuffer.cString(length: Int) : String {
//    return bytes(length).joinToString("") { it.toChar()+"" }
//}
//fun ByteBuffer.cString(length: Long) : String {
//    return bytes(length).joinToString("") { it.toChar()+"" }
//}

private const val NULL_BYTE:Byte = 0.toByte()
//
//val ByteBuffer.cString : String get() {
//    val stringBuilder = StringBuilder()
//    while(true) {
//        try {
//            val byte = get()
//            if (byte == NULL_BYTE) {
//                break
//            }
//            stringBuilder.append(byte.toChar())
//        } catch(e:Exception) { break }
//
//    }
//    return stringBuilder.toString()
//}
//
//fun ByteBuffer.bytes(length: Int): ByteArray {
//    return (0 until length).map {
//        get()
//    }.toByteArray()
//}
//
//fun ByteBuffer.bytes(length: Long): ByteArray {
//    return (0 until length).map {
//        get()
//    }.toByteArray()
//}
//
//fun ByteBuffer.skip(length: Int) {
//    seek(length)
//}
//
//fun ByteBuffer.seek(offset:Int) {
//    (this as Buffer).position((this as Buffer).position() + offset)
//}
//
//fun ByteBuffer.writeUInt64(u: Long) {
//    this.putLong(u)
//}
//
//fun ByteBuffer.writeUInt32(u: Int) {
//    this.putInt(u)
//}
//
//fun ByteBuffer.writeUInt32(u: Long) {
//    this.putInt(u.toInt())
//}
//
//@Suppress("CAST_NEVER_SUCCEEDS")
//fun ByteBuffer.writeUInt32BE(u: Long) {
//    assert(u >= 0 && u <= 1L shl 32) { "The given long is not in the range of uint32 ($u)" }
//    this.writeUInt16BE(u.toInt() and 0xFFFF)
//    this.writeUInt16BE((u shr 16 and 0xFFFF) as Int)
//}
//
//
//fun ByteBuffer.writeUInt24(iIn: Int) {
//    var i = iIn
//    i = i and 0xFFFFFF
//    this.writeUInt16(i shr 8)
//    this.writeUInt8(i)
//}
//
//
//fun ByteBuffer.writeUInt16(iIn: Int) {
//    var i = iIn
//    i = i and 0xFFFF
//    this.writeUInt8(i shr 8)
//    this.writeUInt8(i and 0xFF)
//}
//
//fun ByteBuffer.writeUInt16BE(iIn: Int) {
//    var i = iIn
//    i = i and 0xFFFF
//    this.writeUInt8(i and 0xFF)
//    this.writeUInt8(i shr 8)
//}
//
//fun ByteBuffer.writeUInt8(iIn: Int) {
//    var i = iIn
//    i = i and 0xFF
//    this.put(i.toByte())
//}
//
//fun ByteBuffer.littleEndian() :ByteBuffer {
//    order(ByteOrder.LITTLE_ENDIAN)
//    return this
//}


fun ByteArray.decompress() : ByteArray {
    return try {
        val decompressed = ByteArrayOutputStream()
        val decompressor = InflaterOutputStream(decompressed)
        decompressor.write(this)
        decompressed.toByteArray()
    } catch (e:Exception) {
        this
    } ?: this
}

fun ByteArray.compressed() : ByteArray {
    val compressor = Deflater()
    val data = this.copyOf()
    compressor.setInput(data)
    compressor.finish()
    val bytesWritten = compressor.bytesWritten
    compressor.end()
    return data.copyOfRange(0, bytesWritten.toInt())
}

fun VirtualFile.getAllBytes() : ByteArray {
    return inputStream.getAllBytes()
}

fun InputStream.getAllBytes() : ByteArray {
    val buffer = ByteArrayOutputStream()
    var nRead: Int
    val data = ByteArray(16384)

    while (this.read(data, 0, data.size).also { nRead = it } != -1) {
        buffer.write(data, 0, nRead)
    }
    return buffer.toByteArray()
}

val ByteArray.sizeString:String get() {
    val size = size
    return when {
        size > 1_000_000_000_000 -> String.format("%.2f TB", size / 1_000_000_000_000.0)
        size > 1_000_000_000 -> String.format("%.2f GB", size / 1_000_000_000.0)
        size > 1_000_000 -> String.format("%.2f MB", size / 1_000_000.0)
        size > 1_000 -> String.format("%.2f KB", size / 1_000.0)
        else -> "${size} bytes"
    }
}