@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16

import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl

class S16File(viewProvider: FileViewProvider) : PsiBinaryFileImpl(viewProvider.manager as PsiManagerImpl, viewProvider) {

    override fun getFileType(): FileType = S16FileType

    override fun canNavigate(): Boolean = true

    override fun isPhysical(): Boolean = true



}