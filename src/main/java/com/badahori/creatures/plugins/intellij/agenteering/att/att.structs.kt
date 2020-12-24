package com.badahori.creatures.plugins.intellij.agenteering.att

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.editor.HasImage
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage

/**
 * Holds all points in the line
 */
data class AttFileLine( val points: List<Pair<Int,Int>>)


/**
 * Holds data about the points in the ATT file by line
 */
data class AttFileData(val lines:List<AttFileLine>) {
    /**
     * Formats the ATT data object to proper ATT file text
     */
    fun toFileText(variant:CaosVariant) : String {
        val pointToString = if (variant.isOld) pointToStringC1e else pointToStringC2e
        return lines.joinToString("\n") { line ->
            line.points.joinToString(" ", transform = pointToString) + " "
        } + "\n"
    }
}

/**
 * Converts a point in the ATT file data object to a ATT file formatted string for Creatures 1/2
 */
private val pointToStringC1e = {point:Pair<Int,Int> ->
    "${point.first}".padStart(2, ' ') + " " + "${point.second}".padStart(2, ' ')
}

/**
 * Converts a point in the ATT file data object to a ATT file formatted string for Creatures 1/2
 */
private val pointToStringC2e = {point:Pair<Int,Int> ->
    "${point.first} ${point.second}"
}


/**
 * Parser for ATT files -> Data objects
 */
object AttFileParser {
    /**
     * Parses an ATT file from raw text to a data object, ensuring that there are the expected number of lines and points
     */
    fun parse(text:String, expectedLines:Int, expectedPoints:Int) : AttFileData {
        val rawLines = text.split("[\n\r]+".toRegex())
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
    /**
     * Parses an ATT file from virtual file and project, to a data object ensuring that there are the expected number of lines and points
     */
    fun parse(project:Project, file:VirtualFile, expectedLines:Int, expectedPoints:Int) : AttFileData {
        val text = file.getPsiFile(project)?.text ?: file.contents
        val rawLines = text.split("[\n\r]+".toRegex())
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