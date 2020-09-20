package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import java.io.IOException
import java.nio.ByteBuffer


internal class SfcReader(internal val byteBuffer: ByteBuffer) {
    lateinit var variant: CaosVariant
    val storage:MutableList<SfcData> = mutableListOf()
    val types:MutableMap<Int,Int> = mutableMapOf()

    fun readFile() : SfcFile {
        byteBuffer.position(0)
        TODO("Implement reading SFC Files")
    }

    fun slurp(type:Int) : SfcData? {
        TODO("Implement SFC data slurping")
    }

    val uInt8 get() = byteBuffer.uInt8
    val uInt16 get() = byteBuffer.uInt16
    val uInt32 get() = byteBuffer.uInt32.toInt()
    fun skip(bytes:Int) = byteBuffer.skip(bytes)

    val sfcString:String get() {
        val length = uInt8.let { small ->
            if (small == 0xFF)
                uInt16.let { medium ->
                    if (medium == 0xFFFF)
                        uInt32
                    else
                        medium
                }
            else
                small
        }
        return byteBuffer.cString(length)
    }

    companion object {
        fun readFile(bytes:ByteArray) : SfcFile {
            val byteBuffer = ByteBuffer.wrap(bytes).littleEndian()
            val dumper = SfcReader(byteBuffer)
            return dumper.readFile()
        }
    }

}


internal open class SfcReadException(message:String) : IOException(message)

internal class OutOfVariantException(variant: CaosVariant) : SfcReadException("Invalid variant for SFC. Found '${variant.code}' expected: [C1,C2]")