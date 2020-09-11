package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBlock.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScriptType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.flip
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.util.io.toByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.InflaterOutputStream

object CobDecompiler {

    public fun decompile(buffer: ByteBuffer): List<CobBlock> {
        val header = buffer.cString(4)
        buffer.position(0)
        return if (header == "cob2") {
            decompileC2Cob(buffer)
        } else try {
            val decompressed = ByteArrayOutputStream()
            val inflator = InflaterOutputStream(decompressed)
            inflator.write(buffer.toByteArray())
            decompileC2Cob(ByteBuffer.wrap(decompressed.toByteArray()))
        } catch (e: Exception) {
            decompileC1Cob(buffer)
        }
    }


    private fun decompileC1Cob(buffer: ByteBuffer): List<CobBlock> {
        val version = buffer.uInt8
        if (version > 4)
            throw Exception("Invalid COB file")
        return listOf(buffer.readC1Cob())
    }

    private fun decompileC2Cob(buffer: ByteBuffer): List<CobBlock> {
        val header = buffer.cString(4)
        if (header != "cob2")
            throw Exception("Invalid C2 Cob file")
        val blocks = mutableListOf<CobBlock>()
        while(true) {
            val block = buffer.readC2CobBlock()
                    ?: break
            blocks.add(block)
        }
        return blocks
    }

}
