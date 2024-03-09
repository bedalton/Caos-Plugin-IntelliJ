@file:Suppress("SameParameterValue")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil.isWindows
import com.bedalton.common.util.formatted
import com.bedalton.common.util.nullIfEmpty
import com.bedalton.common.util.trySilent
import com.bedalton.io.bytes.encodeToWindowsCP1252EncodedBytes
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.intellij.openapi.project.Project
import java.io.*
import java.net.Socket


/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class TCPConnection(
    override val variant: CaosVariant,
    private val data: TCPInjectorInterface,
) : CaosConnection {

    override val supportsJect: Boolean
        get() = false


    override val maxCaosLength: Int
        get() = MAX_PAYLOAD_SIZE

    private val homeDirectory by lazy {
        val path = System.getProperty("user.dir")
            .nullIfEmpty()
        if (path == null) {
            return@lazy null
        }
        val file = File(path)
        if (file.exists()) {
            file
        } else {
            null
        }
    }

    private val host: String by lazy {
        data.path.nullIfEmpty() ?: "127.0.0.1"
    }

    private val port: Int
        get() = trySilent {
            val home = homeDirectory
                ?: return@trySilent null
            val file = File(home, ".creaturesengine/port")
            if (file.exists()) {
                file.readText().toIntOrNull()
            } else {
                null
            }
        } ?: DEFAULT_PORT

    override fun injectWithJect(project: Project, caos: CaosScriptFile, flags: Int): InjectionStatus {
        return InjectionStatus.ActionNotSupported(
            caos.name,
            null,
            message("caos.injector.ject-not-supported")
        )
    }

    /**
     * Inject event script into CV+ games
     */
    override fun injectEventScript(
        project: Project,
        fileName: String,
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
    ): InjectionStatus {

        val header = "scrp $family $genus $species $eventNumber"

        val preparedCaos = if (caos.lowercase().trim().startsWith("scrp")) {
            caos
        } else {
            "$header $caos"
        }

        return inject(
            project,
            fileName,
            header,
            preparedCaos
        )
    }


    /**
     * Inject caos code into CV+ games
     */
    override fun inject(
        project: Project,
        fileName: String,
        descriptor: String?,
        caos: String,
    ): InjectionStatus {

        if (isWindows) {
            return NOT_NIX_STATUS
        }

        // Ensure that the exe has been extracted and placed in accessible folder
        val length = caos.length + TERMINATOR.length

        if (length > MAX_PAYLOAD_SIZE) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.individual-script-too-long", MAX_PAYLOAD_SIZE, length),
                variant
            )
        }

        val clientSocket = try {
            Socket(host, port)
        } catch (e: Exception) {
            LOGGER.severe("Failed to connect to TCP @ $host:$port; ${e.formatted(true)}")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                e.message ?: message("caos.injector.errors.failed-to-connect", "TCP on Port $port"),
                variant
            )
        }

        return clientSocket.use { socket ->
            runSafe(socket, fileName, descriptor, caos + TERMINATOR)
        }
    }

    private fun runSafe(
        socket: Socket,
        fileName: String,
        descriptor: String?,
        caos: String,
    ): InjectionStatus {

        // Write CAOS to socket
        // If error, grab injector status and return
        val output = try {
            socket.getOutputStream()
        } catch (e: Exception) {
            LOGGER.severe("Failed to get output stream for TCP")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.post.failed-to-write-to-stream", e.formatted(false)),
                variant
            )
        }

        writeToSocket(
            output,
            fileName,
            descriptor,
            caos
        )?.let {
            return it
        }

        val input = try {
            socket.getInputStream()
        } catch (e: Exception) {
            LOGGER.severe("Failed to get input stream for TCP response")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.post.read-response-failed", e.formatted(false)),
                variant
            )
        }

        // Read response or return error status if read failed
        val response = readFromSocket(
            input,
            fileName,
            descriptor,
        )
        trySilent { output.close() }
        trySilent { input.close() }
        trySilent { socket.close() }
        return response
    }

    private fun writeToSocket(
        output: OutputStream,
        fileName: String,
        descriptor: String?,
        caos: String,
    ): InjectionStatus? {
        val encoded = caos.encodeToWindowsCP1252EncodedBytes()
        try {
            output.write(encoded)
        } catch (e: Exception) {
            LOGGER.severe("Failed to write to TCP stream @ $host:$port; ${e.formatted(true)}")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.tcp.failed-to-write-to-stream", e.formatted(false)),
                variant
            )
        }
        return null
    }

    private fun readFromSocket(
        inputStream: InputStream,
        fileName: String,
        descriptor: String?,
    ): InjectionStatus {
        try {
            val response = BufferedReader(InputStreamReader(inputStream)).use { input ->
                input.readText()
            }
            return getResponseStatus(fileName, descriptor, response)
        } catch (e: Exception) {
            LOGGER.severe("Failed to read from TCP stream @ $host:$port; ${e.formatted(true)}")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.tcp.failed-to-read-from-stream", e.formatted(false)),
                variant
            )
        }
    }


    private fun getResponseStatus(
        fileName: String,
        descriptor: String?,
        response: String,
    ): InjectionStatus {
        return if (InjectorHelper.isErrorResponse(response)) {
            Log.iIf(DEBUG_INJECTOR) { "C3DS(TCP): CAOS Error: $response" }
            InjectionStatus.Bad(
                fileName,
                descriptor,
                response
            )
        } else {
            InjectionStatus.Ok(
                fileName,
                descriptor,
                response
            )
        }
    }

    /**
     * Disconnect does nothing as there is no persistent connection
     */
    override fun disconnect(): Boolean {
        return true
    }

    /**
     * Assumes always connected
     */
    override fun isConnected(): Boolean {
        return true
    }

    /**
     * Returns true as every call to inject creates a new thread
     */
    override fun connect(silent: Boolean): Boolean {
        return true
    }

    override fun showAttribution(project: Project, variant: CaosVariant) {

    }

    companion object {
        private const val DEFAULT_PORT = 20001
        private const val MAX_PAYLOAD_SIZE: Int = 64 * 1024
        private const val TERMINATOR = "\nrscr"
    }


}