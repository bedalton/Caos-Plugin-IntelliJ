package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


internal class PostConnection(urlString:String, variant:CaosVariant) : CaosConnection {

    private val url:URL by lazy {
        val pathComponent = variant.code.toLowerCase()
        val path =  if (urlString.endsWith("/")) {
            urlString + pathComponent
        } else {
            "$urlString/$pathComponent"
        }
        if (path.startsWith("http"))
            URL(path)
        else
            URL("http://$path")
    }

    override fun inject(caos:String): InjectionStatus  {
        val connection: HttpURLConnection = try {
            url.openConnection() as HttpURLConnection
        } catch (e:IOException) {
            return InjectionStatus.BadConnection("Failed to open caos connection. Error: ${e.message}")
        }
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = 4000
        connection.requestMethod = "POST"
        try {
            connection.outputStream.let {
                it.write(caos.toByteArray())
                it.flush()
                it.close()
            }
        } catch (e:Exception) {
            return InjectionStatus.BadConnection("Failed to write caos code to stream. Error: ${e.message}")
        }
        val response = try {
            val inputStream = BufferedReader(
                    InputStreamReader(connection.getInputStream()))
            var inputLine: String?
            val content = StringBuffer()
            while (inputStream.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            inputStream.close()
            content.toString()
        } catch (e:Exception) {
            return InjectionStatus.Bad("Failed to read response from caos server. Error: ${e.message}")
        }
        LOGGER.info("RESPONSE: $response")
        val json = com.google.gson.JsonParser().parse(response)
        return json.asJsonObject.let {
            val message = try {
                it.get("response").asString
            } catch (e:Exception) {
                return InjectionStatus.Bad("Failed to parse server response")
            }
            when (it.get("status").asString) {
                "!ERR" -> InjectionStatus.Bad(message)
                "!CON" -> InjectionStatus.BadConnection(message)
                else -> InjectionStatus.Ok(message)
            }
        }
    }

    override fun disconnect(): Boolean = true

    override fun isConnected(): Boolean = true

    override fun connect(silent: Boolean): Boolean = true

    override fun showAttribution(project: Project, variant: CaosVariant) {
        Injector.postInfo(project, "", "Requires CAOS injector server from Bedalton")
    }
}