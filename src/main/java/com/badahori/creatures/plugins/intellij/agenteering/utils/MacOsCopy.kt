package com.badahori.creatures.plugins.intellij.agenteering.utils

import bedalton.creatures.util.Log
import bedalton.creatures.util.className
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files


private var wasSuccessful: Boolean? = null

internal fun ensureMacOsCopyLib(): Boolean {
    wasSuccessful?.let {
        Log.i { "Ensure NSPasteboard, was successful? $it" }
        // Was successful was set, meaning an attempt was made to load the dylib
        return it
    }
    if (!OsUtil.isMac) {
        Log.i { "Do not setup NSPasteboard. OS is not macOS" }
        wasSuccessful = true
        return true
    }
    try {
        val libraryPath = ensurePasteboardLibWasCopied()
        if (libraryPath == null) {
            wasSuccessful = false
            return false
        }
        System.load(libraryPath)
        Log.i { "Did load NSClipboard bridge" }
        wasSuccessful = true
        return true
    } catch (e: IOException) {
        Log.e { "Failed to load MacOS copy lib: ${e.className}: ${e.message}" }
        e.printStackTrace()
        wasSuccessful = false
        return false
    } catch (e: Error) {
        Log.e { "Failed with ERROR (Not Exception) to load MacOS copy lib: ${e.className}: ${e.message}" }
        e.printStackTrace()
        wasSuccessful = false
        return false
    }
}

private fun ensurePasteboardLibWasCopied(): String? {
    val pathTemp = "libs/libNSPasteboardJNI.dylib"
    val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
    if (fileOut.exists()) {
        return fileOut.path
    }
    val inputStream = object {}.javaClass.classLoader.getResourceAsStream(pathTemp)
        ?: return null
    inputStream.use { stream ->
        val success = try {
            CaosFileUtil.copyStreamToFile(stream, fileOut, true)
        } catch (e: Exception) {
            throw IOException("Failed to copy JNI Pasteboard dylib by stream to run directory. ${e.className}: ${e.message}")
        }
        if (!success) {
            throw IOException("Failed to copy JNI Pasteboard dylib by stream to run directory")
        }
        return fileOut.path
    }
}