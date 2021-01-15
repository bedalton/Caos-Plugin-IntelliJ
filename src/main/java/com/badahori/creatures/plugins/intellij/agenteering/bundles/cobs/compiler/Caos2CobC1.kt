package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.flipVertical
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprCompiler
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

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
    val removerName:String? = null,
) : Caos2Cob {

    constructor(cobData:Map<CobTag, String?>, objectScripts: List<String>, installScripts: List<String>, removalScript:String?, yearMonthDay:List<Int?> = cobData[CobTag.EXPIRY]?.split("-")?.map { it.toIntSafe() } .orEmpty()) : this(
        agentName = cobData[CobTag.AGENT_NAME] ?: throw Caos2CobException("Cannot create C1 cob without agent name"),
        targetFile = cobData[CobTag.COB_NAME] ?: throw Caos2CobException("Cannot create C1 COB without COB file name"),
        quantityAvailable = cobData[CobTag.QUANTITY_AVAILABLE]?.toIntSafe() ?: 255,
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

    private val thumbnail: BufferedImage? by lazy {
        val image:BufferedImage? = try {
            pictureUrl?.trim()?.let { pictureUrl ->
                LocalFileSystem.getInstance().findFileByPath(pictureUrl)?.let { virtualFile ->
                    VfsUtil.virtualToIoFile(virtualFile).let { file ->
                        ImageIO.read(file)
                            ?: throw Caos2CobException("Failed to read image with ImageIO.read(file)")
                    }
                } ?: throw Caos2CobException("Failed to located virtual file for image: '$pictureUrl'")
            }
        } catch (e: Exception) {
            LOGGER.severe(e.message)
            e.printStackTrace()
            null
        }
        if (pictureUrl != null && image == null) {
            throw Caos2CobException("Could not resolve COB thumbnail image at path: '$pictureUrl'")
        }
        image
    }

    override fun compile(): ByteArray {
        val buffer = ByteArrayOutputStream()
        buffer.writeUInt16(1) // Cob Version
        buffer.writeUInt16(quantityAvailable ?: 255)
        buffer.writeUint32(expiresMonth)
        buffer.writeUint32(expiresDay)
        buffer.writeUint32(expiresYear)
        buffer.writeUInt16(objectScripts.size)
        buffer.writeUInt16(installScripts.size)
        buffer.writeUint32(quantityUsed ?: 0)
        objectScripts.forEach { script ->
            buffer.writeSfcString(script)
        }
        installScripts.forEach { script ->
            buffer.writeSfcString(script)
        }
        val image = thumbnail
        buffer.writeUint32(image?.width ?: 0)
        buffer.writeUint32(image?.height ?: 0)
        buffer.writeUInt16(image?.width ?: 0)
        if (image != null) {
            LOGGER.info("Image is not null")
            image.flipVertical().let { thumbnail ->
                SprCompiler.compileSprite(thumbnail, buffer, false)
            } ?: throw Caos2CobException("Image is null after flip")
        }
        buffer.writeSfcString(agentName)
        return buffer.toByteArray()
    }

    val removerCob : Caos2CobC1? by lazy {
        val removalScript = removalScript
            ?: return@lazy null
        Caos2CobC1(
            agentName = "$agentName Remover",
            targetFile = removerName ?: FileNameUtils.getBaseName(targetFile) + ".rcb",
            installScripts = listOf(removalScript),
            quantityUsed = 0,
            quantityAvailable = 255
        )
    }
}