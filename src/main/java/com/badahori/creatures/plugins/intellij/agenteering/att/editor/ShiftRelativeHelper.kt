package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.intellij.openapi.vfs.VirtualFile


internal object ShiftRelativeHelper {

    fun shiftRelativePointInFile(file: VirtualFile, point: Int, deltaX: Int, deltaY: Int): Boolean {
        val key = BreedPartKey.fromFileName(file.nameWithoutExtension)
        val part = key?.part
            ?: return false
        val relatedPoint = getRelatedPoint(part, point)
        val relatedFiles = getRelatedParts()
        TODO()
    }

    private fun getRelatedPoint(part: Char, point: Int): Int {
        TODO()
    }

    private fun getRelatedPointInHead(point: Int): Int {
        TODO()
    }

    private fun getRelatedPointInBody(point: Int): Int {
        TODO()
    }

    private fun getRelatedFiles(): List<VirtualFile> {
        val relatedParts = getRelatedParts()
        TODO()
    }

    private fun getRelatedParts(): Int {
        TODO()
    }

}