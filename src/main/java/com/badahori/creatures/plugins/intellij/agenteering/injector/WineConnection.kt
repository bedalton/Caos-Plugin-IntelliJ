package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import java.io.File
import java.util.*

/**
 * Class for managing WINE based CAOS injection
 * Requires watcher launcher
 */
internal class WineConnection(path:String, private val variant: CaosVariant) : CaosConnection {

    val prefix: String? by lazy {
        var file: File? = File(basePath)
        while (file != null && file.parentFile.nameWithoutExtension != "drive_c")
            file = file.parentFile
        file?.parentFile?.path
    }

    val homePath: String? by lazy {
        File(basePath).parentFile?.path?.let {
            if (it.endsWith("/"))
                it
            else
                it + "/"
        }
    }

    val basePath: String by lazy {
        when {
            path.endsWith("CaosQueue") -> path + "/"
            path.endsWith("CaosQueue/") -> path
            path.endsWith("/") -> "${path}CaosQueue/"
            else -> "${path}/CaosQueue/"
        }
    }

    val writeDirectory: String by lazy {
        basePath + "input/"
    }

    val readDirectory: String by lazy {
        basePath + "out/"
    }

    override fun connect(silent: Boolean): Boolean = true /*{
        if (isConnected())
            return true
        val launcherFile = File(basePath+variant.code+"Launcher.exe")
        if (!launcherFile.exists())
            return false
        val homePath = homePath ?: return false
        val params = listOf(
                "wine",
                "start",
                "/d",
                homePath,
                "${homePath}${variant.code}Launcher.exe"
        ).toTypedArray()
        val env = listOfNotNull(
                prefix?.let { "WINEPREFIX=$it"}
        ).toTypedArray()
        var timeout = 10000L
        val sleepFor = 200L
        Runtime.getRuntime().exec(params, env)
        while (timeout > 0 && !isConnected()) {
            timeout -= sleepFor
            Thread.sleep(sleepFor)
        }
        return isConnected()
    }*/

    override fun showAttribution(project: Project, variant: CaosVariant) {

    }

    override fun inject(caos: String): InjectionStatus {
        val uuid = UUID.randomUUID().toString()
        val writeFile = File("$writeDirectory$uuid.cos")
        writeFile.createNewFile()
        writeFile.writeText(caos)
        val readFile = File("$readDirectory$uuid.response.json")
        var timeOut = 6000L
        val sleepInterval = 200L
        while (!readFile.exists() && timeOut > 0) {
            timeOut -= sleepInterval
            Thread.sleep(sleepInterval)
        }
        return if (readFile.exists()) {
            val response = try {
                val response = Gson().fromJson(readFile.readText(), WineCaosResponse::class.java)
                when (response.status) {
                    "NO_CONNECTION" -> {
                        val message = response.messageOr { "Failed to connect to Creatures executable" }
                        InjectionStatus.BadConnection(message)
                    }
                    "BAD" -> InjectionStatus.Bad(response.messageOr { "An unknown error occurred" })
                    "OK" -> InjectionStatus.Ok(response.response ?: "")
                    else -> InjectionStatus.Bad("Invalid response received. Data: '" + readFile.readText() + "'")
                }
            } catch (e: Exception) {
                InjectionStatus.Bad("Invalid response received. Data: '" + readFile.readText() + "'")
            }
            try {
                readFile.delete()
            } catch (e: Exception) {
                LOGGER.severe("Failed to delete response file: ${readFile.path}")
            }
            response
        } else {
            InjectionStatus.Bad("System timed out while waiting for response")
        }
    }

    override fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber: Int, caos: String): InjectionStatus {
        return InjectionStatus.BadConnection("Server does not implement inject event script")
    }

    override fun disconnect(): Boolean = true

    override fun isConnected(): Boolean = true /*{
        if (!File(basePath).exists())
            return false
        val params = listOf(
                "winedbg",
                "--command",
                "'info proc'"
        ).toTypedArray()
        val env = listOfNotNull(
                prefix?.let { "WINEPREFIX=$it"}
        ).toTypedArray()
        val process = Runtime.getRuntime().exec(params, env)
        process.waitFor()
        val response = process.inputStream.bufferedReader().readText().toLowerCase()
        if (response.isEmpty())
            return false
        return response.contains("'${variant.code}Launcher.exe".toLowerCase())
    }*/
}

internal data class WineCaosResponse(var status: String, var response: String? = null, var error: String? = null) {
    val errorOrMessage: String?
        get() = error?.nullIfEmpty() ?: response?.nullIfEmpty()

    fun messageOr(producer: () -> String): String {
        return errorOrMessage ?: producer()
    }
}

