package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import kotlinx.serialization.Serializable

private val MULTI_SPACE_REGEX = "\\s+".toRegex()

/**
 * Holds information on the variant in a Lib file
 */
@Serializable
data class CaosVariantData(
        val name:String,
        val code:String,
        val vars:CaosVarConstraints,
        val commands:Map<String, Int>,
        val lvalues:Map<String, Int>,
        val rvalues:Map<String, Int>
) {
    val isOld:Boolean by lazy {
        code in listOf("C1", "C2")
    }
    val isNew:Boolean by lazy {
        code !in listOf("C1", "C2")
    }
}

/**
 * Holds the max values of a variable type
 */
@Serializable
data class CaosVarConstraints(
        /** Max variable index for VARx variables */
        val VARx:Int?,
        /** Max variable index for VAxx variables */
        val VAxx:Int?,
        /** Max variable index for OBVx variables */
        val OBVx:Int?,
        /** Max variable index for OVxx variables */
        val OVxx:Int?,
        /** Max variable index for MVxx variables */
        val MVxx:Int?
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
data class CaosLibDefinitions (
        val rvalues:Map<String, CaosCommand>,
        val lvalues:Map<String, CaosCommand>,
        val commands:Map<String, CaosCommand>,
        val variantMap:Map<String, CaosVariantData>,
        val valuesLists:Map<String, CaosValuesList>
)


/**
 * Represents a single command in the CAOS language
 */
@Serializable
data class CaosCommand(
        val id:Int,
        val command:String,
        val parameters:List<CaosParameter>,
        val returnTypeId:Int,
        val description: String?,
        val returnValuesList:Int?,
        val variants:List<String>
) {

    val returnType: CaosExpressionValueType by lazy {
        CaosExpressionValueType.fromIntValue(returnTypeId)
    }

    val commandCheckRegex:Regex by lazy {
        "^(${command.replace(MULTI_SPACE_REGEX, "\\s+")}).*".toRegex()
    }
}

/**
 * Represents a a parameter to a CAOS command
 */
@Serializable
data class CaosParameter(
        val index:Int,
        val name:String,
        val typeId:Int,
        val argumentList:Int? = null,
        val description:String? = null,
        val min:Int? = null,
        val max:Int? = null
) {

    constructor(
            index:Int,
            name:String,
            type:CaosExpressionValueType,
            argumentList:Int? = null,
            description:String? = null,
            min:Int? = null,
            max:Int? = null) : this(
            index = index,
            name = name,
            typeId = type.value,
            argumentList = argumentList,
            description = description,
            min = min,
            max = max
    )

    val type: CaosExpressionValueType by lazy {
        CaosExpressionValueType.fromIntValue(typeId)
    }
}


/**
 * A list of known values for a parameter or return value
 * Useful in generating suggestions
 */
@Serializable
data class CaosValuesList(
        val name:String,
        val values:List<CaosValuesListValue>,
        val description:String?,
        val superType:String?

)

/**
 * Represents a value in a values list
 */
@Serializable
data class CaosValuesListValue(
        val value:String,
        val name:String,
        val description:String?
)