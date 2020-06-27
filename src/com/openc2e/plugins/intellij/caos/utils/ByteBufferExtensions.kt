package com.openc2e.plugins.intellij.caos.utils

import java.nio.ByteBuffer


private fun byte2int(b: Byte): Int {
    return if (b < 0) b + 256 else b.toInt()
}


val ByteBuffer.uInt8: Int
    get() = byte2int(get())


val ByteBuffer.uInt16: Int
    get() {
        var result = 0
        result += byte2int(get()) shl 8
        result += byte2int(get())
        return result
    }

val ByteBuffer.uInt16BE:Int get() {
    var result = 0
    result += byte2int(get())
    result += byte2int(get()) shl 8
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
    result += byte2int(get())
    return result
}

private fun ByteBuffer.getBytes(length: Int): List<Byte> {
    return (0..length).map {
        get()
    }
}