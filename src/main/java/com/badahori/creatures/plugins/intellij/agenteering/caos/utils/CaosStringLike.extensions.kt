package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.bedalton.common.util.PathUtil
import com.bedalton.vfs.unescapePath


internal fun CaosScriptStringLike.stringTextToAbsolutePath(): String? {
    val relativePath = name
        ?.unescapePath()
        .nullIfEmpty()
        ?: return null
    val myParentFolder = virtualFile
        ?.parent
        ?.path
        .nullIfEmpty()
        ?: return null
    return PathUtil.combine(myParentFolder, relativePath)
}