package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcReader
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.psi.impl.PsiFileEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl

class CobCompiledFile(provider:FileViewProvider)
    : PsiBinaryFileImpl(provider.manager as PsiManagerImpl, provider), PsiCompiledFile, PsiFileEx, PsiBinaryFile
{
    override fun getChildren(): Array<PsiElement> {
        return file.children
    }

    private val file: PsiLargeTextFile by lazy {
        val sfcDump = SfcReader.readFile(virtualFile.contentsToByteArray())
    }

    override fun isPhysical(): Boolean = true

    override fun getFileType(): FileType = SfcFileType

    override fun getMirror(): PsiElement {
        return file
    }

    override fun getContainingFile(): PsiFile {
        return file
    }

    override fun getDecompiledPsiFile(): PsiLargeTextFile {
        return file
    }

    override fun toString(): String {
        return "COB.$name"
    }
}