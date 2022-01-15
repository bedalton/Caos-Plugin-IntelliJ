package com.badahori.creatures.plugins.intellij.agenteering.att

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Holds all points in the line
 */
data class AttFileLine( val points: List<Pair<Int,Int>>) {
    operator fun get(index:Int) : Pair<Int, Int> {
        return points[index]
    }

    fun getOrNull(index:Int): Pair<Int, Int>? {
        return points.getOrNull(index)
    }
}


/**
 * Holds data about the points in the ATT file by line
 */
data class AttFileData(val lines:List<AttFileLine>) {

    /**
     * Gets a specific line in an ATT file
     */
    operator fun get(index:Int) : AttFileLine {
        return lines[index]
    }

    fun getOrNull(index:Int): AttFileLine? {
        return lines.getOrNull(index)
    }

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
 * Formats the ATT data object to proper ATT file text
 */
internal fun List<AttFileLine>.toFileText(variant:CaosVariant) : String {
    val pointToString = if (variant.isOld) pointToStringC1e else pointToStringC2e
    return this.joinToString("\n") { line ->
        line.points.joinToString(" ", transform = pointToString) + " "
    } + "\n"
}

/**
 * Converts a point in the ATT file data object to an ATT file formatted string for Creatures 1/2
 */
private val pointToStringC1e = {point:Pair<Int,Int> ->
    "${point.first}".padStart(2, ' ') + " " + "${point.second}".padStart(2, ' ')
}

/**
 * Converts a point in the ATT file data object to an ATT file formatted string for Creatures 1/2
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
    @JvmStatic
    fun parse(text:String, expectedLines:Int? = null, expectedPoints:Int? = null) : AttFileData {
        // Break ATT into lines
        val rawLines = text
            .trimEnd()
            .split("[\n\r]+".toRegex())
            .filter { it.isNotBlank() }

        // Convert lines into points list
        val lines = (0 until (expectedLines ?: rawLines.size)).map { lineNumber ->
            val line = rawLines.getOrNull(lineNumber) ?: ""
            val intsRaw = line.trim().split(WHITESPACE)
            val points = (0 until (expectedPoints ?: intsRaw.size)).map { pointIndex ->
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
    @JvmStatic
    fun parse(project:Project, file:VirtualFile, expectedLines:Int? = null, expectedPoints:Int? = null) : AttFileData {
        val text = file.getPsiFile(project)?.text ?: file.contents
        val rawLines = text.trimEnd().split("[\n\r]+".toRegex())
        val lines = (0 until (expectedLines ?: rawLines.size)).map { lineNumber ->
            val line = rawLines.getOrNull(lineNumber) ?: ""
            val intsRaw = line.trim().split(WHITESPACE)
            val points = (0 until (expectedPoints ?: intsRaw.size)).map { pointIndex ->
                val x = intsRaw.getOrNull(pointIndex * 2)?.toIntSafe() ?: 0
                val y = intsRaw.getOrNull(pointIndex * 2 + 1)?.toIntSafe() ?: 0
                Pair(x, y)
            }
            AttFileLine(points = points)
        }
        return AttFileData(lines = lines)
    }
}