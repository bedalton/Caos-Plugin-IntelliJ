package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.bytes
import com.badahori.creatures.plugins.intellij.agenteering.utils.cString
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt16
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.toByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.InflaterOutputStream

object CobToDataObjectDecompiler {

    fun decompile(virtualFile:VirtualFile) : CobFileData? {
        val buffer = ByteBuffer.wrap(virtualFile.contentsToByteArray()).littleEndian()
        return try {
            decompile(buffer)
        } catch(e:Exception) {
            null
        }
    }

    fun decompile(buffer: ByteBuffer): CobFileData {
        val header = buffer.cString(4)
        buffer.position(0)
        return if (header == "cob2") {
            decompileC2Cob(buffer)
        } else {
            val decompressed = try {
                val decompressed = ByteArrayOutputStream()
                val decompressor = InflaterOutputStream(decompressed)
                decompressor.write(buffer.toByteArray())
                ByteBuffer.wrap(decompressed.toByteArray())
            } catch (e: Exception) {
                buffer.position(0)
                if (buffer.cString(4) != "cob2")
                    return decompileC1Cob(buffer)
                buffer
            }
            decompileC2Cob(decompressed.littleEndian())
        }
    }


    private fun decompileC1Cob(buffer: ByteBuffer): CobFileData {
        buffer.position(0)
        val version = buffer.uInt16
        if (version > 4) {
            val message = "Invalid COB file with version: $version. Data(\"${buffer.bytes(20).joinToString("") { "${it.toChar()}" }}\")"
            val top = (0 until message.length + 4).joinToString("") {"*"}
            val error = "$top\n* $message *\n$top"
            LOGGER.severe(error)
            return CobFileData.InvalidCobData(error)
        }
        return CobFileData.C1CobData(buffer.readC1Cob())
    }

    private fun decompileC2Cob(buffer: ByteBuffer): CobFileData {
        buffer.position(0)
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
