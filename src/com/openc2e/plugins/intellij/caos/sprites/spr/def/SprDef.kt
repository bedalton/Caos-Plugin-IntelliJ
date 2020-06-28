package com.openc2e.plugins.intellij.caos.sprites.spr.def

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprCompileResult.SprCompileResultBad
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprCompileResult.SprCompileResultOK
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprDefImageData.SprDefImage
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprDefImageData.SprDefMissingImage
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprDefParseResult.SprDefParseData
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprDefParseResult.SprDefParseFail
import com.openc2e.plugins.intellij.caos.utils.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val newLineRegex = "\r\n|\n|\r".toRegex()
object SprDef {
    internal const val REL_PREFIX = "::"
    private const val TARGET_PATH_SEPARATOR = "::"

    @JvmStatic
    fun write(project:Project, file:VirtualFile, lastSavePath: String?, spriteName:String, filesIn:List<VirtualFile>) : Boolean {
        val module = ModuleUtil.findModuleForFile(file, project)?.moduleFile
        val files = if (module != null) {
            filesIn.map {
                VfsUtilCore.findRelativePath(module, it, File.separator.first());
            }
        } else {
            filesIn.map {
                "-r ${VfsUtilCore.findRelativePath(file, it, File.separator.first())}"
            }
        }.joinToString("\n");
        val data = spriteName + (lastSavePath?.let { "${TARGET_PATH_SEPARATOR}$lastSavePath" } ?: "") +
                "\n" + files
        val ioFile = VfsUtilCore.virtualToIoFile(file)
        return runWriteAction {
            try {
                ioFile.writeText(data)
                true
            } catch(e:Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    @JvmStatic
    fun compile(project:Project, sprDefFile:VirtualFile, targetPathIn:String?) : SprCompileResult {
        val data = readSprData(project, sprDefFile)
        if (data is SprDefParseFail) {
            return SprCompileResultBad(sprDefFile, data.error)
        }
        val result = (data as? SprDefParseData)
                ?:  throw Exception("Unexpected SPR source image load result. Type Is: ${data.javaClass.canonicalName}")
        val invalids = result.images.filterNot { it is SprDefImage}
        if (invalids.isNotEmpty()) {
            val missingMessage = invalids.filterIsInstance<SprDefMissingImage>().ifEmpty { null }?.let {
                " Missing ${it.size} images."
            } ?: ""
            val invalidMessage = invalids.filterIsInstance<SprDefImageData.SprDefImageError>().ifEmpty { null }?.let {
                if (missingMessage.isEmpty()) " Encountered ${it.size} image load errors." else " and encountered ${it.size} image load errors."
            }
            val errorMessage = "Cannot compile sprite.$missingMessage$invalidMessage"
            return SprCompileResultBad(sprDefFile, errorMessage)
        }
        val fileName = data.fileName
        val spriteName = data.spriteName
        val images = data.images

        val bytes = SprCompiler.compileSprites(images.map { (it as SprDefImage).bufferedImage })
        val targetPath = if (targetPathIn == null) {
            val lastPath = (result.lastSavePath ?: File(sprDefFile.path).parent).let {
                if (it.endsWith(File.separator))
                    it
                else
                    "$it${File.separator}"
            }
            "$lastPath$spriteName.spr"
        } else {
            targetPathIn
        }
        return runWriteAction write@{
            val ioFile = File(targetPath)
            if (!FileUtil.createIfDoesntExist(ioFile)) {
                return@write SprCompileResultBad(sprDefFile, "Failed to create file for writing at path '$targetPath'")
            }
            try {
                ioFile.writeBytes(bytes)
            } catch (e:Exception) {
                e.printStackTrace()
                return@write SprCompileResultBad(sprDefFile, "Failed to write data to file: '$targetPath'. With Error: ${e.message}")
            }
            SprCompileResultOK(sprDefFile, ioFile)
        }
    }

    @JvmStatic
    fun readSprData(project:Project, sprDefFile:VirtualFile) : SprDefParseResult {
        val fileName = sprDefFile.name
        val lines = sprDefFile.contents.split(newLineRegex)
        val pathInfo = lines.first().split("::")
        val spriteName = pathInfo.first().nullIfEmpty() ?: fileName.substringFromEnd(0, 4)
        val images = (1 .. lines.lastIndex).map { index ->
            val relativePath = lines[index]
            val imageFile = (if (relativePath.startsWith(REL_PREFIX))
                findFileByRelativePath(project, sprDefFile, relativePath.substring(REL_PREFIX.length))
            else
                findFileBySharedModuleAndRelativePath(project, sprDefFile, relativePath))
            if (imageFile == null || !imageFile.exists())
                return@map SprDefMissingImage(index, relativePath, imageFile)
            try {
                val image = SprCompiler.compileForPreview(ImageIO.read(imageFile.contentsToByteArray().inputStream()))
                SprDefImage(index, relativePath, image, imageFile)
            } catch(e:Exception) {
                e.printStackTrace()
                val error = "Exception encountered reading image '$index' at path '$relativePath'. Error: ${e.message}"
                LOGGER.severe(error)
                SprDefImageData.SprDefImageError(index, relativePath, error, imageFile)
            }
        }
        return SprDefParseData(fileName = fileName, lastSavePath = pathInfo.lastOrNull(),spriteName = spriteName, images =  images)
    }

}

sealed class SprCompileResult {
    data class SprCompileResultOK(val file:VirtualFile, val sprFile:File) : SprCompileResult()
    data class SprCompileResultBad(val file:VirtualFile, val error:String) : SprCompileResult()
}

sealed class SprDefParseResult {
    data class SprDefParseData(val fileName:String, val lastSavePath:String?, val spriteName: String?, val images:List<SprDefImageData>) : SprDefParseResult()
    data class SprDefParseFail(val fileName:String, val error:String, val spriteName:String? = null) : SprDefParseResult()
}

sealed class SprDefImageData(val index:Int, val relativePath:String, val virtualFile: VirtualFile?) {
    class SprDefImageError(index:Int, relativePath:String, val error:String, virtualFile: VirtualFile) : SprDefImageData(index, relativePath, virtualFile)
    class SprDefMissingImage(index:Int, relativePath:String, virtualFile: VirtualFile?) : SprDefImageData(index, relativePath, virtualFile)
    class SprDefImage(index:Int, relativePath:String, val bufferedImage: BufferedImage, virtualFile: VirtualFile) : SprDefImageData(index, relativePath, virtualFile)
}