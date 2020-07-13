package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.substringFromEnd
import com.intellij.openapi.project.Project
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class C3Connection(private val variant: CaosVariant) : CaosConnection {

    private val exeName = "C3Engine.exe";
    private val file:File by lazy {
        ensureExe()
    }

    override fun inject(caos: String): InjectionStatus {
        val file = this.file
        if (!file.exists()) {
            return InjectionStatus.BadConnection("Failed to initialize communication executable")
        }
        var escaped = caos.trim()
        if (escaped.isBlank()) {
            return InjectionStatus.Bad("Cannot inject empty command")
        }
        if (escaped.toLowerCase().endsWith("endm")) {
            escaped = escaped.substringFromEnd(0, 4).trim()
        }
        escaped = escaped.replace("\"", "\\\"").replace("\n", "\\\n")
        if (escaped.isEmpty()) {
            return InjectionStatus.Ok("");
        }
        val args = listOf(
                "cmd",
                "/c",
                file.path,
                variant.code,
                "-c",
                escaped

        ).toTypedArray()
        val proc:Process
        try {
            proc = Runtime.getRuntime().exec(args)
        } catch (e:Exception) {
            e.printStackTrace()
            return InjectionStatus.BadConnection("Failed to run executable with error: ${e.message};")
        }
        try {
            proc.waitFor()
        } catch (e:Exception) {
            e.printStackTrace()
            InjectionStatus.Bad("Injection process interrupted.")
        }
        return try {
            val response = proc.inputStream.bufferedReader().readLines().joinToString("\n").trim().substringFromEnd(0,1)
            when (response.substring(0, 4)) {
                "!CMD" -> InjectionStatus.BadConnection("Internal plugin run error. "+response.substring(4))
                "!CONN" -> InjectionStatus.BadConnection("Connection error: " + response.substring(4))
                "!ERR" -> InjectionStatus.Bad("Injection failed with error: " + response.substring(4))
                else -> InjectionStatus.Ok(response)
            }
        } catch (e:Exception) {
            InjectionStatus.Ok("")
        }
    }

    override fun disconnect(): Boolean {
        return true
    }

    override fun isConnected(): Boolean {
        return false
    }

    override fun connect(silent: Boolean): Boolean {
        return true
    }

    private fun ensureExe(): File {
        val pathTemp = "c3engine/$exeName"
        val path = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath(pathTemp)?.path
            ?: throw Exception("$exeName does not exist in plugin")
        LOGGER.info("$exeName Path: $path")
        // have to use a stream
        val inputStream: InputStream = javaClass.classLoader.getResourceAsStream(pathTemp)
                ?: throw Exception("Failed to get resource as stream")
        // always write to different location
        val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
        if (fileOut.exists())
            return fileOut;
        LOGGER.info("Writing dll to: " + fileOut.absolutePath)
        val out: OutputStream = FileUtils.openOutputStream(fileOut)
        IOUtils.copy(inputStream, out)
        inputStream.close()
        out.close()
        return fileOut
    }

}