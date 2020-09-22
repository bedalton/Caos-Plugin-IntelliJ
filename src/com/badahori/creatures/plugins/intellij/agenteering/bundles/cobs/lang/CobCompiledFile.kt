package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.editor.CobEditorProvider
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.runInspections
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiFileEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import kotlin.test.assertEquals

class CobCompiledFile(provider:FileViewProvider)
    : PsiBinaryFileImpl(provider.manager as PsiManagerImpl, provider), PsiCompiledFile, PsiFileEx, PsiBinaryFile
{
    private val myProvider:CobEditorProvider by lazy {
        provider as CobEditorProvider
    }

    override fun getChildren(): Array<PsiElement> {
        return file.children
    }

    private val file:CaosScriptFile by lazy {
        putUserData(CaosScriptFile.VariantUserDataKey, myProvider.variant)
        val text = myProvider.caos
        val fileName = virtualFile.name
        val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("$fileName (Decompiled)", CaosScriptLanguage, text) as CaosScriptFile

        psiFile.variant = myProvider.variant
        assertEquals (myProvider.variant, psiFile.variant,"PsiFile should have had variant set to ${myProvider.variant.fullName}")
        psiFile.runInspections = false
        psiFile
    }

    override fun isPhysical(): Boolean = true

    override fun getFileType(): FileType = CobFileType

    override fun getVirtualFile(): VirtualFile = myProvider.virtualFile

    override fun getMirror(): PsiElement {
        return file
    }

    override fun getContainingFile(): PsiFile {
        return file
    }

    override fun getDecompiledPsiFile(): CaosScriptFile {
        return file
    }

    override fun toString(): String {
        return "COB.$name"
    }
}