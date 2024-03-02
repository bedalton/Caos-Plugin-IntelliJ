package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.formatted
import java.io.File
import java.io.InputStream


object CaosLibraryLoader {

    /**
     * Puts library to temp dir and loads to memory
     */
    @JvmStatic
    fun loadLib(pathIn: String): Boolean {
        val pathTemp = if (pathIn.endsWith("dll")) {
            pathIn
        } else {
            "$pathIn.dll"
        }
        val myFile: String = PathUtil.getFileNameWithoutExtension(pathIn)!!

        try {
            // have to use a stream
            val dllInputStream: InputStream = javaClass.classLoader.getResourceAsStream(pathTemp)
                ?: throw Exception("Failed to get resource as stream")
            // always write to different location
            val fileOut = File.createTempFile(pathTemp.replace("\\","_").replace("/", "_"), "")
            if (fileOut.exists()) {
                try {
                    fileOut.delete()
                } catch (_: Exception) {

                }
            }

            try {
                dllInputStream.use { inputStream ->
                    CaosFileUtil.copyStreamToFile(inputStream, fileOut, true)
                }
            } catch (e: Exception) {
                if (!fileOut.exists()) {
                    return false
                }
            }
            System.load(fileOut.absolutePath)
            return true
        } catch (e: Exception) {
            throw Exception("Failed to load required DLL: $pathTemp with error: ${e.formatted(true)}")
        }
    }

}