package com.openc2e.plugins.intellij.caos.psi.util

import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.utils.orFalse

object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock:CaosScriptCodeBlock) : CaosScriptCodeBlock = codeBlock;

    @JvmStatic
    fun getCommandString(command: CaosScriptCommand) : String {
        return command.stub?.command
                ?: getCommandTokens(command).joinToString(" ")
    }

    @JvmStatic
    fun getParameterTypes(command:CaosScriptCommandCall) : List<CaosScriptExpressionType> {
        return command.expressionList.mapNotNull {
            it.type
        }
    }

    @JvmStatic
    fun getParametersLength(command:CaosScriptCommandCall) : int {
        return command.stub?.parameters ?: command.expressionList.size
    }

    @JvmStatic
    fun getCommandTokens(command: CaosScriptCommand) : List<String> {
        return command.stub?.commandTokens
                ?: command.commandTokenList.map { it.text }
    }

    @JvmStatic
    fun getCommandString(commandToken:CaosScriptCommandToken) : String {
        return commandToken.stub?.text ?: commandToken.text
    }

    @JvmStatic
    fun getIndex(commandToken:CaosScriptCommandToken) : Int {
        return commandToken.stub?.index
                ?: (commandToken.parent as? CaosScriptCommand)
                        ?.commandTokenList
                        ?.indexOf(commandToken)
                ?: 0
    }

    @JvmStatic
    fun getType(expression:CaosScriptExpression) : CaosScriptExpressionType {
        val type = expression.stub?.type
        if (type != null)
            return type
        when {
            expression.animationString != null -> CaosScriptExpressionType.BRACKET_STRING
            expression.commandCall?.expressionList != null && expression.commandCall?.tokenList?.size == 1 -> CaosScriptExpressionType.COMMAND
            expression.equalityExpression != null -> CaosScriptExpressionType.EQ
            expression.literal?.isFloat.orFalse() -> CaosScriptExpressionType.FLOAT
            expression.literal?.isInt.orFalse() -> CaosScriptExpressionType.FLOAT
            expression.literal?.isString.orFalse() -> CaosScriptExpressionType.STRING

        }
    }

    @JvmStatic
    fun isFloat(literal:CaosScriptLiteral) : Boolean {
        return literal.number?.decimal != null
    }

    @JvmStatic
    fun floatValue(literal:CaosScriptLiteral) : Float? {
        return literal.number?.decimal?.text?.toFloatOrNull()
    }

    @JvmStatic
    fun isInt(literal: CaosScriptLiteral) : Boolean {
        return literal.number?.int != null
    }

    @JvmStatic
    fun intValue(literal:CaosScriptLiteral) : Int? {
        return literal.number?.decimal?.text?.toIntOrNull()
    }

    @JvmStatic
    fun isString(literal: CaosScriptLiteral) : Boolean {
        return literal.stringLiteral != null
    }

    @JvmStatic
    fun stringValue(literal: CaosScriptLiteral) : String {
        return literal.text
    }

    @JvmStatic
    fun isNumeric(literal: CaosScriptLiteral) : Boolean {
        return literal.isFloat || literal.isInt
    }

}