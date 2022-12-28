package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

val CAOS2Cob by lazy { AgentMessages.message("cob.caos2cob.group") }
val CAOS2Pray by lazy { AgentMessages.message("pray.caos2pray.group") }
val PRAY by lazy { AgentMessages.message("pray.group") }
val CAOSScript by lazy { CaosBundle.message("caos.intentions.family") }
val CAOS2Path by lazy { arrayOf(CAOSScript) }


internal val PsiElement.psiDirectory: PsiDirectory?
    get() {
        return containingFile.parent
            ?: containingFile?.originalFile?.parent
            ?: originalElement?.containingFile?.parent
            ?: originalElement?.containingFile?.originalFile?.parent
    }

internal val PsiElement.directory: VirtualFile?
    get() {
        return containingFile.virtualFile?.parent
            ?: containingFile.originalFile.virtualFile?.parent
            ?: psiDirectory?.virtualFile
    }


internal fun getFileNameWithArrayAccess(path: String): String? {
    if (path.isEmpty()) {
        return null
    }
    if (!path.contains('[')) {
        return path
    }
    Caos2CobUtil.ARRAY_ACCESS_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
        return "${groupValues[1]}.${groupValues[2]}"
    }
    Caos2CobUtil.ARRAY_ACCESS_BEFORE_EXTENSION_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
        return "${groupValues[1]}.${groupValues[3]}"
    }
    return null
}