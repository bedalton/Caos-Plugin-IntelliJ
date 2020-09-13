package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.editor

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobBinaryDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang.CobCompiledFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.lang.CobFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.testFramework.LightVirtualFile
import java.nio.ByteBuffer


class CobEditorProvider(manager: PsiManager,private val virtualFileIn:VirtualFile) : SingleRootFileViewProvider(manager, virtualFileIn, true, CaosScriptLanguage), DumbAware {

    internal val variant by lazy {
        LOGGER.info("Getting variant in provider")
        virtualFileIn.getUserData(CaosScriptFile.VariantUserDataKey)?.let {
            return@lazy it
        }
        CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(virtualFileIn.contentsToByteArray()).littleEndian()).variant
    }

    override fun supportsIncrementalReparse(rootLanguage: Language): Boolean = false

    internal val caos:String by lazy {
        LOGGER.info("Getting caos code in COB file provider")
        if (virtualFileIn.contents.startsWith("****"))
            virtualFileIn.contents
        else
            CobBinaryDecompiler.decompileToString(virtualFileIn.name, virtualFileIn.contentsToByteArray())
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        if (fileType != CobFileType) {
            LOGGER.severe("Create file in CobEditorProvider should be of type COBFile. Found: ${fileType.name}")
            return super.createFile(project, file, fileType)
        }
        LOGGER.info("Creating compiled file")
        return CobCompiledFile(this)
    }

    override fun createCopy(copy: VirtualFile): SingleRootFileViewProvider {
        if (copy.fileType != CobFileType)
            return super.createCopy(copy)
        return CobEditorProvider(manager, copy)
    }

    override fun getContents(): CharSequence {
        return caos
    }

    override fun getBaseLanguage(): Language = CaosScriptLanguage

    override fun getVirtualFile(): VirtualFile = virtualFileIn

    override fun findElementAt(offset: Int): PsiElement? {
        return this.findElementAt(offset, CaosScriptLanguage)
    }

    override fun findElementAt(offset: Int, language: Language): PsiElement? {
        var file = getPsi(language)
        if (file is PsiCompiledFile) {
            file = file.decompiledPsiFile
        }
        return findElementAt(file, offset)
    }

    override fun findReferenceAt(offset: Int): PsiReference? {
        return this.findReferenceAt(offset, CaosScriptLanguage)
    }

    override fun findReferenceAt(offset: Int, language: Language): PsiReference? {
        var file = getPsi(language)
        if (file is PsiCompiledFile) {
            file = file.decompiledPsiFile
        }
        return findReferenceAt(file, offset)
    }
}