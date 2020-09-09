package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.substringFromEnd
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

internal class C3Connection(private val variant: CaosVariant) : CaosConnection {

    private var ranOnce = false
    private val exeName = "C3CaosInjector.exe"
    private val file: File by lazy {
        val file = ensureExe(!ranOnce)
        ranOnce = true
        file
    }


    override fun inject(caos:String) : InjectionStatus {
        return inject("-c", caos)
    }

    override fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber:Int, caos: String) : InjectionStatus {
        return inject("-s", "$family", "$genus", "$species", "$eventNumber", caos)
    }

    /**
     * Inject caos code into CV+ games
     */
    private fun inject(vararg argsIn:String): InjectionStatus {
        // Ensure that the exe has been extracted and placed in accessible folder
        val file = try {
            this.file
        } catch (e: Exception) {
            return InjectionStatus.BadConnection("Failed to locate injector exe in plugin library.")
        }
        if (!file.exists()) {
            return InjectionStatus.BadConnection("Failed to initialize communication executable")
        }

        // Escape string
        val escaped = escape(argsIn.last() as String)
        if (escaped.isEmpty()) {
            return InjectionStatus.Ok("")
        }
        // Create cmd args for caos injector exe
        val args = listOf(
                "cmd",
                "/c",
                file.path,
                variant.code,
                *argsIn.dropLast(1).toTypedArray(),
                escaped
        ).toTypedArray()

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
            proc.waitFor(6, TimeUnit.SECONDS)
            val response = proc.inputStream.bufferedReader().readText().substringFromEnd(0, 1).trim().nullIfEmpty()
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
                    if (response.contains("{@}") && errorPrefix.none { response.startsWith(it) } && errorMessageRegex.none { it.matches(response) }) {
                        InjectionStatus.Bad(response.substringFromEnd(if (response.startsWith("!RES")) 4 else 0, 1))
                    } else if (code == "!RES") {
                        InjectionStatus.Ok(response.substringFromEnd(4, 1))
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
     * Disconnect does nothing as there is no persistant connection
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
        val out: OutputStream = FileUtils.openOutputStream(fileOut)
        IOUtils.copy(inputStream, out)
        inputStream.close()
        out.close()
        return fileOut
    }

    companion object {

        private val errorPrefix = listOf(

                "Invalid command",
                "Invalid RValue",
                "Invalid LValue",
                "Invalid subcommand",
                "Invalid string RValue",
                "Invalid string LValue",
                "Blocking command executed on a non-blockable script",
                "Invalid TARG",
                "Invalid OWNR",
                "Invalid agent",
                "Not a CompoundAgent",
                "Not a SimpleAgent",
                "Division by Zero",
                "Invalid port ID",
                "Value is not an integer",
                "Value is not a float",
                "Incompatible type: string expected",
                "Incompatible type: agent expected",
                "Path number out of range",
                "Not a Vehicle",
                "ATTR/PERM change caused invalid map position",
                "Invalid map position", "Invalid agent ID",
                "Invalid compare operator for agents",
                "Incompatible type: decimal expected",
                "Internal: Unexpected type when fetching value",
                "Chemical numbers range from (0-255)",
                "Not a Creature",
                "Index number outside string",
                "Value is not a character (integer 0-255)",
                "Slice attempted outside string",
                "Failed to delete room",
                "Failed to delete meta room",
                "Failed to add background",
                "Failed to get meta room location",
                "Failed to set background",
                "Failed to set door permiability",
                "Failed to set room type",
                "Failed to set room property",
                "Failed to set Cellular Automata rates",
                "Failed to get room ID",
                "Failed to increase CA property or CA is navigable - ALTR does not work on navigable CAs",
                "Failed to get door permiability",
                "Failed to add meta room",
                "Failed to add room",
                "Attempt to clear a photograph in the creature history which is currently in use by an agent.",
                "Failed to get room property",
                "Failed to get current background",
                "Failed to find room with highest CA",
                "Failed to find room with lowest CA",
                "Could not set neuron value",
                "Could not set dendrite value",
                "Could not set lobe value",
                "Could not set tract value",
                "Could not dump lobe",
                "Could not dump tract",
                "Could not dump neuron",
                "Could not dump dendrite",
                "Invalid Creature TARG",
                "Invalid parameter to DBG#/DBGA ",
                "RECYCLE ME",
                "Invalid pose for PUPT/PUHL",
                "Invalid string for ANMS - not a number", "Number out of range in ANMS. Values must be from 0 to 255",
                "Error parsing CAOS script-command string",
                "Error orderising macro for CAOS script-command string",
                "Error processing script for scriptorium in CAOS script-command string",
                "Error installing script into scriptorium for CAOS script-command string",
                "Failed to set link permiability",
                "Failed to get link permiability",
                "Mutation parameters must be in the range of 0 to 255",
                "Failed to get room CA Rates",
                "Failed to get room IDs",
                "Failed to get room location",
                "Failed to get metaroom backgrounds",
                "Not a fixed or text entry part",
                "Not a text entry part",
                "Repeat count is less than one",
                "Invalid locus",
                "No such drive number",
                "No such gait number",
                "No such direction number",
                "No such involuntary action id",
                "Operation not allowed on Creature",
                "Not a graph part",
                "Request out of range in IntegerRV CLIK, Range is 0 to 3",
                "Agent profiler not compiled in this engine",
                "Invalid gene variable",
                "Maximum creatures exceeded",
                "Gene file not found or attempt to load into invalid slot",
                "Attempt to use uninitialised output stream",
                "Creature can't be born again",
                "Life event doesn't exist for this moniker",
                "Attempt to use uninitialised input stream",
                "You can only clear history for totally dead or exported creatures, and unused genomes.\nThat means OOWW return values 0, 6, 4.or 7.",
                "That part slot is already taken. You may only create a part if it has a unique number. Please change the number or else kill the old part first.",
                "Failed to find one or more sprite files while creating a creature.",
                "User aborted possibly frozen script",
                "Assertion failed",
                "Invalid range",
                "Pray Builder error: .*",
                "Base change failed - new base",
                "Pose change failed - new pose",
                "Anim change failed - on part",
                "Frame rate %d out of range 1 to 255",
                "Anim by string failed - string",
                "Invalid emit - invalid smell index",
                "Invalid cacl - wildcard classifier used or the classifier already has a smell allocated to it",
                "Negative square root",
                "MVSF only works on autonomous agents",
                "Failed to find safe location",
                "Tried to set BHVR %d when the agent doesn't have one of the appropriate scripts",
                "Sound file missing",
                "An agent script is possibly in an infinite loop.\nChoose Abort to throw an error, so you can see which agent it is and stop it\nChoose Retry to let it run for another 1,000,000 instructions\nChoose Ignore to let it carry on forever",
                "Syntax error",
                "Invalid command",
                "Failed to close block",
                "Expected a string",
                "Expected a subcommand",
                "Expected a variable",
                "Expected numeric rvalue",
                "Expected agent",
                "Expected a comparison operator",
                "Label expected",
                "Already at top level",
                "Mismatched block type",
                "NEXT without matching ENUM/ESEE/ETCH/EPAS",
                "UNTL or EVER without matching LOOP",
                "REPE without matching REPS",
                "Label already defined",
                "Expected byte string '\\['",
                "Value out of valid range (0..255)",
                "Expected any rvalue",
                "Expected byte or '\\]'"
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
                (it.replace("%[df]".toRegex(), replacement) + "(.*)").toRegex()
            }
        }

        private fun escape(caos: String): String {
            var escaped = caos.trim()
            // Remove last endm, as injection requires its removal
            if (escaped.toLowerCase().endsWith("endm")) {
                escaped = escaped.substringFromEnd(0, 4).trim()
            }
            // Escape escaped chars
            escaped = escaped.replace("\"", "\\\"").replace("\n", "\\\n")
            // Return
            return escaped
        }
    }

}