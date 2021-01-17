@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

private val NULL_BYTE = byteArrayOf(0)

private fun byteToUInt(b: Byte): Int {
    return if (b < 0) b + 256 else b.toInt()
}

fun OutputStream.writeUint32(valueIn: Long) {
    var value = valueIn
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = (value.toByte())
        value = value shr 8
    }
    write(bytes)
}
fun OutputStream.writeUint32(valueIn: Int) {
    var value = valueIn
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = (value.toByte())
        value = value shr 8
    }
    write(bytes)
}

fun intToUInt32Bytes(valueIn: Int) : ByteArray {
    var value = valueIn
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = (value.toByte())
        value = value shr 8
    }
    return bytes
}


fun OutputStream.writeNullTerminatedString(text:String, encoding:Charset = Charsets.UTF_8) {
    write(text.toByteArray(encoding))
    write(NULL_BYTE)
}

fun OutputStream.writeInt32(valueIn: Int) {
    write(valueIn)
}

fun OutputStream.writeInt16(v:Int) {
    write(byteArrayOf((v shr 0 and 0xff).toByte(), (v shr 8 and 0xff).toByte()))
}

fun OutputStream.writeUInt16(valueIn: Int) {
    var value = valueIn
    val bytes = ByteArray(2)
    for (i in 0 until 2) {
        bytes[i] = (value.toByte())
        value = value shr 8
    }
    write(bytes)
}

fun intToUInt16Bytes(valueIn: Int) : ByteArray {
    var value = valueIn
    val bytes = ByteArray(2)
    for (i in 0 until 2) {
        bytes[i] = (value.toByte())
        value = value shr 8
    }
    return bytes
}

fun OutputStream.writeUInt8(valueIn: Int) {
    write(byteArrayOf(valueIn.toByte()))
}
fun OutputStream.writeUInt8(valueIn: Byte) {
    write(byteArrayOf(valueIn))
}

fun OutputStream.writeSfcString(string:String) {
    string.length.let { length ->
        if (length > 255) {
            writeUInt8(255.toByte())
            writeUInt16(length)
        } else {
            writeUInt8(length.toByte())
        }
    }
    write(string.toByteArray(Charsets.UTF_8))
}

fun OutputStream.writeNullByte() {
    write(NULL_BYTE)
}

fun ByteOutputStream.trimmed() : ByteArray {
    return bytes.sliceArray(0..size())
}