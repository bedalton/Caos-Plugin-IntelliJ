package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil.isWindows
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.substringFromEnd
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class C3Connection(private val variant: CaosVariant) : CaosConnection {

    private var tempFileIndex = 1
    private var ranOnce = false
    private val exeName = "C3CaosInjector.exe"
    private val libName = "c2einjector"
    private val file: File by lazy {
        val file = if (isWindows) {
             ensureExe(!ranOnce)
        } else {
            ensureLib(!ranOnce)
        }
        ranOnce = true
        file
    }


    override fun inject(caos:String) : InjectionStatus {
        return if (caos.length > 7590) {
            writeCaosToFileAndInject(caos) {
                inject("-c", caos)
            }
        } else {
            return inject("-c", caos)
        }
    }

    override fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber:Int, caos: String) : InjectionStatus {
        return if (caos.length > 7590) {
            writeCaosToFileAndInject(caos) {
                inject("-s", "$family", "$genus", "$species", "$eventNumber", caos)
            }
        } else {
            inject("-s", "$family", "$genus", "$species", "$eventNumber", caos)
        }
    }

    private fun writeCaosToFileAndInject(caos:String, inject:(caos:String) -> InjectionStatus) : InjectionStatus {
        val tempFile = File.createTempFile("CAOS", "${tempFileIndex++}".padStart(3,'0'))
        try {
            tempFile.writeText(caos)
        } catch (e:Exception) {
            try {
                if (tempFile.exists())
                    tempFile.delete()
            } catch (e2:Exception) {
                LOGGER.severe("Failed to delete temp CAOS file")
                e.printStackTrace()
            }
            return InjectionStatus.Bad("Failed to write CAOS to temp file for injecting; CAOS too long for direct injection")
        }
        return try {
            inject(caos)
        } finally {
            if (tempFile.exists())
                tempFile.delete()
        }
    }

    /**
     * Inject caos code into CV+ games
     */
    private fun inject(vararg argsIn:String): InjectionStatus {
        // Ensure that the exe has been extracted and placed in accessible folder
        val file = try {
            this.file
        } catch (e: Exception) {
            return InjectionStatus.BadConnection("Failed to copy injector EXE to run directory. Error: ${e.message}.")
        }
        if (!file.exists()) {
            return InjectionStatus.BadConnection("Failed to initialize communication executable")
        }

        // Escape string
        val escaped = escape(argsIn.last())
        if (escaped.isEmpty()) {
            return InjectionStatus.Ok("")
        }

        LOGGER.severe("RAW:\n${argsIn.last()}\nEscaped:\n$escaped")
        // Create cmd args for caos injector exe
        val args = when {
            isWindows -> {
                listOf(
                    "cmd",
                    "/c",
                    file.path,
                    variant.code,
                    *argsIn.dropLast(1).toTypedArray(),
                    escaped
                ).toTypedArray()
            }
            OsUtil.isLinux -> {
                listOf(
                    "cmd",
                    "/c",
                    file.path,
                    variant.code,
                    *argsIn.dropLast(1).toTypedArray(),
                    escaped
                ).toTypedArray()
            }
            else -> {
                return InjectionStatus.BadConnection("Only windows and Linux versions of Creatures is supported")
            }
        }
        // Create injection process
        val proc: Process
        try {
            proc = Runtime.getRuntime().exec(args)
        } catch (e: Exception) {
            e.printStackTrace()
            return InjectionStatus.BadConnection("Failed to run executable with error: ${e.message};")
        }

        // Wait for process to finish
        try {
            proc.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            InjectionStatus.Bad("Injection process interrupted.")
        }

        // Parse result
        return try {
            proc.waitFor()
            var response = proc.inputStream.bufferedReader().readText().substringFromEnd(0, 1).trim().nullIfEmpty()
                    ?: proc.errorStream.bufferedReader().readText().nullIfEmpty()
                    ?: "!ERRInjector returned un-formatted empty response"
            val code = if (response.length >= 4) {
                response.substring(0, 4)
            } else
                response
            when (code) {
                "!CMD" -> InjectionStatus.BadConnection("Internal plugin run error. " + response.substring(4))
                "!CON" -> InjectionStatus.BadConnection(response.substring(4))
                "!ERR" -> InjectionStatus.Bad(response.substring(4))
                else -> {
                    val responseLower = response.toLowerCase()
                    if (response.contains("{@}"))
                        LOGGER.info("MIGHT be bad response: $response")
                    else
                        LOGGER.info("Is not bad response: $response")
                    if (response.contains("{@}") && errorPrefix.any { responseLower.startsWith(it) }) {
                        InjectionStatus.Bad(response.substringFromEnd(if (response.startsWith("!RES")) 4 else 0, 1))
                    } else if (code == "!RES") {
                        if (response.last() == 0.toChar()) {
                            response = response.substringFromEnd(if (response.startsWith("!RES")) 4 else 0, 1)
                        } else if (response.startsWith("!RES")) {
                            response = response.substring(4)
                        }
                        InjectionStatus.Ok(response)
                    } else {
                        InjectionStatus.Bad("INJECTOR Exception: "+response.substringFromEnd(0, 1))
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.severe("Caos injection failed with error: " + e.message)
            e.printStackTrace()
            InjectionStatus.Bad("Plugin Error: Response parsing failed with error: " + e.message)
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
        CaosInjectorNotifications.show(project, "Attribution", "${variant.code} caos injector uses modified version of CAOS-CLASS-LIBRARY @ https://github.com/AlbianWarp/Caos-Class-Library", NotificationType.INFORMATION)
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
            } else
                return fileOut
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
            LOGGER.info("Saved Injector EXE to '${fileOut.absolutePath}'")
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
        inputStream.use {stream ->
            CaosFileUtil.copyStreamToFile(stream, fileOut, true)
        }
        return fileOut
    }

    companion object {

        private const val MAX_CONSOLE_LENGTH:Int = 7590
        private const val ESCAPED_QUOTE_PLACEHOLDER = "/;__ESCAPED_QUOTE__/;"
        private val NEED_ESCAPE = listOf(
                "^",
                ">",
                "<",
                "&",
                "|"
        )
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
                "frame rate %d out of range 1 to 255",
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

        /**
         * Creates error regex expressions to test for error injections
         * May be annoying if false positives
         */
        private val errorMessageRegex by lazy {
            val replacement = "(\\\\d+(\\\\.\\\\d+)?|\\\\d*\\\\.\\\\d+)"
            listOf(
                    "SCRX failed - script (%d/%d/%d/%d) is in use",
                    "Script (%d %d %d %d) not in scriptorium",
                    "Part identifier %d is invalid",
                    "Position (%f, %f) is not valid inside carrying vehicle's cabin",
                    "(.*?) at token '([^']*?)'",
                    "Unresolved label \"([^\"]*)\""
            ).map {
                (it.replace("%[df]".toRegex(RegexOption.IGNORE_CASE), replacement) + "(.*)").toRegex(RegexOption.IGNORE_CASE)
            }
        }


        private fun escape(caos: String): String {
            var escaped = caos.trim()
            if (escaped.toLowerCase().endsWith("endm")) {
                // Remove last endm, as injection requires its removal on Windows
                if (isWindows)
                    escaped = escaped.substringFromEnd(0, 4).trim()

            } else if (OsUtil.isLinux) {
                // lc2e requires endm according to pyc2e
                escaped += " endm"
            }

            // Escape escaped chars
            escaped = escaped.replace("\\\"", ESCAPED_QUOTE_PLACEHOLDER)
                    .replace("\"", "\\\"")

            // Windows specific escaping
            if (isWindows) {
                for(char in NEED_ESCAPE) {
                    escaped = escaped.replace(char, "^$char")
                }
                escaped = escaped.replace("\r\n", ";\\;n")
                    .replace("\n", ";\\;n")
                    .replace("\r", ";\\;n")
            } else {
                // Unix escaping
                escaped = escaped.replace("\n", "\\n")
            }
            // Return
            return escaped
        }
    }


}