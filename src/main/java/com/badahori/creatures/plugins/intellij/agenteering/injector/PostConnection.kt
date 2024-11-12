package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.messageOrNoneText
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.injector.C3Connection.Companion.MAX_CAOS_FILE_LENGTH
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
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
internal class PostConnection(
    override val variant: CaosVariant,
    private val gameInterfaceName: PostInjectorInterface,
) : CaosConnection {

    override val supportsJect: Boolean
        get() = false

    override val maxCaosLength: Int
        get() = MAX_CAOS_FILE_LENGTH

    private val url: URL? by lazy {
        gameInterfaceName.getURL(variant)
    }

    override fun inject(
        project: Project,
        fileName: String,
        descriptor: String?,
        caos: String,
    ): InjectionStatus {
        val url = url
            ?: return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.post.invalid-url"),
                variant
            )
        val connection: HttpURLConnection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.failed-to-open-connection", e.messageOrNoneText()),
                variant
            )
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
            e.rethrowAnyCancellationException()
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.post.failed-to-write-to-stream", e.messageOrNoneText()),
                variant
            )
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
            e.rethrowAnyCancellationException()
            return InjectionStatus.Bad(
                fileName,
                descriptor,
                message("caos.injector.errors.post.read-response-failed", e.messageOrNoneText())
            )
        }
        val json = com.google.gson.JsonParser.parseString(response)
        return json.asJsonObject.let {
            val status = try {
                it.get("status").asString
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                LOGGER.severe("Invalid injection response. Response: <$response> not valid JSON; Error: " + e.message)
                e.printStackTrace()
                return InjectionStatus.Bad(fileName, descriptor, message("caos.injector.errors.json-invalid", response))
            }
            val message = try {
                it.get("response").asString ?: ""
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                ""
            }
            if (message.contains("{@}"))
                return InjectionStatus.Bad(fileName, descriptor, message)
            when (status) {
                "!ERR" -> InjectionStatus.Bad(fileName, descriptor, message)
                "!CON", "!CONN" -> InjectionStatus.BadConnection(fileName, descriptor, message, variant)
                "OK" -> InjectionStatus.Ok(fileName, descriptor, message)
                else -> InjectionStatus.Bad(
                    fileName,
                    descriptor,
                    message("caos.injector.errors.invalid-status", status),
                )
            }
        }
    }

    override fun injectWithJect(project: Project, caos: CaosScriptFile, flags: Int): InjectionStatus {
        return InjectionStatus.ActionNotSupported(
            caos.name,
            null,
            message("caos.injector.errors.ject-not-supported-by-injector", "POST")
        )
    }

    override fun injectEventScript(
        project: Project,
        fileName: String,
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
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
        return inject(project, fileName, "scrp $family $genus $species $eventNumber", caosFormatted)
    }

    override fun disconnect(): Boolean = true

    override fun isConnected(): Boolean = true

    override fun connect(silent: Boolean): Boolean = true

    override fun showAttribution(project: Project, variant: CaosVariant) {
//        postInfo(project, "", "Requires CAOS injector server from Bedalton")
    }
}