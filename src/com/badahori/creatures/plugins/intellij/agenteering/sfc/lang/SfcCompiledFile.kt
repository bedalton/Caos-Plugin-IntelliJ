package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
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

    private val file: PsiFile by lazy {
        val sfcDump = SfcReader.readFile(virtualFile.contentsToByteArray()).toString()
        val virtualFile = CaosVirtualFile(file.name+".txt", sfcDump, false)
        virtualFile.getPsiFile(project)!!
    }

    override fun isPhysical(): Boolean = true

    override fun getFileType(): FileType = SfcFileType

    override fun getMirror(): PsiElement {
        return file
    }

    override fun getContainingFile(): PsiFile {
        return file
    }

    override fun getDecompiledPsiFile(): PsiFile {
        return file
    }

    override fun toString(): String {
        return "SFC.$name"
    }
}