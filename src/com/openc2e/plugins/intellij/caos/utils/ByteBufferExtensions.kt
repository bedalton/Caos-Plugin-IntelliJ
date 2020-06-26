package com.openc2e.plugins.intellij.caos.utils

import java.nio.ByteBuffer
import kotlin.experimental.and

val ByteBuffer.uByte:Short get() {
    return (get().toShort() and 0xFF)
}

val ByteBuffer.uShort:Int get() {
    return (short.toInt() and 0x0000FFFF)
}

val ByteBuffer.uInt:Long get() {
    return int.toLong() and 0x00000000FFFFFFFFL
}

private fun ByteBuffer.getBytes(length:Int) : List<Byte> {
    return (0..length).map {
        get()
    }
}