package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import kotlinx.serialization.Serializable

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
        val rvalues: Map<String, Int>,
        val valuesListIds:List<Int> = listOf()
) {
    val isOld: Boolean by lazy {
        code in listOf("C1", "C2")
    }
    val isNew: Boolean by lazy {
        code !in listOf("C1", "C2")
    }
    val valuesLists:List<CaosValuesList> get() = CaosLibs.getLists(valuesListIds).sortedBy {
        it.name
    }

}

/**
 * Holds the max values of a variable type
 */
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
        val rvalues: Map<String, CaosCommand>,
        val lvalues: Map<String, CaosCommand>,
        val commands: Map<String, CaosCommand>,
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
        val description: String?,
        val returnValuesListIds: Map<String, Int>? = null,
        val requiresOwnr:Boolean = false,
        val variants: List<String>,
        val rvalue:Boolean,
        val lvalue:Boolean,
        val commandGroup:String
) {

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
}

/**
 * Represents a a parameter to a CAOS command
 */
@Serializable
data class CaosParameter(
        val index: Int,
        val name: String,
        val typeId: Int,
        val valuesListIds: Map<String, Int>? = null,
        val description: String? = null,
        val min: Int? = null,
        val max: Int? = null
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
        val description: String?,
        val superType: String?
) {

    private val negative = values.filter { it.not }
    private val greaterThan = values.filter { it.greaterThan }

    operator fun get(key: String): CaosValuesListValue? {
        key.toIntSafe()?.let { intValue ->
            return get(intValue)
        }
        return values.firstOrNull { it.value == key }
    }

    operator fun get(key: Int): CaosValuesListValue? {
        return values.firstOrNull { it.intValue == key }
                ?: negative.firstOrNull { it.intValue != key }
                ?: greaterThan.firstOrNull { it.intValue!! < key }
    }

    val bitflag:Boolean by lazy {
        superType?.equalsIgnoreCase("BitFlags").orFalse()
    }
}

/**
 * Represents a value in a values list
 */
@Serializable
data class CaosValuesListValue(
        val value: String,
        val name: String,
        val description: String?
) {
    val intValue: Int? by lazy {
        if (value.startsWith(">") || value.startsWith("!"))
            value.substring(1).toIntSafe()
        else
            value.toIntSafe()
    }

    val not by lazy {
        value.startsWith("!")
    }

    val greaterThan by lazy {
        intValue != null && value.startsWith(">")
    }

}