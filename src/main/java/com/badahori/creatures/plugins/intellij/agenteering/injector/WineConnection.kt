@file:Suppress("unused", "UNUSED_PARAMETER")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.messageOrNoneText
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.injector.CLIInjectFlag.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.common.util.*
import com.bedalton.io.bytes.decodeToWindowsCP1252
import com.bedalton.log.Log
import com.bedalton.log.eIf
import com.bedalton.log.iIf
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import korlibs.io.util.escape
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class WineConnection(
    override val variant: CaosVariant,
    private val data: WineInjectorInterface
) : CaosConnection {
    override val supportsJect: Boolean
        get() = false

    private val exeName = "ject.exe"
    private val file: File by lazy {
        val file = ensureExe(!ranOnce)
        ranOnce = true
        file
    }
    val prefix = File(data.prefix)


    val isWin32 by lazy {
        !File(prefix, "drive_c" + File.separator + "Program Files (x86)").exists()
    }

    val isWin64 get() = !isWin32

    private val tempDir by lazy {
        File(prefix, "windows/temp")
    }

    init {
        Log.setMode(DEBUG_INJECTOR, true)

    }

    override fun injectWithJect(project: Project, caos: CaosScriptFile, flags: Int): InjectionStatus {
//        Caos
//        val fileName = copyForJect(variant, data, caos.text)
        return InjectionStatus.ActionNotSupported(
            caos.name,
            null,
            CaosBundle.message("caos.injector.ject-not-supported")
        )
    }


    /**
     * Injects raw CAOS using the Windows CLI injector
     */
    override fun inject(project: Project, fileName: String, descriptor: String?, caos: String): InjectionStatus {
        val parts = prepareCaos(caos, true)
        val caosOrFile = parts.first
        val cliTypeFlag = parts.second
            ?: return InjectionStatus.BadConnection(fileName, descriptor, parts.first, variant)
        val flag = if (cliTypeFlag == CAOS_FILE || cliTypeFlag == EVENT_FILE) "-f" else "-b"
        // Create cmd args for caos injector exe
        val args = listOf(
            file.path,
            "macro",
            getGameName(),
            flag,
            caosOrFile
        ).toTypedArray()
        return try {
            process(project, fileName, descriptor, args)
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                val tempFile = File(caosOrFile)
                if (tempFile.exists()) {
//                    tempFile.delete()
                }
            } catch (e: Exception) {
                LOGGER.severe("Failed to delete temp CAOS file")
            }
        }
    }

    /**
     * Inject caos code into CV+ games
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
        val parts = prepareCaos(caos, macro = false)
        val caosOrFile = parts.first
        val descriptor = "scrp $family $genus $species $eventNumber"
        val cliFlag = parts.second
            ?: return InjectionStatus.BadConnection(fileName, descriptor, parts.first, variant)

        // If not event file, it is base 64 encoded
        val fileFlag = if (cliFlag == EVENT_FILE) "-f" else "-b"

        // Create cmd args for caos injector exe
        val args = listOf(
            file.path,
            "script",
            getGameName(),
            "" + family,
            "" + genus,
            "" + species,
            "" + eventNumber,
            fileFlag,
            caosOrFile
        ).toTypedArray()

        return try {
            process(project, fileName, descriptor, args)
        } catch (e: Exception) {
            throw e
        } finally {
            if (cliFlag == EVENT_FILE) {
                try {
                    val tempFile = File(caosOrFile)
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    LOGGER.severe("Failed to delete temp CAOS file")
                }
            }
        }
    }

    private fun getGameName(): String {
        data.gameName?.let {
            return@let it
        }
        val variant = variant
        if (variant == CaosVariant.UNKNOWN || variant == CaosVariant.ANY) {
            throw CaosInjectorException("Cannot inject CAOS without concrete variant")
        }
        return variant.code
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
        var tempFile: File? = null
        var tempDir = tempDir

        val exists = try {
            tempDir.exists()
        } catch (e: Exception) {
            LOGGER.severe("Failed to check if temp file $tempDir exists")
            e.printStackTrace()
            false
        }
        if (!exists) {
            tempDir = File(prefix, "drive_c/Program Files/CaosInjector")
            val newExists = try {
                tempDir.exists()
            } catch (e: Exception) {
                LOGGER.severe("Failed to check if alternate injector directory exists")
                e.printStackTrace()
                false
            }
            if (!newExists) {
                tempDir.mkdirs()
            }
        }
        while (tempFile == null) {
            val name = psuedoRandomUUID().apply {
                Log.iIf(DEBUG_INJECTOR) { "Temp CAOS file name = $this" }
            }
            val file = File(tempDir, name)
            try {
                if (file.exists()) {
                    continue
                }
            } catch (_: Exception) {
                continue
            }
            tempFile = file
        }
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


    private fun prepareCaos(caos: String, macro: Boolean): Pair<String, CLIInjectFlag?> {
        // Ensure that the exe has been extracted and placed in accessible folder
        val length = caos.length
        val base64Length = length * 1.4
        val cliFlag: CLIInjectFlag = when {
            base64Length < MAX_CONSOLE_LENGTH -> if (macro) CAOS_TEXT else EVENT_TEXT
            length < MAX_CAOS_FILE_LENGTH -> if (macro) CAOS_FILE else EVENT_FILE
            else -> return Pair(
                CaosBundle.message("caos.injector.errors.individual-script-too-long", MAX_CAOS_FILE_LENGTH, length),
                null
            )
        }
        try {
            assertExeWasCopied()
        } catch (e: Exception) {
            return Pair(e.message ?: CaosBundle.message("caos.injector.failed-to-copy-executable"), null)
        }
        val caosOrFile = try {
            prepareCaos(caos, cliFlag)
        } catch (e: Exception) {
            return Pair(
                e.message ?: CaosBundle.message("caos.injector.errors.failed-to-prepare-script", "script"),
                null
            )
        }
        return Pair(caosOrFile, cliFlag)
    }

    private fun prepareCaos(caos: String, cliFlag: CLIInjectFlag): String {
        if (cliFlag == CAOS_TEXT || cliFlag == EVENT_TEXT) {
            return InjectorHelper.escape(caos)
        }
        val tempFile = try {
            writeTempFile(caos)
        } catch (e: Exception) {
            throw Exception(e.message ?: "Failed to write CAOS to temp file for inject")
        }
        return tempFile.path
    }

    private fun process(project: Project, fileName: String, descriptor: String?, args: Array<String>): InjectionStatus {
        // Ensure prefix
        if (data.prefix.nullIfEmpty() == null) {
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.wine.prefix-cannot-be-null"),
                variant
            )
        }


        // Try to get the absolute path to the wine executable
        val wineExec = runBlocking { getWineExecutable(project) }
            ?: return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.wine.injector-not-found"),
                variant
            )

        // Build process runner
        val processBuilder = ProcessBuilder(wineExec, *args)
        val env = processBuilder.environment()
        env["WINEPREFIX"] = data.prefix
//        env["WINEDEBUG"] = "-all"

        val argsString = args.joinToString(" ") {
            if (it.contains(' ')) {
                "\"$it\""
            } else {
                it
            }
        }

        // Create injection process
        val proc: Process
        try {
            proc = processBuilder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            LOGGER.severe(
                "Failed to run command:\n\tWINEDEBUG=-all WINEPREFIX=\"${data.prefix.escape()}\" \"$wineExec\" $argsString"
            )
            return InjectionStatus.BadConnection(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.failed-to-run-executable", e.messageOrNoneText()),
                variant
            )
        }

        LOGGER.info("Command = WINEDEBUG=-all WINEPREFIX=\"${data.prefix.escape()}\" \"$wineExec\" $argsString")

        // Parse result
        return try {
            proc.waitFor()
            if (proc.exitValue() != 0) {
                LOGGER.severe("Process exited non-zero <${proc.exitValue()}>")
                try {
                    val error = proc.errorStream?.bufferedReader()?.readText()?.trim()?.nullIfEmpty()

                    LOGGER.severe(
                        "Failed to run command:\n\tWINEDEBUG=-all WINEPREFIX=\"${data.prefix.escape()}\" \"$wineExec\" $argsString\nSTDERR: $error"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    InjectionStatus.Bad(
                        fileName,
                        descriptor,
                        CaosBundle.message("caos.injector.errors.injector-crashed-or-failed", e.messageOrNoneText())
                    )
                }
            }
            getResponseFromProcess(fileName, descriptor, proc)
        } catch (e: Exception) {
            LOGGER.severe("Caos injection failed with error: " + e.message)
            e.printStackTrace()
            InjectionStatus.Bad(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.response-parse-failed", e.messageOrNoneText())
            )
        }
    }

    private fun getResponseFromProcess(fileName: String, descriptor: String?, proc: Process): InjectionStatus {
        val responseString = proc.inputStream
            ?.readAllBytes()
            ?.decodeToWindowsCP1252()
            .nullIfEmpty()
            ?: return InjectionStatus.Bad(fileName, descriptor, CaosBundle.message("caos.injector.errors.no-response"))
        return formatResponse(fileName, descriptor, responseString)
    }

    private fun formatResponse(fileName: String, descriptor: String?, responseString: String): InjectionStatus {
        val stripped = responseString.replace(
            "^\\s*wine:[^\n]*\n*".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            ""
        ).trim()
        val response = try {
            Json.decodeFromString(WineResult.serializer(), stripped)
        } catch (e: Exception) {
            LOGGER.severe("Malformed WINE response <$responseString> as <$stripped>")
            return InjectionStatus.Bad(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.json-invalid", responseString)
            )
        }
        return formatResponse(fileName, descriptor, response)
    }

    private suspend fun getWineExecutable(project: Project): String? {

        var executable: String? = data.wineExecutable
        if (executable != null && File(executable).exists()) {
            return executable
        }

        executable = WineHelper.getDefault(isWin32, false)

        if (executable != null && File(executable).exists()) {
            return executable
        }

        val prompt = if (isWin32) {
            "Select Wine 32 Executable"
        } else {
            "Select Wine 64 Executable"
        }

        val unixPath = if (Platform.isMacOs()) {
            "/usr/local/bin/wine"
        } else {
            "/usr/local/share/wine"
        }
        val startPath = if (executable != null) {
            try {
                File(executable).parent
            } catch (e: Exception) {
                unixPath
            }
        } else {
            unixPath
        }

        return suspendCoroutine { cont ->
            WineExecutableSelector.create(
                project,
                startPath,
                isWin32,
            ) { path ->
                if (path == null) {
                    cont.resume(null)
                    return@create
                }
                val settings = CaosInjectorApplicationSettingsService.getInstance()
                settings.gameInterfaceNames = settings.gameInterfaceNames.replaceAllInstances(
                    data.copy(wineExecutable = path)
                ) { it == data }

                if (isWin32) {
                    settings.wine32Path = path
                } else {
                    settings.wine64Path = path
                }
                if (settings.winePath == null) {
                    settings.winePath = path
                }
                cont.resume(path)
            }
        }
    }

    private fun formatResponse(fileName: String, descriptor: String?, response: WineResult): InjectionStatus {
        val code = response.status
        val message = response.message
        return when (code) {
            "!CMD" -> InjectionStatus.BadConnection(
                fileName,
                descriptor,
                CaosBundle.message("caos.injector.errors.internal-plugin-error", response.details ?: "<none>"),
                variant
            )

            "!CON" -> InjectionStatus.BadConnection(
                fileName,
                descriptor,
                message ?: CaosBundle.message("caos.injector.errors.failed-to-connect", variant.code),
                variant
            )

            "!ERR" -> InjectionStatus.Bad(
                fileName,
                descriptor,
                message ?: CaosBundle.message("caos.errors.unknown-error")
            )

            else -> {
                if (message == null) {
                    return InjectionStatus.Ok(fileName, descriptor, "")
                }

                if (InjectorHelper.isErrorResponse(message)) {
                    InjectionStatus.Bad(fileName, descriptor, message)
                } else {
                    // Allow for injectors other than mine. Return OK and response just in case
                    InjectionStatus.Ok(fileName, descriptor, message)
                }
            }
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
            CaosBundle.message("caos.injector.attribution.wine"),
            NotificationType.INFORMATION
        )
    }

    /**
     * Ensures that the bundled exe is extracted to accessible location to be run
     */
    private fun ensureExe(clear: Boolean): File {
        val pathTemp = "injector/$exeName"
        // have to use a stream
        val inputStream: InputStream = try {
            javaClass.classLoader.getResourceAsStream(pathTemp)
                ?: throw Exception("Failed to get injector EXE resource as stream")
        } catch (e: Exception) {
            throw Exception("Failed to get injector EXE resource as stream. Error: ${e.message}")
        }
        // always write to different location
        if (!prefix.exists()) {
            throw IOException("Wine prefix <${data.prefix}> is invalid")
        }
        var jectDir = File(tempDir, exeName)
        if (!jectDir.exists()) {
            jectDir = File(prefix, "drive_c/Program Files/CaosInjector")
            if (!jectDir.exists()) {
                jectDir.mkdir()
            }
        }
        val fileOut = File(jectDir, exeName)
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

    companion object {

        private var ranOnce = false
        private const val MAX_CONSOLE_LENGTH: Int = InjectorHelper.MAX_CONSOLE_LENGTH
        private const val MAX_CAOS_FILE_LENGTH = InjectorHelper.MAX_CAOS_FILE_LENGTH

        private var wineExecFallback: String? = null

        private fun askForWineExec(onEnter: (found: Boolean) -> Unit) {

        }
    }


}

@Serializable
internal data class WineResult(
    val status: String,
    val message: String?,
    val details: String?,
)


private fun which(name: String): String? {
    val proc = try {
        ProcessBuilder("type", "-a", name)
            .start()
    } catch (e: Exception) {
        LOGGER.warning("Failed to find wine through which; ${e.className}: ${e.message}")
        return null
    }
    val returnCode = proc.waitFor()
    if (returnCode != 0) {
        val error = try {
            proc.errorStream?.readAllBytes()?.decodeToString() ?: "<NONE>"
        } catch (e: Exception) {
            "${e.className}: ${e.message}"
        }
        LOGGER.severe("PROC call for which returned error code: $returnCode; Error: $error")

    }
    return try {
        val result = proc.inputStream
            .readAllBytes()
            .decodeToString()
            .trim()
            .nullIfEmpty()
        result?.split(" is ", limit = 2)
            ?.getOrNull(1)
            ?.trim()
            .also {
                LOGGER.info("Path to wine: $it")
            }
    } catch (e: Exception) {
        Log.eIf(DEBUG_INJECTOR) {
            "Failed to find $name exec with WHICH in bash. ${e.message ?: ""}\n${e.stackTraceToString()}"
        }
        null
    }
}

private val wineResolvedWithWhich by lazy {

    which("wine")
        ?: which("wine32on64")
        ?: which("wine32")
        ?: which("wine64")
}

private val knownBinLocations by lazy {
    listOf(
        "/usr/bin",
        "/usr/local/bin",
        "/usr/local/share",
        "/opt/wine-stable/bin",
        "/opt/homebrew/bin"
    )
}

private val wineX86InKnownLocations by lazy {
    knownBinLocations.filter { folder ->
        File(folder.ensureEndsWith('/') + "wine").exists()
    } + knownBinLocations.filter { folder ->
        File(folder.ensureEndsWith('/') + "wine32on64").exists()
    } + knownBinLocations.filter { folder ->
        File(folder.ensureEndsWith('/') + "wine32").exists()
    }
}

private val wineX64InKnownLocations by lazy {
    knownBinLocations.filter { folder ->
        File(folder.ensureEndsWith('/') + "wine64").exists()
    }.nullIfEmpty() ?: knownBinLocations.filter { folder ->
        File(folder.ensureEndsWith('/') + "wine").exists()
    }
}

private fun String.unsafeWineShellEscape(): String {
    val out = this.stripSurroundingQuotes(false)
        .escape()
        .replace("$", "\\$")
    return if (out.contains(' ')) {
        "\"$out\"".escape()
    } else {
        out.escape()
    }
}

object WineHelper {
    @JvmStatic
    fun getDefault(isWin32: Boolean, nullIfNotExists: Boolean): String? {

        val applicationSettings = CaosInjectorApplicationSettingsService
            .getInstance()
       var executable = if (isWin32) {
            applicationSettings.wine32Path ?: applicationSettings.winePath
        } else {
            applicationSettings.wine64Path ?: applicationSettings.winePath
        }
        if (executable != null && File(executable).exists()) {
            return executable
        }

        executable = wineResolvedWithWhich

        if (executable != null && File(executable).exists()) {
            return executable
        }

        executable = if (isWin32) {
            wineX86InKnownLocations.firstOrNull()
        } else {
            wineX64InKnownLocations.firstOrNull()
        }

        if (executable != null && (!nullIfNotExists || File(executable).exists())) {
            return executable
        }
        return null
    }
}
