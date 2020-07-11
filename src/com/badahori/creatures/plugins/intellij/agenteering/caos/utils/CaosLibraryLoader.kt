package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.util.*

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
        val myFile: String = FilenameUtils.getBaseName(pathIn)
        try {
            val path = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath(pathTemp)?.path
                    ?: throw Exception("DLL does not exist in plugin")
            LOGGER.info("DLL Path: $path")
            // have to use a stream
            val inputStream: InputStream = javaClass.classLoader.getResourceAsStream(pathTemp)
                    ?: throw Exception("Failed to get resource as stream")
            // always write to different location
            val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
            LOGGER.info("Writing dll to: " + fileOut.absolutePath)
            val out: OutputStream = FileUtils.openOutputStream(fileOut)
            IOUtils.copy(inputStream, out)
            inputStream.close()
            out.close()

            val directory = fileOut.parentFile
            addLibraryPath(directory.absolutePath)
            System.loadLibrary(myFile)
            return true;
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
        usrPathsField.setAccessible(true)

        //get array of paths
        val paths = getUserPaths();
        if (paths.contains(pathToAdd))
            return

        //check if the path to add is already present
        for (path in paths) {
            if (path == pathToAdd) {
                return
            }
        }

        //add the new path
        val newPaths: Array<String> = Arrays.copyOf(paths, paths.size + 1)
        newPaths[newPaths.size - 1] = pathToAdd
        usrPathsField.set(null, newPaths)
    }

    private fun getUserPaths() : Array<String>  {
        val usrPathsField: Field = ClassLoader::class.java.getDeclaredField("usr_paths")
        usrPathsField.isAccessible = true
        return usrPathsField.get(null) as Array<String>
    }
}