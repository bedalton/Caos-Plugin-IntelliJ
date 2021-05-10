package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.utils.flipVertical
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import java.nio.ByteBuffer
import java.util.*


internal fun ByteBuffer.readC1Cob(fileName:String?): CobBlock.AgentBlock {
    val quantityAvailable = int16

    val expiresMonth = int32
    val expiresDay = int32
    val expiresYear = int32
    val expiry = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(expiresYear, expiresMonth, expiresDay, 0, 0, 0)
    }
    val numObjectScripts = int16
    if (numObjectScripts > 200)
        throw Exception("Suspicious number of objects script. COB is expecting $numObjectScripts object scripts")
    val numInstallScripts = int16
    if (numInstallScripts > 200)
        throw Exception("Suspicious number of install script. COB is expecting $numInstallScripts object scripts")
    val quantityUsed = int32
    val objectScripts = (0 until numObjectScripts).map { index ->
        val code = readC1Script()
        AgentScript(code, "Script $index", AgentScriptType.OBJECT)
    }
    val installScripts = (0 until numInstallScripts).mapNotNull { index ->
        val suffix = if(numInstallScripts > 1) " ($index)" else ""
        AgentScript.InstallScript(readC1Script(), (fileName?.let { "$it "} ?: "") + "Install Script$suffix")
    }
    val pictureWidth = int32
    val pictureHeight = int32
    skip(2)
    val image = if (pictureWidth > 0 && pictureHeight > 0)
        SprSpriteFrame(this, position().toLong(), pictureWidth, pictureHeight).image?.flipVertical()
    else
        null
    val agentName = cString(uInt8)
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

private fun ByteBuffer.readC1Script(): String {
    val scriptSize = uInt8.let {
        if (it == 255)
            int16
        else
            it
    }
    return cString(scriptSize)
}