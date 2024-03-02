package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeNullTerminatedString
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt8
import com.bedalton.common.util.PathUtil
import com.bedalton.creatures.sprite.compilers.S16Compiler
import com.bedalton.creatures.sprite.util.ColorEncoding
import com.bedalton.io.bytes.ByteStreamWriter
import com.bedalton.io.bytes.CREATURES_CHARACTER_ENCODING
import com.intellij.openapi.vfs.VirtualFile
import korlibs.image.bitmap.Bitmap32
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class Caos2CobC2(
    override val agentName: String,
    override val targetFile: String,
    val description: String? = null,
    val quantityAvailable: Int? = null,
    val expiresMonth: Int? = null,
    val expiresDay: Int? = null,
    val expiresYear: Int? = null,
    val objectScripts: List<String> = listOf(),
    val installScript: String?,
    val removalScript: String?,
    val quantityUsed: Int? = null,
    val pictureUrl: String? = null,
    val reuseInterval: Int? = null,
    // Author blocks
    val creationTime: String? = null,
    val version: Int? = null,
    val revision: Int? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val authorURL: String? = null,
    val authorComments: String? = null,
    val dependencies: Set<String>,
    val filesToInline: List<VirtualFile>
) : Caos2Cob {

    constructor(
        cobData: Map<CobTag, String?>,
        installScript: String?,
        objectScripts: List<String>,
        removalScript: String?,
        depends: Set<String>,
        inline: List<VirtualFile>,
        yearMonthDay: List<Int?> = cobData[CobTag.EXPIRY]?.split("-")?.map { it.toIntSafe() }.orEmpty()
    ) : this(
        agentName = cobData[CobTag.AGENT_NAME]
            ?: throw Caos2CobException("Cannot make C2 COB without 'Agent Name' tag"),
        targetFile = cobData[CobTag.COB_NAME] ?: throw Caos2CobException("Cannot make C2 COB without 'COB Name' tag"),
        description = cobData[CobTag.DESCRIPTION],
        quantityAvailable = cobData[CobTag.QUANTITY_AVAILABLE]?.toIntSafe(),
        expiresYear = yearMonthDay.getOrNull(0),
        expiresMonth = yearMonthDay.getOrNull(1),
        expiresDay = yearMonthDay.getOrNull(2),
        objectScripts = objectScripts,
        installScript = installScript,
        removalScript = removalScript,
        quantityUsed = cobData[CobTag.QUANTITY_USED]?.toIntSafe(),
        pictureUrl = cobData[CobTag.THUMBNAIL],
        creationTime = cobData[CobTag.CREATION_DATE],
        version = cobData[CobTag.VERSION]?.toIntSafe(),
        revision = cobData[CobTag.REVISION]?.toIntSafe(),
        authorName = cobData[CobTag.AUTHOR_NAME],
        authorEmail = cobData[CobTag.AUTHOR_EMAIL],
        authorURL = cobData[CobTag.AUTHOR_URL],
        authorComments = cobData[CobTag.AUTHOR_COMMENTS],
        reuseInterval = cobData[CobTag.REUSE_INTERVAL]?.toIntSafe(),
        dependencies = depends,
        filesToInline = inline
    )

    private val thumbnail: Bitmap32? by lazy {
        pictureUrl?.let { url ->
            loadThumbnail(url)
        }
    }

    override suspend fun compile(): ByteArray {
        // Initialize Output
        return ByteStreamWriter.writeBytes {
            write(COB2_HEADER)
            // Write Blocks
            writeAgent(this)
            writeAuthor(this)
            writeFiles(this)
        }
    }

    private fun writeAgent(outputStream: ByteStreamWriter) {
        val chunk = ByteStreamWriter.writeBytes {
            val buffer = this
            buffer.writeUInt16(quantityAvailable ?: -1)
            buffer.writeUInt32(0)
            buffer.writeUInt32(reuseInterval ?: 0)
            buffer.writeUInt8(expiresDay ?: 0)
            buffer.writeUInt8(expiresMonth ?: 0)
            buffer.writeUInt16(expiresYear ?: 0)
            buffer.writeUInt32(0) // Reserved 1
            buffer.writeUInt32(0) // Reserved 2
            buffer.writeUInt32(0) // Reserved 3
            buffer.writeNullTerminatedString(agentName)
            buffer.writeNullTerminatedString(description ?: "")
            buffer.writeNullTerminatedString(installScript ?: "")
            buffer.writeNullTerminatedString(removalScript ?: "")
            buffer.writeUInt16(objectScripts.size)
            objectScripts.forEach { script -> buffer.writeNullTerminatedString(script) }
            buffer.writeUInt16(dependencies.size)
            dependencies.forEach { fileName ->
                val tag = when (PathUtil.getExtension(fileName)?.lowercase()) {
                    "s16" -> 0
                    "wav" -> 1
                    else -> throw Caos2CobException("Invalid dependency declared. Valid filetypes are S16 and WAV")
                }
                buffer.writeUInt16(tag)
                buffer.writeNullTerminatedString(fileName)
            }

            val thumbnail = thumbnail

            buffer.writeUInt16(thumbnail?.width ?: 0)
            buffer.writeUInt16(thumbnail?.height ?: 0)
            if (thumbnail != null) {
                S16Compiler.writeCompiledImage(thumbnail, buffer, false, ColorEncoding.X_565)
            }
        }

        // Actually write chunk
        writeChunk(outputStream, AGNT_HEADER, chunk)

    }

    private fun writeAuthor(outputStream: ByteStreamWriter) {
        if (!hasAuthProperties)
            return
        val buffer = ByteArrayOutputStream()
        val creationTime = (creationTime?.trim() ?: DATE_FORMAT.format(Date())).split("-").mapNotNull { it.toIntSafe() }
        if (creationTime.size != 3) {
            throw Caos2CobException("Invalid creation date format. Expected YYYY-MM-DD. Found '${creationTime}'")
        }
        buffer.writeUInt8(creationTime[2])
        buffer.writeUInt8(creationTime[1])
        buffer.writeUInt16(creationTime[0])
        buffer.writeUInt8(version ?: 1)
        buffer.writeUInt8(revision ?: 0)
        buffer.writeNullTerminatedString(authorName ?: "", CREATURES_CHARACTER_ENCODING)
        buffer.writeNullTerminatedString(authorEmail ?: "", CREATURES_CHARACTER_ENCODING)
        buffer.writeNullTerminatedString(authorURL ?: "", CREATURES_CHARACTER_ENCODING)
        buffer.writeNullTerminatedString(authorComments ?: "", CREATURES_CHARACTER_ENCODING)

        // Actually write chunk
        writeChunk(outputStream, AUTH_HEADER, buffer.toByteArray())
    }

    private fun writeFiles(outputStream: ByteStreamWriter) {
        if (filesToInline.isEmpty())
            return
        for (file in filesToInline) {
            val bytes = ByteStreamWriter.writeBytes {
                val buffer = this
                val tag = when (file.extension?.lowercase()) {
                    "s16" -> 0
                    "wav" -> 1
                    else -> throw Caos2CobException("Invalid dependency declared. Valid filetypes are S16 and WAV")
                }
                buffer.writeUInt16(tag)
                buffer.writeUInt32(0)
                val fileBytes = file.contentsToByteArray()
                buffer.writeUInt32(fileBytes.size)
                buffer.writeNullTerminatedString(file.name)
                buffer.write(fileBytes)
            }
            // Actually write chunk
            writeChunk(outputStream, FILE_HEADER, bytes)
        }
    }

    private fun writeChunk(outputStream: ByteStreamWriter, blockType: ByteArray, data: ByteArray) {
        outputStream.write(blockType)
        outputStream.writeUInt32(data.size)
        outputStream.write(data)
    }

    private val hasAuthProperties: Boolean by lazy {
        creationTime != null ||
                version != null ||
                revision != null ||
                authorName != null ||
                authorEmail != null ||
                authorURL != null ||
                authorComments != null
    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

        private val COB2_HEADER = "cob2".toByteArray(CREATURES_CHARACTER_ENCODING)
        private val AGNT_HEADER = "agnt".toByteArray(CREATURES_CHARACTER_ENCODING)
        private val FILE_HEADER = "file".toByteArray(CREATURES_CHARACTER_ENCODING)
        private val AUTH_HEADER = "auth".toByteArray(CREATURES_CHARACTER_ENCODING)
    }
}