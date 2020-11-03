package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.CaosLibException
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token


/**
 * Interface marking a class as having an get operator
 */
interface CommandGetter {
    operator fun get(tokenString: String, bias: CaosExpressionValueType = CaosExpressionValueType.ANY): CaosCommand?
}

/**
 * Makes a list of CaosCommands queryable by token
 */
private class CaosCommandMap(commands: Map<String, CaosCommand>, tokenIds: Map<String, Int>) : CommandGetter {
    private val map = mapCommands(commands, tokenIds)

    /**
     * Gets a command definition given a word token as int and a token stream
     * Token stream is required to read more tokens if need be
     * Bias is required for special processing of TRAN
     */
    override operator fun get(tokenString: String, bias: CaosExpressionValueType): CaosCommand? {
        val tokens = tokenString.toLowerCase().split("\\s+".toRegex())
        val token = token(tokens[0])
        val otherTokens = tokens.drop(1).map { token(it) }
        // Find list of sub tokens for a given token
        val commands = map[token]
            ?: return null

        // Find all commands matching sub tokens
        // Does not return first match, as TRAN as rvalue can return 2 matches
        val potential = commands.mapNotNull map@{
            if (it.first(otherTokens))
                it.second
            else
                null
        }

        // If multiple matches (ie. TRAN), return the one matching the BIAS
        return when (potential.size) {
            0 -> return null
            1 -> potential.first()
            else -> potential.firstOrNull { it.returnType == bias } ?: potential.first()
        }
    }
}

/**
 * Transforms a list of CAOS commands into a map for use in a CAOS command map object
 */
private fun mapCommands(
    commands: Map<String, CaosCommand>,
    tokenIds: Map<String, Int>
): Map<Int, List<Pair<CommandCheck, CaosCommand>>> {
    val commandGroups: MutableMap<Int, List<Pair<CommandCheck, CaosCommand>>> = mutableMapOf()
    for (keySet in tokenIds) {
        val parts = keySet.key
            .toLowerCase()
            .split(" ")
            .filter { it.isNotBlank() }
            .map {
                token(it)
            }
        val command = commands["${keySet.value}"]
            ?: throw CaosLibException("Command Token: $keySet does not match any command in set")
        val mappedCommand = mapCommand(parts.drop(1), command)
        if (parts.isEmpty())
            throw CaosLibException("Command ${command.command} yielded empty token list")
        commandGroups[parts[0]] = commandGroups[parts[0]]?.let { it + mappedCommand } ?: listOf(mappedCommand)
    }
    return commandGroups
}

/**
 * Maps a Command to a token checking function for use in a command map
 */
private fun mapCommand(subCommands: List<Int>, command: CaosCommand): Pair<CommandCheck, CaosCommand> {
    return if (subCommands.isEmpty())
        Pair(NoSubCommandsCheck, command)
    else
        Pair(CommandCheck(subCommands), command)
}

/**
 * Class holding a check to validate whether a list of tokens, matches this item
 */
private open class CommandCheck(private val subCommands: List<Int>) {
    /**
     * Checks a list of tokens against another.
     * If tokens match, return number in list
     * else return null for no matching tokens
     */
    open operator fun invoke(tokens: List<Int>): Boolean {
        for (i in tokens.indices) {
            if (tokens[i] != subCommands[i])
                return false
        }
        return true
    }
}

/**
 * Command check for single token commands.
 */
private object NoSubCommandsCheck : CommandCheck(emptyList()) {
    /**
     * Return 1 if token list is
     */
    override operator fun invoke(tokens: List<Int>): Boolean = tokens.isEmpty()
}

/**
 * A queryable CAOS lib object
 */
@Suppress("unused")
class CaosLib internal constructor(private val lib: CaosLibDefinitions, val variant: CaosVariantData) {

    /**
     * Rvalue definitions getter
     */
    val rvalue: CommandGetter = CaosCommandMap(lib.rvalues, variant.rvalues)

    /**
     * List of all RValues in this variant
     */
    val rvalues: List<CaosCommand> by lazy {
        lib.rvalues.values.filter {
            it.variants.contains(variantCode)
        }
    }

    /**
     * LValue definitions getter
     */
    val lvalue: CommandGetter = CaosCommandMap(lib.lvalues, variant.lvalues)


    /**
     * List of all LValues in this variant
     */
    val lvalues: List<CaosCommand> by lazy {
        lib.lvalues.values.filter {
            it.variants.contains(variantCode)
        }
    }

    /**
     * Command definitions getter
     */
    val command: CommandGetter = CaosCommandMap(lib.commands, variant.commands)

    /**
     * List of all commands in this variant
     */
    val commands: List<CaosCommand> by lazy {
        lib.commands.values.filter {
            it.variants.contains(variantCode)
        }
    }
/*
    /**
     * Values list getter
     */
    val valuesList: HasGetter<Int, CaosValuesList?> = CaosValuesListMap(lib.typeLists)


    /**
     * Values lists getter
     */
    val valuesLists: Collection<CaosValuesList>
        get() = lib.typeLists.values
*/
    /**
     * Getter for variable max value type
     */
    val variableTypeMaxValues: HasGetter<CaosScriptVarTokenGroup, Int?> = variant.vars


    // Variant query information
    /** Variant CODE */
    val variantCode: String = variant.code

    /** The full name of this variant */
    val variantName: String = variant.name

    /** Helper command to check variant oldness (ie. C1,C2) */
    val isOldVariant: Boolean
        get() = variant.isOld

    /** Helper command to check variant newness (ie. CV,C3,DS) */
    val isNewVariant: Boolean
        get() = variant.isNew

    internal operator fun get(type: CaosCommandType): CommandGetter {
        return when (type) {
            CaosCommandType.RVALUE -> rvalue
            CaosCommandType.LVALUE -> lvalue
            CaosCommandType.COMMAND -> command
            CaosCommandType.CONTROL_STATEMENT -> command
            CaosCommandType.UNDEFINED -> command
        }
    }
    operator fun get(type: CaosCommandType, token: String, bias: CaosExpressionValueType = CaosExpressionValueType.ANY): CaosCommand? {
        return when (type) {
            CaosCommandType.RVALUE -> rvalue
            CaosCommandType.LVALUE -> lvalue
            CaosCommandType.COMMAND -> command
            CaosCommandType.CONTROL_STATEMENT -> command
            CaosCommandType.UNDEFINED -> command
        }[token, bias]
    }
}


/**
 * Holder object to fetch and store the variant Libs
 */
object CaosLibs {
    private val libs = mutableMapOf<String, CaosLib>()

    val valuesList:HasGetter<Int, CaosValuesList?> = object : HasGetter<Int, CaosValuesList?> {
        override operator fun get(key:Int) = universalLib.valuesLists["list_$key"]
    }

    operator fun get(variant:CaosVariant) : CaosLib = get(variant.code)

    operator fun get(variantCode: String): CaosLib {
        val variant = universalLib.variantMap[variantCode]
            ?: throw CaosLibException("Invalid variant: '$variantCode' encountered. Known variants are: ${universalLib.variantMap.keys}")
        val lib = CaosLib(universalLib, variant)
        libs[variantCode] = lib
        return lib
    }
}

/*
private class CaosValuesListMap(private val valuesList: Map<String, CaosValuesList>) : HasGetter<Int, CaosValuesList?> {
    override fun get(key: Int): CaosValuesList? {
        return valuesList["list_$key"]
    }
}*/

interface HasLib {
    var caosLib: CaosLib
}