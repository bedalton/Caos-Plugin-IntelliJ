package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.ensureEndsWith
import com.bedalton.common.util.nullIfEmpty
import com.bedalton.common.util.pathSeparatorChar
import com.bedalton.io.bytes.ByteStreamWriter
import com.bedalton.io.bytes.writeNullByte
import com.bedalton.io.bytes.writeSfcString
import com.soywiz.korim.bitmap.Bitmap32
import kotlinx.serialization.Serializable
import kotlin.ByteArray
import kotlin.Exception
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

    override suspend fun compile(): ByteArray {
        return ByteStreamWriter.writeBytes {
            writeUInt16(1) // Cob Version
            writeUInt16(quantityAvailable ?: -1)
            writeUInt32(expiresMonth)
            writeUInt32(expiresDay)
            writeUInt32(expiresYear)
            writeUInt16(objectScripts.size)
            writeUInt16(installScripts.size)
            writeUInt32(quantityUsed ?: 0)
            objectScripts.forEach { script ->
                writeSfcString(script)
            }
            installScripts.forEach { script ->
                writeSfcString(script)
            }
            val image = thumbnail
            if (image != null && (image.width > 1000 || image.height > 1000)) {
                throw Exception("Cannot compile COB1. Thumbnail image is too large. Width: ${image.width}; Height: ${image.height}")
            }
            writeUInt32(image?.width ?: 0)
            writeUInt32(image?.height ?: 0)
            writeUInt16(image?.width ?: 0)
            image?.flipY()?.let { thumbnail ->
                bedalton.creatures.sprite.compilers.SprCompiler.writeCompiledImage(
                    thumbnail.toBMP32IfRequired(),
                    false,
                    this
                )
            }
            writeSfcString(agentName)
            writeNullByte()
        }
    }

    val removerCob: Caos2CobC1? by lazy {
        val removalScript = removalScript
            ?: return@lazy null
        var removerName = removerName
        if (removerName == null) {
            val directory = PathUtil
                .getWithoutLastPathComponent(targetFile)
                ?.nullIfEmpty()
                ?.ensureEndsWith(pathSeparatorChar)
                ?: ""
            removerName = PathUtil.getFileNameWithoutExtension(targetFile).nullIfEmpty()
            removerName = if (removerName == null) {
                targetFile
            } else {
                directory + removerName
            }
        }
        val extension = PathUtil.getExtension(removerName)
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