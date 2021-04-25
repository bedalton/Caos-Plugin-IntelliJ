package com.badahori.creatures.plugins.intellij.agenteering.att.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


internal  fun getInitialVariant(project: Project?, file: VirtualFile): CaosVariant {
    val breed = file.name.substring(3, 4)
    val part = file.name.substring(0, 1)
    if (part likeAny listOf("O","P", "Q"))
        return CaosVariant.CV
    return file.cachedVariant ?: if (breed.toIntSafe()?.let { it in 0..7 }.orFalse()) {
        CaosVariant.C1
    } else if (project != null) {
        file.getModule(project)?.variant
            ?: (file.getPsiFile(project) as? AttFile)?.variant
    } else {
        null
    } ?: getVariantByAttLengths(file, part)
}

private fun getVariantByAttLengths(file: VirtualFile, part: String): CaosVariant {
    val contents = file.contents.trim()
    val lines = contents.split("[\r\n]+".toRegex())
    if (lines.size == 16) {
        if (part like "a") {
            val longestLine = lines.maxBy { it.length }
                ?: return CaosVariant.C3
            val points = longestLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (points.lastOrNull()?.toIntSafe().orElse(0) > 0)
                return CaosVariant.CV
            return CaosVariant.C3
        }
        return CaosVariant.C3
    }
    return CaosVariant.C2
}
