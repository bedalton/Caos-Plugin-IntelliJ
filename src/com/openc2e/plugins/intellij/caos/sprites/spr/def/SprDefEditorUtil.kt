package com.openc2e.plugins.intellij.caos.sprites.spr.def

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprCompiler.compileForPreview
import com.openc2e.plugins.intellij.caos.sprites.spr.def.SprDefImageData.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.imageio.ImageIO

object SprDefEditorUtil {

    @JvmStatic
    fun parseTransferable(project:Project, virtualFile:VirtualFile, transferable: Transferable?, startIndex:Int): List<SprDefImageData>? {
        if (transferable == null)
            return null
        if (transferable.isDataFlavorSupported(SprImageDataFlavor)) {
            try {
                @Suppress("UNCHECKED_CAST")
                return (transferable.getTransferData(SprImageDataFlavor) as List<SprDefImageData?>).filterNotNull()
            } catch (e: UnsupportedFlavorException) {
                LOGGER.severe("Failed to parse File transfer with unsupported flavor exception: " + e.message)
                e.printStackTrace()
            } catch (e: IOException) {
                LOGGER.severe("Failed to parse File transfer data with IOException: " + e.message)
                e.printStackTrace()
            }
        } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            @Suppress("UNCHECKED_CAST")
            try {
                val module = ModuleUtil.findModuleForFile(virtualFile, project)!!.moduleFile
                val inFiles = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                return IntStream
                        .range(0, inFiles.size)
                        .mapToObj { i: Int ->
                            val insertIndex = startIndex + i
                            val ioFile = inFiles[i]
                            try {
                                val file = LocalFileSystem.getInstance().findFileByPath(ioFile.absolutePath)
                                        ?: return@mapToObj SprDefMissingImage(insertIndex, ioFile.absolutePath, null)
                                var relativePath: String?
                                relativePath = if (module != null) {
                                    VfsUtilCore.findRelativePath(file, module, File.separatorChar)
                                } else {
                                    "::" + VfsUtilCore.findRelativePath(file, virtualFile, File.separatorChar)
                                }
                                if (relativePath == null) {
                                    relativePath = ioFile.absolutePath
                                }
                                val inputStream: InputStream = ByteArrayInputStream(file.contentsToByteArray())
                                val image = compileForPreview(ImageIO.read(inputStream))
                                return@mapToObj SprDefImage(insertIndex, relativePath!!, image, file)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val error = "Exception encountered reading image '\$index' at path '\$relativePath'. Error: \${e.message}"
                                return@mapToObj SprDefImageError(insertIndex, ioFile.absolutePath, error, null)
                            }
                        }.collect(Collectors.toList())
            } catch (e: UnsupportedFlavorException) {
                LOGGER.severe("Failed to parse File transfer with unsupported flavor exception: " + e.message)
                e.printStackTrace()
            } catch (e: IOException) {
                LOGGER.severe("Failed to parse File transfer data with IOException: " + e.message)
                e.printStackTrace()
            }
        }
        return null
    }

}