package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import bedalton.creatures.bytes.MemoryByteStreamWriter
import bedalton.creatures.bytes.writeNullByte
import bedalton.creatures.bytes.writeSfcString
import bedalton.creatures.util.FileNameUtil
import bedalton.creatures.util.ensureEndsWith
import bedalton.creatures.util.nullIfEmpty
import bedalton.creatures.util.pathSeparatorChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.soywiz.korim.bitmap.Bitmap32
import kotlinx.serialization.Serializable
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.getValue
import kotlin.lazy
import kotlin.let

@Serializable
data class Caos2CobC1(
    override val agentName: String,
    override val targetFile: String,
    val quantityAvailable: Int? = null,
    val expiresMonth: Int = 12,
    val expiresDay: Int = 31,
    val expiresYear: Int = 9999,
    val objectScripts: List<String> = listOf(),
    val installScripts: List<String> = emptyList(),
    val removalScript: String? = null,
    val quantityUsed: Int? = null,
    val pictureUrl: String? = null,
    val removerName: String? = null,
) : Caos2Cob {

    constructor(
        cobData: Map<CobTag, String?>,
        objectScripts: List<String>,
        installScripts: List<String>,
        removalScript: String?,
        yearMonthDay: List<Int?> = cobData[CobTag.EXPIRY]?.split("-")?.map { it.toIntSafe() }.orEmpty(),
    ) : this(
        agentName = cobData[CobTag.AGENT_NAME] ?: throw Caos2CobException("Cannot create C1 cob without agent name"),
        targetFile = cobData[CobTag.COB_NAME] ?: throw Caos2CobException("Cannot create C1 COB without COB file name"),
        quantityAvailable = cobData[CobTag.QUANTITY_AVAILABLE]?.toIntSafe() ?: -1,
        expiresYear = yearMonthDay.getOrNull(0) ?: 9999,
        expiresMonth = yearMonthDay.getOrNull(1) ?: 12,
        expiresDay = yearMonthDay.getOrNull(2) ?: 31,
        objectScripts = objectScripts,
        installScripts = installScripts,
        removalScript = removalScript,
        quantityUsed = cobData[CobTag.QUANTITY_USED]?.toIntSafe() ?: 0,
        pictureUrl = cobData[CobTag.THUMBNAIL],
        removerName = cobData[CobTag.REMOVER_NAME]
    )

    private val thumbnail: Bitmap32? by lazy {
        pictureUrl?.let { url ->
            loadThumbnail(url)
        }
    }

    override fun compile(): ByteArray {
        val buffer = MemoryByteStreamWriter()
        buffer.writeUInt16(1) // Cob Version
        buffer.writeUInt16(quantityAvailable ?: -1)
        buffer.writeUInt32(expiresMonth)
        buffer.writeUInt32(expiresDay)
        buffer.writeUInt32(expiresYear)
        buffer.writeUInt16(objectScripts.size)
        buffer.writeUInt16(installScripts.size)
        buffer.writeUInt32(quantityUsed ?: 0)
        objectScripts.forEach { script ->
            buffer.writeSfcString(script)
        }
        installScripts.forEach { script ->
            buffer.writeSfcString(script)
        }
        val image = thumbnail
        if (image != null && (image.width > 1000 || image.height > 1000)) {
            throw Exception("Cannot compile COB1. Thumbnail image is too large. Width: ${image.width}; Height: ${image.height}")
        }
        buffer.writeUInt32(image?.width ?: 0)
        buffer.writeUInt32(image?.height ?: 0)
        buffer.writeUInt16(image?.width ?: 0)
        image?.flipY()?.let { thumbnail ->
            bedalton.creatures.sprite.compilers.SprCompiler.writeCompiledImage(thumbnail.toBMP32IfRequired(), false, buffer)
        }
        buffer.writeSfcString(agentName)
        buffer.writeNullByte()
        return buffer.bytes
    }

    val removerCob: Caos2CobC1? by lazy {
        val removalScript = removalScript
            ?: return@lazy null
        var removerName = removerName
        if (removerName == null) {
            val directory = FileNameUtil
                .getWithoutLastPathComponent(targetFile)
                ?.nullIfEmpty()
                ?.ensureEndsWith(pathSeparatorChar)
                ?: ""
            removerName = FileNameUtil.getFileNameWithoutExtension(targetFile).nullIfEmpty()
            removerName = if (removerName == null) {
                targetFile
            } else {
                directory + removerName
            }
        }
        val extension = FileNameUtil.getExtension(removerName)
        if (extension.isNullOrBlank()) {
            removerName += ".rcb"
        }
        Caos2CobC1(
            agentName = "$agentName Remover",
            targetFile = removerName,
            installScripts = listOf(removalScript),
            quantityUsed = 0,
            quantityAvailable = 255
        )
    }
}