package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.sfc.decompileSFCToJson
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.PsiFileEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl

class SfcCompiledFile(provider: FileViewProvider)
    : PsiBinaryFileImpl(provider.manager as PsiManagerImpl, provider), PsiCompiledFile, PsiFileEx, PsiBinaryFile {
    /**
     * Gets children from the in memory json psi file
     */
    override fun getChildren(): Array<out PsiElement> {
        return file.children
    }

    /**
     * Create a json PSI file from the decompiled SFC data
     */
    private val file: PsiFile by lazy {
        // Get JSON result
        val sfcDump = decompileSFCToJson(virtualFile)

        // Get the plugin temp directory.
        val tmp = CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory("tmp")
        // Create a temporary virtual file
        val virtualFile = tmp.createChildWithContent(virtualFile.name + ".json", sfcDump)
        // Reformat and return the JSON psi file
        virtualFile.getPsiFile(project)!!.apply {
            // If this is run on the Event thread,
            // Reformat it
            if (ApplicationManager.getApplication().isDispatchThread) {
                runWriteAction {
                    CodeStyleManager.getInstance(project).reformat(this)
                }
            }
        }
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
