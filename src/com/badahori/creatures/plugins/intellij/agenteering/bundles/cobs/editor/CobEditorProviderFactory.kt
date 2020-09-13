package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager

class CobEditorProviderFactory() : FileViewProviderFactory {
    override fun createFileViewProvider(
            virtualFile: VirtualFile,
            language: Language?,
            manager: PsiManager,
            eventSystemEnabled: Boolean): FileViewProvider {
        return CobEditorProvider(manager, virtualFile)
    }
}