package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.runInspections
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.psi.impl.PsiFileEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import java.nio.ByteBuffer


class CobCompiledFile(provider:FileViewProvider) : PsiBinaryFileImpl(provider.manager as PsiManagerImpl, provider), PsiCompiledFile, PsiFileEx {

    private val cobData by lazy {
        CobDecompiler.decompile(ByteBuffer.wrap(virtualFile.contentsToByteArray()))
    }

    override fun getLanguage(): Language {
        return CaosScriptLanguage.instance
    }

    override fun getFileType(): FileType = CobFileType.INSTANCE

    override fun getMirror(): PsiElement {
        return decompiledPsiFile
    }

    override fun getText(): String {
        return CobBinaryDecompiler.presentCobData(virtualFile.name, cobData)
    }


    override fun getDecompiledPsiFile(): PsiFile {
        val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("D_"+virtualFile.name, CaosScriptLanguage.instance, text) as CaosScriptFile
        val variant = if (cobData is CobFileData.C1CobData) CaosVariant.C1 else CaosVariant.C2
        psiFile.variant = variant
        psiFile.runInspections = false
        return psiFile
    }

}