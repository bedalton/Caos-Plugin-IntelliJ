package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.util.io.toByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.InflaterOutputStream

object CobDecompiler {

    public fun decompile(buffer: ByteBuffer): CobFileData {
        val header = buffer.cString(4)
        buffer.position(0)
        return if (header == "cob2") {
            decompileC2Cob(buffer)
        } else try {
            val decompressed = ByteArrayOutputStream()
            val decompressor = InflaterOutputStream(decompressed)
            decompressor.write(buffer.toByteArray())
            decompileC2Cob(ByteBuffer.wrap(decompressed.toByteArray()))
        } catch (e: Exception) {
            decompileC1Cob(buffer)
        }
    }


    private fun decompileC1Cob(buffer: ByteBuffer): CobFileData.C1CobData {
        val version = buffer.uInt8
        if (version > 4)
            throw Exception("Invalid COB file")
        return CobFileData.C1CobData(buffer.readC1Cob())
    }

    private fun decompileC2Cob(buffer: ByteBuffer): CobFileData.C2CobData {
        val header = buffer.cString(4)
        if (header != "cob2")
            throw Exception("Invalid C2 Cob file")
        val blocks = mutableListOf<CobBlock>()
        while(true) {
            val block = buffer.readC2CobBlock()
                    ?: break
            blocks.add(block)
        }
        return CobFileData.C2CobData(blocks)
    }

}
