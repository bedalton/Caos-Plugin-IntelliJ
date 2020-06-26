package com.openc2e.plugins.intellij.caos.utils

import java.nio.ByteBuffer
import kotlin.experimental.and

val ByteBuffer.uByte:Short get() {
    return (this.get() and 0xff.toByte()).toShort()
}

val ByteBuffer.uShort:Int get() {
    return getUShort(false)
}

fun ByteBuffer.getUShort(bigEndian:Boolean = false): Int {
    val bytes = (if (bigEndian) getBytes(2) else getBytes(2).reversed()).map { it.toInt() }
    return bytes[0] and 0xff shl 8 or (bytes[1] and 0xff)
}

val ByteBuffer.uInt:Long get() {
    return getUInt(false)
}

fun ByteBuffer.getUInt(bigEndian: Boolean = false) : Long {
    val bytes = if (bigEndian) getBytes(4) else getBytes(4).reversed()
    return (((bytes[0].toUInt() and 0xFFu) shl 24) or
            ((bytes[1].toUInt() and 0xFFu) shl 16) or
            ((bytes[2].toUInt() and 0xFFu) shl 8) or
            (bytes[3].toUInt() and 0xFFu)).toLong()
}

private fun ByteBuffer.getBytes(length:Int) : List<Byte> {
    return (0..length).map {
        get()
    }
}