@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.utils.notLike
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Holds information on the variant in a Lib file
 */
@Serializable
data class CaosVariantData(
        val name: String,
        val code: String,
        val vars: CaosVarConstraints,
        val commands: Map<String, Int>,
        val lvalues: Map<String, Int>,
        val rvalues: Map<String, Map<Int,Int>>,
        val valuesListsIds:List<Int> = listOf()
) {
    val isOld: Boolean by lazy {
        code in listOf("C1", "C2")
    }
    val isNew: Boolean by lazy {
        code !in listOf("C1", "C2")
    }
    val valuesLists:List<CaosValuesList> get() = CaosLibs.getValuesLists(valuesListsIds).sortedBy {
        it.name
    }

}

/**
 * Holds the max values of a variable type
 */
@Suppress("PropertyName")
@Serializable
data class CaosVarConstraints(
        /** Max variable index for VARx variables */
        val VARx: Int?,
        /** Max variable index for VAxx variables */
        val VAxx: Int?,
        /** Max variable index for OBVx variables */
        val OBVx: Int?,
        /** Max variable index for OVxx variables */
        val OVxx: Int?,
        /** Max variable index for MVxx variables */
        val MVxx: Int?
) : HasGetter<CaosScriptVarTokenGroup, Int?> {

    // Gets the max value by variable type
    override fun get(key: CaosScriptVarTokenGroup): Int? {
        return when (key) {
            CaosScriptVarTokenGroup.VARx -> VARx
            CaosScriptVarTokenGroup.VAxx -> VAxx
            CaosScriptVarTokenGroup.OBVx -> OBVx
            CaosScriptVarTokenGroup.OVxx -> OVxx
            CaosScriptVarTokenGroup.MVxx -> MVxx
            CaosScriptVarTokenGroup.UNKNOWN -> null
        }
    }
}

/**
 * The base lib object struct
 */
@Serializable
data class CaosLibDefinitions(
        val modDate:Long,
        val commands: Map<String,CaosCommand>,
        val variantMap: Map<String, CaosVariantData>,
        val valuesLists: Map<String, CaosValuesList>
)


/**
 * Represents a single command in the CAOS language
 */
@Serializable
data class CaosCommand(
        val id: Int,
        val command: String,
        val parameters: List<CaosParameter>,
        val returnTypeId: Int,
        val description: String? = null,
        val returnValuesListIds: Map<String, Int>? = null,
        val requiresOwnr:Int = 0,
        val variants: List<String>,
        val rvalue:Boolean,
        val lvalue:Boolean,
        val lvalueName:String? = null,
        val commandGroup:String,
        val doifFormat:String? = null,
        val requiresCreatureOwnr:Boolean = false
) {
    val fullCommandHeader: String by lazy {
        val commandHeader = formatNameWithType(command, returnType)
        parameters.nullIfEmpty()?.let {
            val builder = StringBuilder(commandHeader)
            for(parameter in parameters) {
                builder.append(" ").append(formatNameWithType(parameter.name, parameter.type))
            }
            builder.toString()
        } ?: commandHeader
    }

    val isCommand: Boolean by lazy {
        !(rvalue || lvalue)
    }
    val returnType: CaosExpressionValueType by lazy {
        CaosExpressionValueType.fromIntValue(returnTypeId)
    }

    val returnValuesList: HasGetter<CaosVariant, CaosValuesList?> by lazy {
        HasGetterImpl get@{ key ->
            val valuesListId = returnValuesListIds?.get(key.code)
                    ?: return@get null
            CaosLibs.valuesList[valuesListId]
        }
    }


    fun requiresOwnr(variant:CaosVariant) : Boolean {
        return if (variant.isOld)
            requiresOwnr != 0
        else
            abs(requiresOwnr) == 2
    }

    fun requiresOwnrIsError(variant:CaosVariant) : Boolean {
        return if (variant.isOld)
            requiresOwnr > 0
        else
            requiresOwnr == 2
    }
    fun requiresOwnrIsWarning(variant:CaosVariant) : Boolean {
        return if (variant.isOld)
            requiresOwnr < 0
        else
            requiresOwnr == -2
    }
}

private fun formatNameWithType(name:String, type:CaosExpressionValueType) : String {
    return name + type.simpleName.let { simpleName ->
        if (simpleName[0] == '[')
            " $simpleName"
        else
            " ($simpleName)"
    }
}

/**
 * Represents a parameter to a CAOS command
 */
@Serializable
data class CaosParameter(
        val index: Int,
        val name: String,
        val typeId: Int,
        val valuesListIds: Map<String, Int>? = null,
        val description: String? = null,
        val min: Int? = null,
        val max: Int? = null,
        val variableSetToType:String? = null
) {

    constructor(
            index: Int,
            name: String,
            type: CaosExpressionValueType,
            valuesListIds: Map<String, Int>? = mapOf(),
            description: String? = null,
            min: Int? = null,
            max: Int? = null) : this(
            index = index,
            name = name,
            typeId = type.value,
            valuesListIds = valuesListIds,
            description = description,
            min = min,
            max = max
    )

    val type: CaosExpressionValueType by lazy {
        CaosExpressionValueType.fromIntValue(typeId)
    }

    val valuesList: HasGetter<CaosVariant, CaosValuesList?> by lazy {
        if (valuesListIds.isNullOrEmpty()) {
            HasGetterImpl {
                null
            }
        } else {
            HasGetterImpl get@{ key ->
                val valuesListId = valuesListIds[key.code]
                        ?: return@get null
                CaosLibs.valuesList[valuesListId]
            }
        }
    }

}

/**
 * A list of known values for a parameter or return value
 * Useful in generating suggestions
 */
@Serializable
data class CaosValuesList(
        val id:Int,
        val name: String,
        val values: List<CaosValuesListValue>,
        val description: String? = null,
        val extensionType: String? = null
) {

    private val negative = values.filter { it.not }
    private val greaterThan = values.filter { it.greaterThan }

    /**
     * Gets command checking both literal value and int comparison values
     */
    operator fun get(key: String): CaosValuesListValue? {
        key.toIntOrNull()?.let { intValue ->
            return get(intValue)
        }
        return values.firstOrNull { it.value == key }
    }

    /**
     * Gets command using both literal numeric value and value comparison
     */
    operator fun get(key: Int): CaosValuesListValue? {
        return values.firstOrNull { it.intValue == key }
                ?: negative.firstOrNull { it.intValue != key }
                ?: greaterThan.firstOrNull { it.intValue!! < key }
    }

    fun getWithBitFlags(bitFlag:Int) : List<CaosValuesListValue>? {
        if (extensionType notLike "BitFlags")
            return null
        return values.filter filter@{value ->
            val intValue = value.intValue
                    ?: return@filter false
            bitFlag and intValue > 0
        }
    }

    val bitflag:Boolean by lazy {
        extensionType?.equals("BitFlags", true) ?: false
    }
}

/**
 * Represents a value in a values list
 */
@Serializable
data class CaosValuesListValue(
        val value: String,
        val name: String,
        val description: String? = null,
        val beforeRegion: String? = null
) {
    val intValue: Int? by lazy {
        val value = value.trim()
        if (value.startsWith(">") || value.startsWith("!"))
            value.substring(1).toIntOrNull()
        else
            value.toIntOrNull()
    }

    val not by lazy {
        value.startsWith("!")
    }

    val greaterThan by lazy {
        intValue != null && value.startsWith(">")
    }

}


/**
 * Holder for command types
 * ie LValue/RValue/Command
 */
enum class CaosCommandType(val value: String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    CONTROL_STATEMENT("Control Statement"),
    UNDEFINED("???");
}


/**
 * Named game var type enum
 */
enum class CaosScriptNamedGameVarType(val value: Int, val token: String) {
    UNDEF(-1, com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF),
    NAME(1, "NAME"),
    EAME(2, "EAME"),
    GAME(3, "GAME"),
    MAME(4, "MAME");

    companion object {
        fun fromValue(value: Int): CaosScriptNamedGameVarType {
            return when (value) {
                NAME.value -> NAME
                EAME.value -> EAME
                GAME.value -> GAME
                MAME.value -> MAME
                else -> UNDEF
            }
        }
    }
}

val CaosScriptNamedGameVarType.isGameEngineVar:Boolean get() = this == GAME || this == EAME
val CaosScriptNamedGameVarType.isObjectVar:Boolean get() = this == NAME || this == MAME