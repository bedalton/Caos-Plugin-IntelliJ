package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


/**
 * Connection class for managing POST based CAOS injection
 * Requires CAOS server executable to be running to handle caos injection post requests
 */
internal class WineConnection(private val variant: CaosVariant, creaturesDirectory: String) : CaosConnection {

    private val creaturesDirectory: File = File(creaturesDirectory)
    private val exeName = "CreaturesCaosInjector_WINE.exe"

    override val supportsJect: Boolean
        get() = false


    private fun ensureCopied() {
        //
    }


    /**
     * Ensures that the bundled exe is extracted to accessible location to be run
     */
    private fun ensureExe(clear: Boolean): File {

        val pathTemp = "c3engine/$exeName"
        // have to use a stream
        val inputStream: InputStream = try {
            javaClass.classLoader.getResourceAsStream(pathTemp)
                ?: throw Exception("Failed to get injector EXE resource as stream")
        } catch (e:Exception) {
            throw Exception("Failed to get injector EXE resource as stream. Error: ${e.message}")
        }
        // always write to different location
        val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
        if (fileOut.exists()) {
            if (clear) {
                try {
                    fileOut.delete()
                } catch (e: Exception) {
                    LOGGER.severe("Failed to delete prior injector EXE")
                }
            } else {
                return fileOut
            }
        }
        inputStream.use {stream ->
            val success = try {
                CaosFileUtil.copyStreamToFile(stream, fileOut, true)
            } catch(e:Exception) {
                throw IOException("Failed to copy Injector EXE by stream to run directory. Error: ${e.message}")
            }
            if (!success) {
                throw IOException("Failed to copy Injector EXE by stream to run directory")
            }
        }
        return fileOut
    }

    private fun installDotNet5Dialog(): DialogBuilder {
        TODO("Implement dot net install")
    }


    private fun dot5Installed(): Boolean {
        val rootDriveC = rootDriveC
            ?: return false
        val sdkPath = File(rootDriveC, "Program Files/dotnet/sdk")
        if (!sdkPath.exists()) {
            return false
        }
        return false//sdkPath.list().any { it.startsWith("Dot")}
    }

    private val rootDriveC: File? get() {
        var start: File? = creaturesDirectory
        while (start != null) {
            val isRoot = start.list()?.let { list ->
                list.any { it.toLowerCase() == "windows" } && list.any { it.toLowerCase() == "program files" }
            } == true
            if (isRoot) {
                break
            }
            start = start.parentFile
        }
        return start
    }

    private fun notImplemented(): InjectionStatus {
        return InjectionStatus.BadConnection("Wine based injection is not yet supported")
    }

    override fun inject(caos: String): InjectionStatus {
        return notImplemented()
    }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
        return if (variant.isOld) {
            return InjectionStatus.BadConnection("JECT based injection is not supported in C1E")
        } else {
            injectWithJect(
                connection = this,
                creaturesDirectory = creaturesDirectory,
                caos = caos.text,
                flags = flags
            )
        }
    }

    override fun injectEventScript(
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String
    ): InjectionStatus {
        val expectedHeader = "scrp $family $genus $species $eventNumber"
        val removalRegex = "^scrp\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s*".toRegex()
        // Does not have expected event script header
        val caosFormatted = if (!caos.trim().lowercase().startsWith(expectedHeader)) {
            // If script has a header, but for a different family, genus, species and event number
            // Replace the bad header with the correct one
            val stripped = if (removalRegex.matches(caos)) {
                caos.replace(removalRegex, "").trim()
            } else {
                caos.trim()
            }
            // Combine the expected header with the script body
            "$expectedHeader $stripped"
        } else
            caos
        return inject(caosFormatted)
    }

    override fun disconnect(): Boolean = true

    override fun isConnected(): Boolean = true

    override fun connect(silent: Boolean): Boolean = true

    override fun showAttribution(project: Project, variant: CaosVariant) {
//        postInfo(project, "", "Requires CAOS injector server from Bedalton")
    }
}