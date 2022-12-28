package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import bedalton.creatures.common.bytes.*
import bedalton.creatures.sprite.util.ColorPalette
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.soywiz.korim.awt.toAwt
import java.util.*

const val COB_LOG_KEY = "COB_LOG_VERBOSE"

internal suspend fun ByteStreamReader.readC1Cob(fileName: String?): CobBlock.AgentBlock {
    val quantityAvailable = uInt16()

    val expiresMonth = int32()
    val expiresDay = int32()
    val expiresYear = int32()
    val expiry = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(expiresYear, expiresMonth, expiresDay, 0, 0, 0)
    }
    val numObjectScripts = int16()
    if (numObjectScripts > 200) {
        throw Exception("Suspicious number of objects script. COB is expecting $numObjectScripts object scripts")
    }
    val numInstallScripts = int16()
    if (numInstallScripts > 200) {
        throw Exception("Suspicious number of install script. COB is expecting $numInstallScripts object scripts")
    }
    val quantityUsed = int32()
    val objectScripts = (0 until numObjectScripts).map { index ->
        val script = sfcString()
        AgentScript(script, "Script $index", AgentScriptType.OBJECT)
    }
    val installScripts = (0 until numInstallScripts).mapNotNull { index ->
        val suffix = if (numInstallScripts > 1) " ($index)" else ""
        val script = sfcString()
        AgentScript.InstallScript(script, (fileName?.let { "$it " } ?: "") + "Install Script$suffix")
    }
    val pictureWidth = int32()
    val pictureHeight = int32()
    if (pictureWidth > 1000 || pictureHeight > 1000) {
        throw Exception("Invalid picture dimensions. Width: $pictureWidth; Height: $pictureHeight")
    }
    val actualWidth = int16()
    if (pictureWidth < actualWidth) {
        throw Exception("Actual width is greater than buffered width; BufferedWidth: $pictureWidth; ActualWidth: $actualWidth")
    }

    val image = if (pictureWidth > 0 && pictureHeight > 0) {
        bedalton.creatures.sprite.parsers.readSprFrame(
            this,
            position().toLong(),
            pictureWidth,
            pictureHeight,
            ColorPalette.C1TransparentBlack
        )
            .flipY()
            .toAwt()
    } else {
        null
    }
    val agentName = sfcString()

    return CobBlock.AgentBlock(
        format = CobFormat.C1,
        name = agentName,
        description = "",
        image = image,
        quantityAvailable = quantityAvailable,
        quantityUsed = quantityUsed,
        expiry = expiry,
        lastUsageDate = null,
        useInterval = null,
        eventScripts = objectScripts,
        installScripts = installScripts,
        removalScript = null,
        dependencies = emptyList()
    )
}