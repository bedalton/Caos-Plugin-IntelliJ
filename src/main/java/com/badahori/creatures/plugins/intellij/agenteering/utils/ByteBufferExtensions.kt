@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder


private fun byteToUInt(b: Byte): Int {
    return if (b < 0) b + 256 else b.toInt()
}


val ByteBuffer.byte :Int get() {
    return get().toInt()
}

val ByteBuffer.int8:Int get() = get().toInt()

val ByteBuffer.uInt8: Int
    get() = byte and 0xFF

val ByteBuffer.int16: Int
    get() {
        return short.toInt()
    }

val ByteBuffer.int16BE: Int get() {
    val currentEncoding = order()
    order(ByteOrder.BIG_ENDIAN)
    val value = short.toInt()
    order(currentEncoding)
    return value
}

val ByteBuffer.int16LE: Int get() {
    val currentEncoding = order()
    order(ByteOrder.LITTLE_ENDIAN)
    val value = short.toInt()
    order(currentEncoding)
    return value
}

val ByteBuffer.uInt16: Int
    get() {
        return byteToUInt(get()) + (byteToUInt(get()) shl 8)
    }

val ByteBuffer.uInt16LE: Int get() {
    val currentEncoding = order()
    order(ByteOrder.LITTLE_ENDIAN)
    val value = (get().toInt() shl 8) + get()
    order(currentEncoding)
    return value
}

val ByteBuffer.uInt16BE: Int get() {
    val currentEncoding = order()
    order(ByteOrder.BIG_ENDIAN)
    val value = short.toInt() and 0xffff
    order(currentEncoding)
    return value
}

val ByteBuffer.int32:Int get() = int

val ByteBuffer.uInt32: Long
    get() {
        return int.toLong() and 0xffffffffL
    }

fun ByteBuffer.cString(length: Int) : String {
    return bytes(length).joinToString("") { it.toChar()+"" }
}
fun ByteBuffer.cString(length: Long) : String {
    return bytes(length).joinToString("") { it.toChar()+"" }
}

val ByteBuffer.cString : String get() {
    val stringBuilder = StringBuilder()
    while(true) {
        try {
            val byte = get()
            if (byte == 0.toByte()) {
                break
            }
            stringBuilder.append(byte.toChar())
        } catch(e:Exception) { break }

    }
    return stringBuilder.toString()
}

fun ByteBuffer.bytes(length: Int): ByteArray {
    return (0 until length).map {
        get()
    }.toByteArray()
}

fun ByteBuffer.bytes(length: Long): ByteArray {
    return (0 until length).map {
        get()
    }.toByteArray()
}

fun ByteBuffer.skip(length: Int) {
    bytes(length)
}

fun ByteBuffer.writeUInt64(u: Long) {
    this.putLong(u)
}

fun ByteBuffer.writeUInt32(u: Int) {
    this.putInt(u)
}

fun ByteBuffer.writeUInt32(u: Long) {
    this.putInt(u.toInt())
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun ByteBuffer.writeUInt32BE(u: Long) {
    assert(u >= 0 && u <= 1L shl 32) { "The given long is not in the range of uint32 ($u)" }
    this.writeUInt16BE(u.toInt() and 0xFFFF)
    this.writeUInt16BE((u shr 16 and 0xFFFF) as Int)
}


fun ByteBuffer.writeUInt24(iIn: Int) {
    var i = iIn
    i = i and 0xFFFFFF
    this.writeUInt16(i shr 8)
    this.writeUInt8(i)
}


fun ByteBuffer.writeUInt16(iIn: Int) {
    var i = iIn
    i = i and 0xFFFF
    this.writeUInt8(i shr 8)
    this.writeUInt8(i and 0xFF)
}

fun ByteBuffer.writeUInt16BE(iIn: Int) {
    var i = iIn
    i = i and 0xFFFF
    this.writeUInt8(i and 0xFF)
    this.writeUInt8(i shr 8)
}

fun ByteBuffer.writeUInt8(iIn: Int) {
    var i = iIn
    i = i and 0xFF
    this.put(i.toByte())
}

fun ByteBuffer.littleEndian() :ByteBuffer {
    order(ByteOrder.LITTLE_ENDIAN)
    return this
}