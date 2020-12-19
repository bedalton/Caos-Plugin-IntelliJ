package com.badahori.creatures.plugins.intellij.agenteering.att

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.editor.HasImage
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage

data class AttPoint(val x:Int, val y: Int)

data class ViewPoints(val points:List<AttPoint>)

data class AttJoint(val part:Char, val index:Int, val unused:Boolean = false)

data class AttPart(val part:Char, val parent:AttJoint?, val children:List<AttJoint> = emptyList())

data class Att(val part:Char, val views:List<ViewPoints>)

data class AttFileLine( val points: List<Pair<Int,Int>>)

data class AttFileData(val lines:List<AttFileLine>) {
    fun toFileText() : String {
        return lines.joinToString("\n") { line ->
            line.points.joinToString(" ") { "${it.first} ${it.second}" } + " "
        } + "\n"
    }
}

object AttFileParser {
    fun parse(file:VirtualFile, expectedLines:Int, expectedPoints:Int) : AttFileData {
        val rawLines = file.contents.split("[\n\r]+".toRegex())
        val lines = (0 until expectedLines).map { lineNumber ->
            val line = rawLines.getOrNull(lineNumber) ?: ""
            val intsRaw = line.trim().split(WHITESPACE)
            val points = (0 until expectedPoints).map { pointIndex ->
                val x = intsRaw.getOrNull(pointIndex * 2)?.toIntSafe() ?: 0
                val y = intsRaw.getOrNull(pointIndex * 2 + 1)?.toIntSafe() ?: 0
                Pair(x, y)
            }
            AttFileLine(points = points)
        }
        return AttFileData(lines = lines)
    }
}