package com.badahori.creatures.plugins.intellij.agenteering.injector

import bedalton.creatures.bytes.decodeToCreaturesEncoding
import bedalton.creatures.bytes.toBase64
import bedalton.creatures.util.Log
import bedalton.creatures.util.iIf
import bedalton.creatures.util.psuedoRandomUUID
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.injector.CLIInjectFlag.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.charset.Charset

/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class WineConnection(theVariant: CaosVariant? = null, private val data: WineInjectorInterface) : CaosConnection {
    override val supportsJect: Boolean
        get() = false
    private val variant by lazy {
        theVariant ?: data.variant
    }
    private val exeName = "ject.exe"
    private val file: File by lazy {
        val file = ensureExe(!ranOnce)
        ranOnce = true
        file
    }
    val prefix = File(data.prefix)

    private val tempDir by lazy {
        File(prefix, "windows/temp")
    }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
//        Caos
//        val fileName = copyForJect(variant, data, caos.text)
        return InjectionStatus.ActionNotSupported("JECT is not implemented")
    }


    /**
     * Injects raw CAOS using the Windows CLI injector
     */
    override fun inject(caos: String): InjectionStatus {
        val parts = prepareCaos(caos, true)
        val caosOrFile = parts.first
        val cliTypeFlag = parts.second
            ?: return InjectionStatus.BadConnection(parts.first)
        val flag = if (cliTypeFlag == CAOS_FILE || cliTypeFlag == EVENT_FILE) "-f" else "-b"
        // Create cmd args for caos injector exe
        val args = listOf(
            "wine",
            file.path,
            "macro",
            getGameName(),
            flag,
            caosOrFile
        ).toTypedArray()
        return try {
            process(args)
        } catch (e: Exception) {
            throw e
        } finally {
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

    /**
     * Inject caos code into CV+ games
     */
    override fun injectEventScript(
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
    ): InjectionStatus {
        val parts = prepareCaos(caos, macro = false)
        val caosOrFile = parts.first
        val cliFlag = parts.second
            ?: return InjectionStatus.BadConnection(parts.first)

        // If not event file, it is base 64 encoded
        val fileFlag = if (cliFlag == EVENT_FILE) "-f" else "-b"

        // Create cmd args for caos injector exe
        val args = listOf(
            "wine",
            file.path,
            "script",
            getGameName(),
            "" + family,
            "" + genus,
            "" + species,
            fileFlag,
            caosOrFile
        ).toTypedArray()

        return try {
            process(args)
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
        if (variant == null || variant == CaosVariant.UNKNOWN || variant == CaosVariant.ANY) {
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
        if (!tempDir.exists()) {
            tempDir = File(prefix, "drive_c/Program Files/CaosInjector")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
        }
        while (tempFile == null) {
            val file = File(tempDir, psuedoRandomUUID())
            if (file.exists()) {
                continue
            }
            tempFile = file
        }
        try {
            tempFile.writeText(caos, Charsets.UTF_8)
            return tempFile
        } catch (e: Exception) {
            try {
                if (tempFile.exists())
                    tempFile.delete()
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
                "Script is too long. Plugin max length for injection is $MAX_CAOS_FILE_LENGTH; Actual: $length",
                null
            )
        }
        try {
            assertExeWasCopied()
        } catch (e: Exception) {
            return Pair(e.message ?: "Failed to copy injector executable", null)
        }
        val caosOrFile = try {
            prepareCaos(caos, cliFlag)
        } catch (e: Exception) {
            return Pair(e.message ?: "Failed to prepare event script for injection", null)
        }
        return Pair(caosOrFile, cliFlag)
    }

    private fun prepareCaos(caos: String, cliFlag: CLIInjectFlag): String {
        if (cliFlag == CAOS_TEXT || cliFlag == EVENT_TEXT) {
            return escape(caos)
        }
        val tempFile = try {
            writeTempFile(caos)
        } catch (e: Exception) {
            throw Exception(e.message ?: "Failed to write CAOS to temp file for inject")
        }
        return tempFile.path
    }

    private fun process(args: Array<String>): InjectionStatus {
        val processBuilder = ProcessBuilder(*args)
        val env = processBuilder.environment();
        env["WINEPREFIX"] = data.prefix
        env["WINEDEBUG"] = "-all"

        Log.iIf(DEBUG_INJECTOR) { "COMMAND: WINEPREFIX=\"${data.prefix}\" ${args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it } }" }
        // Create injection process
        val proc: Process
        try {
            proc = processBuilder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            LOGGER.severe("Failed to run command: WINEPREFIX=\"${data.prefix}\" ${args.joinToString(" ") { if (it.contains(' ')) "\"$it\"" else it }}")
            return InjectionStatus.BadConnection("Failed to run executable with error: ${e.message};")
        }

        // Parse result
        return try {
            proc.waitFor()
            if (proc.exitValue() != 0) {
                LOGGER.severe("Process exited non-zero <${proc.exitValue()}>")
                try {
                    val error = proc.errorStream?.bufferedReader()?.readText()?.trim()?.nullIfEmpty()

                    if (error != null) {
                        return if (error.startsWith('{')) {
                            formatResponse(error)
                        } else {
                            InjectionStatus.Bad(error)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    InjectionStatus.Bad("CAOS injector crashed or failed. ${e.message}")
                }
            }
            getResponseFromProcess(proc)
        } catch (e: Exception) {
            LOGGER.severe("Caos injection failed with error: " + e.message)
            e.printStackTrace()
            InjectionStatus.Bad("Plugin Error: Response parsing failed with error: " + e.message)
        }
    }

    private fun getResponseFromProcess(proc: Process): InjectionStatus {
        val responseString = proc.inputStream
            ?.readAllBytes()
            ?.decodeToCreaturesEncoding()
            .nullIfEmpty()
            ?: return InjectionStatus.Bad("Injector returned no response").apply {
                LOGGER.severe("Process returned no response")
            }
        return formatResponse(responseString)
    }

    private fun formatResponse(responseString: String): InjectionStatus {
        val stripped = responseString.replace("^\\s*wine:[^\n]*\n*".toRegex(setOf(RegexOption.IGNORE_CASE,RegexOption.MULTILINE)), "").trim()
        val response = try {
            Json.decodeFromString(WineResult.serializer(), stripped)
        } catch (e: Exception) {
            LOGGER.severe("Malformed WINE response <$responseString> as <$stripped>")
            return InjectionStatus.Bad("Invalid response returned from injector")
        }
        return formatResponse(response)
    }

    private fun formatResponse(response: WineResult): InjectionStatus {
        val code = response.status
        val message = response.message
        return when (code) {
            "!CMD" -> InjectionStatus.BadConnection("Internal plugin run error. " + (response.details ?: ""))
            "!CON" -> InjectionStatus.BadConnection(message ?: "Error connection to game")
            "!ERR" -> InjectionStatus.Bad(message ?: "Unknown error occurred")
            else -> {
                if (message == null) {
                    return InjectionStatus.Ok("")
                }
                val responseLower = message.lowercase()
                if (message.contains("{@}") || errorPrefix.any { responseLower.startsWith(it) } || errorMessageRegex.any {
                        it.matches(message)
                    }) {
                    InjectionStatus.Bad(message)
                } else {
                    // Allow for injectors other than mine. Return OK and response just in case
                    InjectionStatus.Ok(message)
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
            "Wine CAOS injector is based heavily on Chris Double's work as documented @ http://double.nz/creatures\n\t-Plugin utilizes both his DDE code(C1e) as well as his Memory Map code(C2e)",
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
        private const val MAX_CONSOLE_LENGTH: Int = 7590
        private const val MAX_CAOS_FILE_LENGTH = 14100

        @Suppress("SpellCheckingInspection")
        private val errorPrefix = listOf(

            "invalid command",
            "invalid rvalue",
            "invalid lvalue",
            "invalid subcommand",
            "invalid string rvalue",
            "invalid string lvalue",
            "blocking command executed on a non-blockable script",
            "invalid targ",
            "invalid ownr",
            "invalid agent",
            "not a compoundagent",
            "not a simpleagent",
            "division by zero",
            "invalid port id",
            "value is not an integer",
            "value is not a float",
            "incompatible type: string expected",
            "incompatible type: agent expected",
            "path number out of range",
            "not a vehicle",
            "attr/perm change caused invalid map position",
            "invalid map position", "invalid agent id",
            "invalid compare operator for agents",
            "incompatible type: decimal expected",
            "internal: unexpected type when fetching value",
            "chemical numbers range from (0-255)",
            "not a creature",
            "index number outside string",
            "value is not a character (integer 0-255)",
            "slice attempted outside string",
            "failed to delete room",
            "failed to delete meta room",
            "failed to add background",
            "failed to get meta room location",
            "failed to set background",
            "failed to set door permiability",
            "failed to set room type",
            "failed to set room property",
            "failed to set cellular automata rates",
            "failed to get room id",
            "failed to increase ca property or ca is navigable - altr does not work on navigable cas",
            "failed to get door permiability",
            "failed to add meta room",
            "failed to add room",
            "attempt to clear a photograph in the creature history which is currently in use by an agent.",
            "failed to get room property",
            "failed to get current background",
            "failed to find room with highest ca",
            "failed to find room with lowest ca",
            "could not set neuron value",
            "could not set dendrite value",
            "could not set lobe value",
            "could not set tract value",
            "could not dump lobe",
            "could not dump tract",
            "could not dump neuron",
            "could not dump dendrite",
            "invalid creature targ",
            "invalid parameter to dbg#/dbga ",
            "recycle me",
            "invalid pose for pupt/puhl",
            "invalid string for anms - not a number", "number out of range in anms. values must be from 0 to 255",
            "error parsing caos script-command string",
            "error orderising macro for caos script-command string",
            "error processing script for scriptorium in caos script-command string",
            "error installing script into scriptorium for caos script-command string",
            "failed to set link permiability",
            "mutation parameters must be in the range of 0 to 255",
            "failed to get room ca rates",
            "failed to get room ids",
            "failed to get room location",
            "failed to get metaroom backgrounds",
            "not a fixed or text entry part",
            "not a text entry part",
            "repeat count is less than one",
            "invalid locus",
            "no such drive number",
            "no such gait number",
            "no such direction number",
            "no such involuntary action id",
            "operation not allowed on creature",
            "not a graph part",
            "request out of range in integerrv clik, range is 0 to 3",
            "agent profiler not compiled in this engine",
            "invalid gene variable",
            "maximum creatures exceeded",
            "gene file not found or attempt to load into invalid slot",
            "attempt to use uninitialised output stream",
            "creature can't be born again",
            "life event doesn't exist for this moniker",
            "attempt to use uninitialised input stream",
            "you can only clear history for totally dead or exported creatures, and unused genomes.\nthat means ooww return values 0, 6, 4.or 7.",
            "that part slot is already taken. you may only create a part if it has a unique number. please change the number or else kill the old part first.",
            "failed to find one or more sprite files while creating a creature.",
            "user aborted possibly frozen script",
            "assertion failed",
            "invalid range",
            "pray builder error: .*",
            "base change failed - new base",
            "pose change failed - new pose",
            "anim change failed - on part",
            "anim by string failed - string",
            "invalid emit - invalid smell index",
            "invalid cacl - wildcard classifier used or the classifier already has a smell allocated to it",
            "negative square root",
            "mvsf only works on autonomous agents",
            "failed to find safe location",
            "tried to set bhvr %d when the agent doesn't have one of the appropriate scripts",
            "sound file missing",
            "an agent script is possibly in an infinite loop.\nchoose abort to throw an error, so you can see which agent it is and stop it\nchoose retry to let it run for another 1,000,000 instructions\nchoose ignore to let it carry on forever",
            "syntax error",
            "invalid command",
            "failed to close block",
            "expected a string",
            "expected a subcommand",
            "expected a variable",
            "expected numeric rvalue",
            "expected agent",
            "expected a comparison operator",
            "label expected",
            "already at top level",
            "mismatched block type",
            "next without matching enum/esee/etch/epas",
            "untl or ever without matching loop",
            "repe without matching reps",
            "label already defined",
            "expected byte string '\\['",
            "value out of valid range (0..255)",
            "expected any rvalue",
            "expected byte or '\\]'"
        )

        private const val NUMBER_REGEX = "[+-]?\\d+|[+-]?\\d*\\.\\d+"

        /**
         * Creates error regex expressions to test for error injections
         * May be annoying if false positives
         */
        private val errorMessageRegex by lazy {
            listOf(
                "SCRX failed - script \\(%d/%d/%d/%d\\) is in use",
                "Script \\(%d %d %d %d\\) not in scriptorium",
                "Part identifier %d is invalid",
                "Position \\(%f, %f\\) is not valid inside carrying vehicle's cabin",
                "%s at token '%s'",
                "Unresolved label \"%s\"",
                "ATTR/PERM change caused invalid map position \\(%f\\)",
                "Invalid map position \\(%f, %f\\)",
                "Invalid string for ANMS - not a number\n\"%s\"",
                "Pray Builder error: %s",
                "Base change failed - new base %d on part %d",
                "Pose change failed - new pose %d on part %d which has base %d",
                "Anim change failed - on part %d which has base %d",
                "Frame rate %d out of range 1 to 255",
                "Anim by string failed - string \"%s\" on part %d which has base %d",
                "Failed to find safe location \\(%f, %f\\)",
                "Tried to set BHVR %d when the agent doesn't have one of the appropriate scripts",
                "Sound file missing\n%s"
            ).map { errorMessage ->
                (".*$errorMessage.*").replace("%[df]", NUMBER_REGEX).replace("%s", "(.+?)").toRegex(RegexOption.IGNORE_CASE)
            }
        }


        private fun escape(caos: String): String {
            var escaped = caos.trim()
            if (escaped.lowercase().endsWith("endm")) {
                // Remove last endm, as injection requires its removal on Windows
                if (OsUtil.isWindows) {
                    escaped = escaped.substringFromEnd(0, 4).trim()
                }
            } else if (OsUtil.isLinux) {
                // lc2e requires endm according to pyc2e
                escaped += " endm"
            }

            return ByteArrayOutputStream().use { osByteArray ->
                OutputStreamWriter(osByteArray, Charsets.UTF_8).use { w ->
                    w.write(escaped)
                }
                osByteArray.toByteArray().toBase64()
            }
        }
    }

}

@Serializable
internal data class WineResult(
    val status: String,
    val message: String?,
    val details: String?
)