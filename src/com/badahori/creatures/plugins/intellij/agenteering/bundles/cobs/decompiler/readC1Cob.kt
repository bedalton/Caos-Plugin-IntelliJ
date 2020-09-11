package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.flip
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.utils.cString
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt8
import java.nio.ByteBuffer
import java.util.*


internal fun ByteBuffer.readC1Cob(): CobBlock.AgentBlock {
    val quantityAvailable = uInt8
    val expiresMonth = uInt16
    val expiresDay = uInt16
    val expiresYear = uInt16
    val expiry = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(expiresYear, expiresMonth, expiresDay, 0, 0, 0)
    }
    val numObjectScripts = uInt8
    val numInstallScripts = uInt8
    val quantityUsed = uInt16
    val objectScripts = (0 until numObjectScripts).map { index ->
        val code = readC1Script()
        AgentScript(code, "Script $index", AgentScriptType.OBJECT)
    }
    val installScriptString = (0 until numInstallScripts).joinToString("* INSTALL SCRIPT PART") { index ->
        readC1Script()
    }
    val installScript = AgentScript.InstallScript(installScriptString)
    val pictureWidth = uInt16
    val pictureHeight = uInt16
    uInt8
    val image = SprSpriteFrame(this, position().toLong(), pictureWidth, pictureHeight).image?.flip()
    val agentName = cString
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
            installScript = installScript,
            removalScript = null,
            dependencies = emptyList()
    )
}

private fun ByteBuffer.readC1Script() : String {
    val scriptSize = get().toInt().let {
        if (it == 255)
            uInt16
        else
            it
    }
    return cString(scriptSize)
}