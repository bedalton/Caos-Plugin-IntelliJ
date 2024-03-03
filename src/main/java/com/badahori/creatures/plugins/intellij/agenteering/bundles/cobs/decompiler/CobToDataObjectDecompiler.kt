package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.common.util.formatted
import com.bedalton.common.util.trySilent
import com.bedalton.io.bytes.*
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterOutputStream

object CobToDataObjectDecompiler {

    fun decompile(virtualFile: VirtualFile): CobFileData? {
        return runBlocking {
            try {
                MemoryByteStreamReader(virtualFile.contentsToByteArray()).readAtCurrentPosition {
                    decompile(this, virtualFile.nameWithoutExtension)
                }
            } catch (e: Exception) {
                LOGGER.info(e.message ?: "Failed to decompile COB: ${virtualFile.name}; ${e.formatted(true)}")
                null
            }
        }
    }

    suspend fun decompile(buffer: ByteStreamReader, fileName: String?): CobFileData {
        val header = buffer.string(4)
        buffer.setPosition(0)
        if (header == "cob2") {
            return decompileC2Cob(buffer)
        } else {
            trySilent<CobFileData> {
                return decompileC1Cob(buffer, fileName)
            }
            val decompressed = decompressed(buffer)
                ?: throw Exception("COB is invalid, or could not be decompressed")
            return if (decompressed.peakToken(0) == token("cob2")) {
                decompileC2Cob(decompressed)
            } else {
                try {
                    decompileC1Cob(decompressed, fileName)
                } catch (e: Exception) {
                    throw Exception("Could not parse cob data for $fileName; ${e.formatted(true)}")
                }
            }
        }
    }


    private suspend fun decompressed(buffer: ByteStreamReader): ByteStreamReader? {
        return try {
            val decompressed = ByteArrayOutputStream()
            val decompressor = InflaterOutputStream(decompressed)
            withContext(Dispatchers.IO) {
                decompressor.write(buffer.toByteArray())
            }
            val decompressedBytes = decompressed.toByteArray()
            MemoryByteStreamReader(decompressedBytes)
        } catch (e: Exception) {
            return null
        }
    }

    private fun decompileC1Cob(buffer: ByteStreamReader, fileName: String?): CobFileData {
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

    private fun decompileC2Cob(buffer: ByteStreamReader): CobFileData {
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
