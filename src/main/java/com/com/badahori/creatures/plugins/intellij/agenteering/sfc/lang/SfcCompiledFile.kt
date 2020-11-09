package com.badahori.creatures.plugins.intellij.agenteering.sfc.lang

import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
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
        val sfcDump = decompileToJson()

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

    private fun decompileToJson(): String {
        // Check if file was already decompiled, and json data written.
        // If it was, return it
        virtualFile.getUserData(SFC_JSON_KEY)?.let {
            return it
        }
        // Deserialize virtual data if present.
        val holder = virtualFile.getUserData(SFC_DECOMPILED_DATA_KEY)
                ?: try { SfcDecompiledFilePropertyPusher.readFromStorage(virtualFile) } catch(e:Exception) { null }
        // Check if file was decompiled before, and if it was
        // return the json result
        holder?.let {
            return it.data?.toString()
                    ?: generateErrorJson(virtualFile, "SFC Decompile failed")
        }

        // Read and create json response object
        val json: String = try {
            SfcReader.readFile(virtualFile).let {
                it.data?.toString()
                        ?: generateErrorJson(virtualFile, "SFC decompile failed ${it.error ?: "without error message."}")
            }
        } catch (e: Exception) {
            generateErrorJson(virtualFile, "SFC Decompile failed. ${e.message}")
        }
        // Write the json response.
        virtualFile.putUserData(SFC_JSON_KEY, json)
        // finally, return resutl
        return json
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

private val SFC_JSON_KEY = Key<String>("caos.sfc.decompiled.JSON")

fun generateErrorJson(virtualFile: VirtualFile, errorMessageIn: String?, status: String = "DECOMPILE_FAILED"): String {
    val json = JsonObject()
    json.addProperty("file", virtualFile.path)
    json.addProperty("status", status)
    json.addProperty("error", errorMessageIn ?: DECOMPILE_FAILED_DEFAULT_ERROR_MESSAGE)
    return json.asString
}

private const val DECOMPILE_FAILED_DEFAULT_ERROR_MESSAGE = "Only Eden.sfc files can be decoded at this time"