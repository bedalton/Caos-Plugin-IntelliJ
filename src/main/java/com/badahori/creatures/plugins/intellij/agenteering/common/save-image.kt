package com.badahori.creatures.plugins.intellij.agenteering.common

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.bedalton.common.exceptions.rethrowCancellationException
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.swing.JLabel


internal fun saveImageWithDialog(
    project: Project,
    kind: String,
    startDirectoryString: String?,
    startFileName: String?,
    image: BufferedImage,
): Boolean {

    val descriptor = FileSaverDescriptor(
        "Save $kind as PNG",
        "",
        "png"
    )
    val startDirectory = startDirectoryString?.let {
        try {
            VfsUtil.findFileByIoFile(File(startDirectoryString), false)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            null
        }
    }
    val saver = FileChooserFactory
        .getInstance()
        .createSaveFileDialog(
            descriptor,
            project
        )
        .save(startDirectory, startFileName)
        ?: return false
    return try {
        runWriteAction {
            doSave(kind, saver.file, image)
        }
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        false
    }
}

private fun doSave(kind: String, outputFile: File, image: BufferedImage): Boolean {
    if (!outputFile.exists()) {
        var didCreate = false
        try {
            didCreate = outputFile.createNewFile()
        } catch (ignored: IOException) {
        }
        if (!didCreate) {
            val builder = DialogBuilder()
            builder.setTitle("$kind save error")
            builder.setCenterPanel(JLabel("Failed to create ${kind.lowercase()} file '" + outputFile.name + "' for writing"))
            builder.show()
            return false
        }
    }
    try {
        FileOutputStream(outputFile).use { outputStream ->
            var bytes: ByteArray? = null
            try {
                bytes = image.toPngByteArray()
            } catch (e: AssertionError) {
                LOGGER.severe(e.localizedMessage)
                e.printStackTrace()
            } catch (e: Exception) {
                e.rethrowCancellationException()
                e.printStackTrace()
            }
            if (bytes == null || bytes.size < 20) {
                val builder = DialogBuilder()
                builder.setTitle("$kind save error")
                builder.setCenterPanel(JLabel("Failed to prepare rendered ${kind.lowercase()} for writing"))
                builder.show()
                return false
            }
            outputStream.write(bytes)
            outputStream.close()
        }
    } catch (e: IOException) {
        val builder = DialogBuilder()
        builder.setTitle("$kind save error")
        builder.setCenterPanel(JLabel("Failed to save ${kind.lowercase()} image to '" + outputFile.path + "'"))
        builder.show()
    }
    val thisFile =
        VfsUtil.findFileByIoFile(outputFile.parentFile, true)
    if (thisFile != null && thisFile.parent != null) {
        thisFile.parent.refresh(false, true)
    }
    return true
}