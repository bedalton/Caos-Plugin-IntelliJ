package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

data class Caos2CobC2(
    override val agentName: String,
    override val targetFile:String,
    val quantityAvailable: Int? = null,
    val expiresMonth: Int? = null,
    val expiresDay: Int? = null,
    val expiresYear: Int? = null,
    val objectScripts: List<String> = listOf(),
    val installScript: String?,
    val removalScript: String?,
    val quantityUsed: Int? = null,
    val pictureUrl: String? = null,
    // Author blocks
    val creationTime:String? = null,
    val version:Int? = null,
    val revision:Int? = null,
    val authorName:String? = null,
    val authorEmail:String? = null,
    val authorURL:String? = null,
    val authorComments:String? = null,
    val depends:Set<String>,
    val inline:List<VirtualFile>
) : Caos2Cob {

    constructor(
        cobData: Map<CobTag, String?>,
        installScript: String?,
        objectScripts: List<String>,
        removalScript: String?,
        depends:Set<String>,
        inline:List<VirtualFile>,
        yearMonthDay:List<Int?> = cobData[CobTag.EXPIRY]?.split("-")?.map { it.toIntSafe() } .orEmpty()
    ) : this (
        agentName = cobData[CobTag.AGENT_NAME] ?: throw Caos2CobException("Cannot make C2 COB without 'Agent Name' tag"),
        targetFile = cobData[CobTag.COB_NAME] ?: throw Caos2CobException("Cannot make C2 COB without 'COB Name' tag"),
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
        depends = depends,
        inline = inline
    )


    private val thumbnail: BufferedImage? by lazy {
        val image = try {
            pictureUrl?.let { pictureUrl ->
                VirtualFileManager.getInstance().findFileByUrl(pictureUrl)?.let { virtualFile ->
                    VfsUtil.virtualToIoFile(virtualFile).let { file ->
                        ImageIO.read(file)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.severe(e.message)
            e.printStackTrace()
            return@lazy null
        }
        if (pictureUrl != null && image == null) {
            val dialog = DialogBuilder()
            dialog.title("Image Error")
            dialog.setErrorText("Failed to locate image for COB. Image may have moved")
            dialog.showAndGet()
        }
        image
    }

    override fun compile(): ByteArray {
        TODO("C2 COB compiling Not yet implemented")
    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("YYYY-MM-dd")
    }
}