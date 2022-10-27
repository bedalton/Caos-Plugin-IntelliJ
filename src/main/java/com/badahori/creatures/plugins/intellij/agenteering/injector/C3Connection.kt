@file:Suppress("SameParameterValue")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import bedalton.creatures.util.Log
import bedalton.creatures.util.iIf
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil.isWindows
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.io.*


/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class C3Connection(override val variant: CaosVariant, private val data: GameInterfaceName) : CaosConnection {

    override val supportsJect: Boolean
        get() = false
    private var tempFileIndex = 1
    private var ranOnce = false
    private val exeName = "C3CaosInjector.exe"
    private val file: File by lazy {
        val file = if (isWindows) {
            ensureExe(!ranOnce)
        } else {
            ensureLib(!ranOnce)
        }
        ranOnce = true
        file
    }

    private val gameName: String?
        get() {
            val gameName = data.gameName
                ?: return if (variant.isBase) {
                    variant.code
                } else {
                    null
                }
            val isC2eCode = gameName.length == 2 && CaosVariant.fromVal(gameName).nullIfUnknown()?.isNotOld == true
            return if (!isC2eCode && !gameName.startsWith('@')) {
                "@$gameName"
            } else {
                gameName
            }
        }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
        return InjectionStatus.ActionNotSupported(
            caos.name,
            null,
            message("caos.injector.ject-not-supported")
        )
    }

    @Suppress("SameParameterValue")
    override fun inject(fileName: String, descriptor: String?, caos: String): InjectionStatus {
        if (!isWindows) {
            return NOT_WINDOWS_STATUS
        }
        val length = caos.length
        val base64Length = length * 1.4
        val cliFlag: CLIInjectFlag = when {
            base64Length < MAX_CONSOLE_LENGTH -> CLIInjectFlag.CAOS_TEXT
            length < MAX_CAOS_FILE_LENGTH -> CLIInjectFlag.CAOS_FILE
            else -> return InjectionStatus.BadConnection(
                file.name,
                null,
                message("caos.injector.errors.individual-script-too-long", MAX_CAOS_FILE_LENGTH, length),
                variant
            )
        }
        return inject(fileName, descriptor, caos, cliFlag, 7)
    }

    /**
     * Injects raw CAOS using the Windows CLI injector
     * @param cliTypeFlag CLI Flag for injection type values: "-f" for file "-j" for ject style inject
     */
    private fun inject(
        fileName: String,
        descriptor: String?,
        caos: String,
        cliTypeFlag: CLIInjectFlag,
        flags: Int = 7,
    ): InjectionStatus {
        if (!isWindows) {
            return NOT_WINDOWS_STATUS
        }
        try {
            assertExeWasCopied()
        } catch (e: Exception) {
            LOGGER.severe("C3 Injector EXE was not copied")
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                e.message ?: message("caos.injector.failed-to-copy-executable"),
                variant
            )
        }
        val preparedCaos = try {
            prepareCaos(caos, cliTypeFlag)
        } catch (e: Exception) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                e.message ?: message("caos.injector.errors.failed-to-prepare-script", "script"),
                variant
            )
        }
        val gameName = gameName
            ?: return InjectionStatus.BadConnection(
                fileName,
                null,
                message("caos.injector.errors.concrete-game-name-or-variant-required"),
                variant
            )
        // Create cmd args for caos injector exe
        val args = listOf(
            "cmd",
            "/c",
            file.path,
            gameName,
            cliTypeFlag.shortFlag,
            preparedCaos,
            "" + flags
        ).toTypedArray()

        return try {
            Log.iIf(DEBUG_INJECTOR) { "Process with args: <${args.joinToString(" ")}>" }
            process(fileName, descriptor, args)
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                val tempFile = File(preparedCaos)
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                LOGGER.severe("Failed to delete temp CAOS file")
            }
        }
    }

    private fun assertExeWasCopied() {
        // Ensure that the exe has been extracted and placed in accessible folder
        val file = try {
            this.file
        } catch (e: Exception) {
            throw Exception("Failed to copy injector EXE to run directory. Error: ${e.message}.")
        }
        if (!file.exists()) {
            throw Exception("Failed to initialize communication executable")
        }
    }

    private fun writeTempFile(caos: String): File {
        val tempFile = File.createTempFile("CAOS", "${tempFileIndex++}".padStart(4, '0'))
        try {
            tempFile.writeText(caos, Charsets.UTF_8)
            return tempFile
        } catch (e: Exception) {
            try {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e2: Exception) {
                LOGGER.severe("Failed to delete temp CAOS file")
                e.printStackTrace()
            }
            throw Exception("Failed to write CAOS to temp file for injecting; CAOS too long for direct injection")
        }
    }

    /**
     * Inject caos code into CV+ games
     */
    override fun injectEventScript(
        fileName: String,
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
    ): InjectionStatus {
        if (!isWindows) {
            return NOT_WINDOWS_STATUS
        }
        // Ensure that the exe has been extracted and placed in accessible folder
        val length = caos.length
        val base64Length = length * 1.4
        val descriptor = "scrp $family $genus $species $eventNumber"
        val cliFlag: CLIInjectFlag = when {
            base64Length < MAX_CONSOLE_LENGTH -> CLIInjectFlag.EVENT_TEXT
            length < MAX_CAOS_FILE_LENGTH -> CLIInjectFlag.EVENT_FILE
            else -> return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.individual-script-too-long", MAX_CAOS_FILE_LENGTH, length),
                variant
            )
        }
        try {
            assertExeWasCopied()
        } catch (e: Exception) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                e.message ?: message("caos.injector.failed-to-copy-executable"),
                variant
            )
        }
        val caosOrFile = try {
            prepareCaos(caos, cliFlag)
        } catch (e: Exception) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                e.message ?: message("caos.injector.errors.failed-to-prepare-script", "event script"),
                variant
            )
        }
        val gameName = gameName
            ?: return InjectionStatus.BadConnection(
                fileName,
                null,
                message("caos.injector.errors.concrete-game-name-or-variant-required"),
                variant
            )
        // Create cmd args for caos injector exe
        val args = listOf(
            "cmd",
            "/c",
            file.path,
            gameName,
            cliFlag.shortFlag,
            "$family",
            "$genus",
            "$species",
            "$eventNumber",
            caosOrFile,
            "4"
        ).toTypedArray()


        return process(fileName, descriptor, args)
    }

    @Throws(Exception::class)
    private fun prepareCaos(caos: String, cliFlag: CLIInjectFlag): String {
        if (cliFlag == CLIInjectFlag.CAOS_TEXT) {
            return InjectorHelper.escape(caos)
        }
        val tempFile = try {
            writeTempFile(caos)
        } catch (e: Exception) {
            throw Exception(e.message ?: "Failed to write CAOS to temp file for inject")
        }
        return tempFile.path
    }

    private fun process(fileName: String, descriptor: String?, args: Array<String>): InjectionStatus {
        if (!isWindows) {
            return NOT_WINDOWS_STATUS
        }
        // Create injection process
        val proc: Process
        try {
            Log.iIf(DEBUG_INJECTOR) { "C3Injector: Start C3 process: <${args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it }}>" }
            proc = Runtime.getRuntime().exec(args)
        } catch (e: Exception) {
            e.printStackTrace()
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message("caos.injector.errors.failed-to-run-executable", e.message ?: "<none>"),
                variant
            )
        }

        // Parse result
        return try {
            proc.waitFor()
            if (proc.exitValue() != 0) {
                LOGGER.severe("Process exited non-zero")
                return try {
                    val error = proc.errorStream?.bufferedReader()?.readText()?.nullIfEmpty()
                    InjectionStatus.Bad(
                        fileName,
                        descriptor,
                        error ?: message("caos.injector.errors.injector-crashed-or-failed", "").trim()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    InjectionStatus.Bad(
                        fileName,
                        descriptor,
                        message("caos.injector.errors.injector-crashed-or-failed", e.message ?: "").trim()
                    )
                }
            }

            var response = proc.inputStream
                ?.bufferedReader(Charsets.UTF_8)
                ?.readText()
                .nullIfEmpty()
                ?: return InjectionStatus.Bad(fileName, descriptor, message("caos.injector.errors.no-response")).apply {
                    LOGGER.severe("Process returned no response")
                }
            //var response = responseBase64
            val code = if (response.length >= 4) {
                response.substring(0, 4)
            } else {
                response
            }
            when (code) {
                "!CMD" -> InjectionStatus.BadConnection(
                    fileName,
                    descriptor,
                    message("caos.injector.errors.internal-plugin-error", response.substring(4)),
                    variant
                )

                "!CON" -> InjectionStatus.BadConnection(fileName, descriptor, response.substring(4), variant)
                "!ERR" -> InjectionStatus.Bad(fileName, descriptor, response.substring(4))
                else -> {
                    if (InjectorHelper.isErrorResponse(response)) {
                        Log.iIf(DEBUG_INJECTOR) { "C3DS: CAOS Error: $response" }
                        InjectionStatus.Bad(
                            fileName,
                            descriptor,
                            response.substringFromEnd(if (response.startsWith("!RES")) 4 else 0, 1)
                        )
                    } else if (code == "!RES") {
                        if (response.last() == 0.toChar()) {
                            response = response.substringFromEnd(if (response.startsWith("!RES")) 4 else 0, 1)
                        } else if (response.startsWith("!RES")) {
                            response = response.substring(4)
                        }
                        InjectionStatus.Ok(fileName, descriptor, response)
                    } else {
                        // Allow for injectors other than mine. Return OK and response just in case
                        InjectionStatus.Ok(fileName, descriptor, response)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.severe("Caos injection failed with error: " + e.message)
            e.printStackTrace()
            InjectionStatus.Bad(
                fileName,
                descriptor,
                message("caos.injector.errors.response-parse-failed", e.message ?: "<none>")
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
        CaosInjectorNotifications.show(
            project,
            "Attribution",
            message("caos.injector.attribution.c3-windows", variant.code),
            NotificationType.INFORMATION
        )
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
        } catch (e: Exception) {
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
        inputStream.use { stream ->
            val success = try {
                CaosFileUtil.copyStreamToFile(stream, fileOut, true)
            } catch (e: Exception) {
                throw IOException("Failed to copy Injector EXE by stream to run directory. Error: ${e.message}")
            }
            if (!success) {
                throw IOException("Failed to copy Injector EXE by stream to run directory")
            }
        }
        return fileOut
    }

    /**
     * Ensures that the bundled exe is extracted to accessible location to be run
     */
    private fun ensureLib(clear: Boolean): File {
        val pathTemp = "c3engine/$exeName"
        // have to use a stream
        val inputStream: InputStream = javaClass.classLoader.getResourceAsStream(pathTemp)
            ?: throw Exception("Failed to get resource as stream")
        // always write to different location
        val fileOut = File(System.getProperty("java.io.tmpdir") + "/" + pathTemp)
        if (fileOut.exists()) {
            if (clear) {
                fileOut.delete()
            } else
                return fileOut
        }
        inputStream.use { stream ->
            CaosFileUtil.copyStreamToFile(stream, fileOut, true)
        }
        return fileOut
    }

    companion object {

        private const val MAX_CONSOLE_LENGTH: Int = 7590
        private const val MAX_CAOS_FILE_LENGTH = 14100
    }


}


internal enum class CLIInjectFlag(val shortFlag: String) {
    CAOS_TEXT("-c"),
    CAOS_FILE("-f"),
    EVENT_TEXT("-s"),
    EVENT_FILE("-z"),

    @Suppress("unused")
    JECT("-j");

}