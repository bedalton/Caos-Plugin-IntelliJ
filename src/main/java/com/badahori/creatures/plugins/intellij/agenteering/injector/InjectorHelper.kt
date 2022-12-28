package com.badahori.creatures.plugins.intellij.agenteering.injector

import bedalton.creatures.common.bytes.toBase64
import com.badahori.creatures.plugins.intellij.agenteering.utils.OsUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.substringFromEnd
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

object InjectorHelper {

    internal const val MAX_CONSOLE_LENGTH: Int = 7590

    internal const val MAX_CAOS_FILE_LENGTH = 14100



    internal fun escape(caos: String): String {
        var escaped = caos.trim()
        if (escaped.lowercase().endsWith("endm")) {
            // Remove last endm, as injection requires its removal on Windows
            if (OsUtil.isWindows)
                escaped = escaped.substringFromEnd(0, 4).trim()

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

    internal fun isErrorResponse(response: String): Boolean {
        val responseLower = response.lowercase()
        if (response.contains("{@}")) {
            return true
        }
        if (errorPrefix.any { responseLower.startsWith(it) }) {
            return true
        }
        return errorMessageRegex.any {
            it.matches(
                response
            )
        }
    }


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
            errorMessage.replace("%[df]", NUMBER_REGEX).replace("%s", "(.+?)").toRegex(RegexOption.IGNORE_CASE)
        }
    }

}