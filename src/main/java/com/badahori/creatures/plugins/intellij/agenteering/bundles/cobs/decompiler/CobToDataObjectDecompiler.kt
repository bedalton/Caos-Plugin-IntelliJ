package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import bedalton.creatures.common.bytes.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterOutputStream

object CobToDataObjectDecompiler {

    fun decompile(virtualFile: VirtualFile): CobFileData? {
        val buffer = MemoryByteStreamReader(virtualFile.contentsToByteArray())
        return try {
            runBlocking { decompile(buffer, virtualFile.nameWithoutExtension) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun decompile(buffer: ByteStreamReader, fileName: String?): CobFileData {
        val header = buffer.string(4)
        buffer.setPosition(0)
        return if (header == "cob2") {
            decompileC2Cob(buffer)
        } else {
            val decompressed = try {
                val decompressed = ByteArrayOutputStream()
                val decompressor = InflaterOutputStream(decompressed)
                decompressor.write(buffer.toByteArray())
                MemoryByteStreamReader(decompressed.toByteArray())
            } catch (e: Exception) {
                buffer.setPosition(0)
                if (buffer.string(4) != "cob2") {
                    return decompileC1Cob(buffer, fileName)
                }
                buffer
            }
            decompileC2Cob(decompressed)
        }
    }


    private suspend fun decompileC1Cob(buffer: ByteStreamReader, fileName: String?): CobFileData {
        buffer.setPosition(0)
        val version = buffer.uInt16()
        if (version > 4) {
            val message = "Invalid COB file with version: $version. Data(\"${
                buffer.bytes(20).joinToString("") { "${it.toInt().toChar()}" }
            }\")"
            val top = (0 until message.length + 4).joinToString("") { "*" }
            val error = "$top\n* $message *\n$top"
            LOGGER.severe(error)
            return CobFileData.InvalidCobData(error)
        }
        return CobFileData.C1CobData(buffer.readC1Cob(fileName))
    }

    private suspend fun decompileC2Cob(buffer: ByteStreamReader): CobFileData {
        buffer.setPosition(0)
        val header = buffer.string(4)
        if (header != "cob2")
            throw Exception("Invalid C2 Cob file")
        val blocks = mutableListOf<CobBlock>()
        while (true) {
            val block = buffer.readC2CobBlock()
                ?: break
            blocks.add(block)
        }
        return CobFileData.C2CobData(blocks)
    }

}
