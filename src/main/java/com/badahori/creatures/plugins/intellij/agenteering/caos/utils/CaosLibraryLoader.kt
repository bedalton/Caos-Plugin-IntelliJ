package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Field

object CaosLibraryLoader{

    /**
     * Puts library to temp dir and loads to memory
     */
    @JvmStatic
    fun loadLib(pathIn: String) : Boolean {
        val pathTemp = if (pathIn.endsWith("dll"))
            pathIn
        else
            "$pathIn.dll"
        val myFile: String = FileNameUtils.getBaseName(pathIn)
        try {
            // have to use a stream
            val inputStream: InputStream = javaClass.classLoader.getResourceAsStream(pathTemp)
                    ?: throw Exception("Failed to get resource as stream")
            // always write to different location
            val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
            val out: OutputStream = FileOutputStream(fileOut)
            IOUtils.copy(inputStream, out)
            inputStream.close()
            out.close()

            val directory = fileOut.parentFile
            addLibraryPath(directory.absolutePath)
            System.loadLibrary(myFile)
            return true
        } catch (e: Exception) {
            throw Exception("Failed to load required DLL: $pathTemp with error: ${e.message}")
        }
    }

    /**
     * Adds the specified path to the java library path
     *
     * @param pathToAdd the path to add
     * @throws Exception
     */
    @Throws(java.lang.Exception::class)
    private fun addLibraryPath(pathToAdd: String) {
        val usrPathsField: Field = ClassLoader::class.java.getDeclaredField("usr_paths")
        usrPathsField.isAccessible = true

        //get array of paths
        val paths = getUserPaths()
        if (paths.contains(pathToAdd))
            return

        //check if the path to add is already present
        for (path in paths) {
            if (path == pathToAdd) {
                return
            }
        }

        //add the new path
        val newPaths: Array<String> = paths + pathToAdd
        usrPathsField.set(null, newPaths)
    }

    private fun getUserPaths() : Array<String>  {
        val usrPathsField: Field = ClassLoader::class.java.getDeclaredField("usr_paths")
        usrPathsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return usrPathsField.get(null) as Array<String>
    }
}