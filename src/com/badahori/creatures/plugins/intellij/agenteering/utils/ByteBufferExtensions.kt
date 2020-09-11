@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.nio.ByteBuffer


private fun byteToUInt(b: Byte): Int {
    return if (b < 0) b + 256 else b.toInt()
}


val ByteBuffer.byte :Int get() {
    return byteToUInt(get())
}

val ByteBuffer.uInt8: Int
    get() = byteToUInt(get())


val ByteBuffer.uInt16: Int
    get() {
        var result = 0
        result += byteToUInt(get()) shl 8
        result += byteToUInt(get())
        return result
    }

val ByteBuffer.uInt16BE:Int get() {
    var result = 0
    result += byteToUInt(get())
    result += byteToUInt(get()) shl 8
    return result
}

val ByteBuffer.uInt32BE: Long get() {
    val ch1: Long = uInt8.toLong()
    val ch2: Long = uInt8.toLong()
    val ch3: Long = uInt8.toLong()
    val ch4: Long = uInt8.toLong()
    return (ch4 shl 24) + (ch3 shl 16) + (ch2 shl 8) + (ch1 shl 0)
}

val ByteBuffer.uInt32: Long
    get() {
        var i: Long = int.toLong()
        if (i < 0) {
            i += 1L shl 32
        }
        return i
    }

val ByteBuffer.uInt24: Int get() {
    var result = 0
    result += uInt16 shl 8
    result += byteToUInt(get())
    return result
}

fun ByteBuffer.cString(length:Int) : String {
    return bytes(length).joinToString("") { it.toChar()+"" }
}

val ByteBuffer.cString : String get() {
    val stringBuilder = StringBuilder()
    while(true) {
        val byte = get()
        if (byte == 0.toByte()) {
            break;
        }
        stringBuilder.append(byte.toChar())
    }
    return stringBuilder.toString()
}

fun ByteBuffer.bytes(length: Int): ByteArray {
    return (0..length).map {
        get()
    }.toByteArray()
}

fun ByteBuffer.skip(length:Int) {
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


fun ByteBuffer.writeUInt24(iIn:Int) {
    var i = iIn
    i = i and 0xFFFFFF
    this.writeUInt16(i shr 8)
    this.writeUInt8(i)
}


fun ByteBuffer.writeUInt16(iIn:Int) {
    var i = iIn
    i = i and 0xFFFF
    this.writeUInt8(i shr 8)
    this.writeUInt8(i and 0xFF)
}

fun ByteBuffer.writeUInt16BE(iIn:Int) {
    var i = iIn
    i = i and 0xFFFF
    this.writeUInt8(i and 0xFF)
    this.writeUInt8(i shr 8)
}

fun ByteBuffer.writeUInt8(iIn:Int) {
    var i = iIn
    i = i and 0xFF
    this.put(i.toByte())
}
