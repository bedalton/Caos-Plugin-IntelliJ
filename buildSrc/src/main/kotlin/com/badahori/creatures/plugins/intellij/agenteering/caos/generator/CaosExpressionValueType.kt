package com.badahori.creatures.plugins.intellij.agenteering.caos.generator


/**
 * Represents the base types for a CaosScript expression
 */
internal enum class CaosExpressionValueType(val value: Int, val simpleName: String) {
    INT(1, "integer"),
    FLOAT(2, "float"),
    TOKEN(3, "token"),
    STRING(4, "string"),
    VARIABLE(6, "variable"),
    COMMAND(8, "command"),
    C1_STRING(9, "[string]"),
    BYTE_STRING(10, "[byte_string]"),
    AGENT(11, "agent"),
    ANY(13, "any"),
    CONDITION(15, "condition"),
    DECIMAL(16, "decimal"),
    ANIMATION(17, "[anim]"),
    HEXADECIMAL(18, "hexadecimal"),
    NULL(0, "NULL"),
    PICT_DIMENSION(19, "pict_dimensions"),
    UNKNOWN(-1, "UNKNOWN");

    companion object {

        /**
         * Gets the expression type from its simple name
         */
        @Suppress("SpellCheckingInspection")
        fun fromSimpleName(simpleName: String): CaosExpressionValueType {
            return when (val typeToLower = simpleName.trim().toLowerCase()) {
                "any" -> ANY
                "agent" -> AGENT
                "[anim]" -> ANIMATION
                "[byte_string]", "[byte string]", "byte string", "bytestring" -> BYTE_STRING
                "command" -> COMMAND
                "condition" -> CONDITION
                "decimal" -> DECIMAL
                "float" -> FLOAT
                "hexadecimal" -> HEXADECIMAL
                "int", "integer" -> INT
                "pict_dimension", "pict_dimensions", "picdemensions", "picdemension", "pict dimensions",  "pict dimension" -> PICT_DIMENSION
                "string" -> STRING
                "[string]" -> C1_STRING
                "token" -> TOKEN
                "variable" -> VARIABLE
                "null" -> NULL
                else -> {
                    UNKNOWN
                }
            }
        }

        /**
         * Gets the expression type from its int representation
         */
        fun fromIntValue(value: Int): CaosExpressionValueType {
            return when (value) {
                1 -> INT
                2 -> FLOAT
                3 -> TOKEN
                4 -> STRING
                6 -> VARIABLE
                8 -> COMMAND
                9 -> C1_STRING
                10 -> BYTE_STRING
                11 -> AGENT
                13 -> ANY
                15 -> CONDITION
                16 -> DECIMAL
                17 -> ANIMATION
                18 -> HEXADECIMAL
                0 -> NULL
                19 -> PICT_DIMENSION
                -1 -> UNKNOWN
                else -> return UNKNOWN
            }
        }
    }
}


private val listOfNumberTypes = listOf(
        CaosExpressionValueType.INT,
        CaosExpressionValueType.FLOAT,
        CaosExpressionValueType.DECIMAL
)

internal val CaosExpressionValueType.isNumberType: Boolean
    get()
    = this in listOfNumberTypes

internal val listOfStringTypes = listOf(
        CaosExpressionValueType.STRING,
        CaosExpressionValueType.C1_STRING,
        CaosExpressionValueType.HEXADECIMAL
)

internal val listOfAgentTypes = listOf(
        CaosExpressionValueType.AGENT,
        CaosExpressionValueType.NULL
)
internal val CaosExpressionValueType.isAgentType: Boolean
    get()
    = this in listOfAgentTypes

internal val CaosExpressionValueType.isStringType: Boolean
    get()
    = this in listOfStringTypes

internal val listOfAnyTypes = listOf(
        CaosExpressionValueType.ANY,
        CaosExpressionValueType.UNKNOWN
)
internal val CaosExpressionValueType.isAnyType: Boolean
    get()
    = this in listOfAnyTypes