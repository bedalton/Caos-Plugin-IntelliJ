package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.bytes
import bedalton.creatures.bytes.cString
import bedalton.creatures.bytes.uInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.vfs.VirtualFile
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterOutputStream

object CobToDataObjectDecompiler {

    fun decompile(virtualFile:VirtualFile) : CobFileData? {
        val buffer = ByteStreamReader(virtualFile.contentsToByteArray())
        return try {
            decompile(buffer, virtualFile.nameWithoutExtension)
        } catch(e:Exception) {
            null
        }
    }

    fun decompile(buffer: ByteStreamReader, fileName: String?): CobFileData {
        val header = buffer.cString(4)
        buffer.position(0)
        return if (header == "cob2") {
            decompileC2Cob(buffer)
        } else {
            val decompressed = try {
                val decompressed = ByteArrayOutputStream()
                val decompressor = InflaterOutputStream(decompressed)
                decompressor.write(buffer.toByteArray())
                ByteStreamReader(decompressed.toByteArray())
            } catch (e: Exception) {
                buffer.position(0)
                if (buffer.cString(4) != "cob2")
                    return decompileC1Cob(buffer, fileName)
                buffer
            }
            decompileC2Cob(decompressed)
        }
    }


    private fun decompileC1Cob(buffer: ByteStreamReader, fileName:String?): CobFileData {
        buffer.position(0)
        val version = buffer.uInt16
        if (version > 4) {
            val message = "Invalid COB file with version: $version. Data(\"${buffer.bytes(20).joinToString("") { "${it.toChar()}" }}\")"
            val top = (0 until message.length + 4).joinToString("") {"*"}
            val error = "$top\n* $message *\n$top"
            LOGGER.severe(error)
            return CobFileData.InvalidCobData(error)
        }
        return CobFileData.C1CobData(buffer.readC1Cob(fileName))
    }

    private fun decompileC2Cob(buffer: ByteStreamReader): CobFileData {
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
