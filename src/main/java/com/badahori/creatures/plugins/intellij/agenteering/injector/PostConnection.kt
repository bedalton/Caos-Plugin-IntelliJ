package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * Connection class for managing POST based CAOS injection
 * Requires CAOS server executable to be running to handle caos injection post requests
 */
internal class PostConnection(urlString: String, variant: CaosVariant?) : CaosConnection {

    override val supportsJect: Boolean
        get() = false

    private val url: URL by lazy {
        val path = if (urlString.contains("*")) {
            if (variant == null)
                throw Exception("Cannot use dynamic POST route. Variant is null")
            urlString.replace("*", variant.code)
        } else
            urlString
        LOGGER.info("URLPath: $path")
        if (path.startsWith("http"))
            URL(path)
        else
            URL("http://$path")
    }

    override fun inject(caos: String): InjectionStatus {
        val connection: HttpURLConnection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return InjectionStatus.BadConnection("Failed to open caos connection. Error: ${e.message}")
        }
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = 7000
        connection.requestMethod = "POST"
        try {
            connection.outputStream.apply {
                write(caos.toByteArray())
                flush()
                close()
            }
        } catch (e: Exception) {
            return InjectionStatus.BadConnection("Failed to write caos code to stream. Error: ${e.message}")
        }
        val response = try {
            val inputStream = BufferedReader(
                InputStreamReader(connection.inputStream)
            )
            var inputLine: String?
            val content = StringBuffer()
            while (inputStream.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            inputStream.close()
            content.toString()
        } catch (e: Exception) {
            return InjectionStatus.Bad("Failed to read response from caos server. Error: ${e.message}")
        }
        LOGGER.info("CAOSRESPONSE: $response")
        val json = com.google.gson.JsonParser().parse(response)
        return json.asJsonObject.let {
            val status = try {
                it.get("status").asString
            } catch (e: Exception) {
                LOGGER.severe("Invalid injection response. Response: <$response> not valid JSON; Error: " + e.message)
                e.printStackTrace()
                return InjectionStatus.Bad("Invalid injection response. Response: <$response> not valid JSON")
            }
            val message = try {
                it.get("response").asString ?: ""
            } catch (e: Exception) {
                ""
            }
            if (message.contains("{@}"))
                return InjectionStatus.Bad(message)
            when (status) {
                "!ERR" -> InjectionStatus.Bad(message)
                "!CON", "!CONN" -> InjectionStatus.BadConnection(message)
                "OK" -> InjectionStatus.Ok(message)
                else -> InjectionStatus.Bad("Invalid status: '$status'  returned")
            }
        }
    }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
        throw Exception("JECT not supported by POST connection")
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
        val caosFormatted = if (!caos.trim().lowercase().startsWith(expectedHeader)) {
            if (removalRegex.matches(caos)) {
                caos.replace(removalRegex, "")
            } else
                "$expectedHeader $caos"
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