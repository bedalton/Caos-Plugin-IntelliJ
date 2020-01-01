package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference
import com.openc2e.plugins.intellij.caos.utils.orElse
import com.openc2e.plugins.intellij.caos.utils.orFalse

object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    @JvmStatic
    fun getCommandString(command: CaosScriptCommand): String {
        return command.stub?.command
                ?: getCommandTokens(command).joinToString(" ")
    }

    @JvmStatic
    fun getParameterTypes(command: CaosScriptCommandCall): List<CaosScriptExpressionType> {
        return command.expressionList.mapNotNull {
            it.type
        }
    }

    @JvmStatic
    fun getCommandString(call: CaosScriptCommandCall): String {
        return call.stub?.command
                ?: getCommandTokens(call).joinToString(" ")
    }

    @JvmStatic
    fun getCommandTokens(call: CaosScriptCommandCall): List<String> {
        return call.stub?.commandTokens
                ?: call.command.commandTokens
    }

    @JvmStatic
    fun getParametersLength(command: CaosScriptCommandCall): Int {
        return command.stub?.numParameters ?: command.expressionList.size
    }

    @JvmStatic
    fun getCommandTokens(command: CaosScriptCommand): List<String> {
        return command.stub?.commandTokens
                ?: command.commandTokenList.map { it.text }
    }

    @JvmStatic
    fun getCommandString(commandToken: CaosScriptCommandToken): String {
        return commandToken.stub?.text ?: commandToken.text
    }

    @JvmStatic
    fun getIndex(commandToken: CaosScriptCommandToken): Int {
        return commandToken.stub?.index
                ?: (commandToken.parent as? CaosScriptCommand)
                        ?.commandTokenList
                        ?.indexOf(commandToken)
                ?: 0
    }

    @JvmStatic
    fun getType(expression: CaosScriptExpression): CaosScriptExpressionType {
        val type = expression.stub?.type
        if (type != null)
            return type
        return when {
            expression.animationString != null -> CaosScriptExpressionType.BRACKET_STRING
            expression.commandCall?.expressionList != null || expression.commandCall?.commandTokens?.size.orElse(0) > 1
                -> CaosScriptExpressionType.COMMAND
            expression.commandCall?.commandTokens?.size == 1 -> CaosScriptExpressionType.TOKEN
            expression.equalityExpression != null -> CaosScriptExpressionType.EQ
            expression.literal?.isFloat.orFalse() -> CaosScriptExpressionType.FLOAT
            expression.literal?.isInt.orFalse() -> CaosScriptExpressionType.INT
            expression.literal?.isString.orFalse() -> CaosScriptExpressionType.STRING
            expression.varToken != null -> CaosScriptExpressionType.VARIABLE
            else -> CaosScriptExpressionType.UNKNOWN
        }
    }

    @JvmStatic
    fun isFloat(literal: CaosScriptLiteral): Boolean {
        return literal.number?.decimal != null
    }

    @JvmStatic
    fun floatValue(literal: CaosScriptLiteral): Float? {
        return literal.number?.decimal?.text?.toFloatOrNull()
    }

    @JvmStatic
    fun isInt(literal: CaosScriptLiteral): Boolean {
        return literal.number?.int != null
    }

    @JvmStatic
    fun intValue(literal: CaosScriptLiteral): Int? {
        return literal.number?.decimal?.text?.toIntOrNull()
    }

    @JvmStatic
    fun isString(literal: CaosScriptLiteral): Boolean {
        return literal.stringLiteral != null
    }

    @JvmStatic
    fun stringValue(literal: CaosScriptLiteral): String {
        return literal.text
    }

    @JvmStatic
    fun isNumeric(literal: CaosScriptLiteral): Boolean {
        return literal.isFloat || literal.isInt
    }

    @JvmStatic
    fun getCommentText(comment: CaosScriptComment): String {
        return comment.text.trimStart(' ', '*').trim()
    }

    @JvmStatic
    fun isEver(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.ever != null
    }

    @JvmStatic
    fun isUntil(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.untl != null
    }

    @JvmStatic
    fun getStringValue(element: CaosScriptStringLiteral): String {
        val text = element.text
        val length = text.length
        if (length < 2)
            return ""
        val quoteChar = text.substring(1, 2)
        return if (length == 2)
            text.substring(1)
        else if (length > 2 && text.substring(length - 1, length) == quoteChar)
            text.substring(0, length - 1)
        else
            text
    }

    @JvmStatic
    fun getVarGroup(element:CaosScriptVarToken) : CaosScriptVarTokenGroup {
        val group = element.stub?.varGroup
        if (group != null)
            return group
        return when {
            element.varX != null -> CaosScriptVarTokenGroup.VARx
            element.vaXx != null -> CaosScriptVarTokenGroup.VAxx
            element.obvX != null -> CaosScriptVarTokenGroup.OBVx
            element.ovXx != null -> CaosScriptVarTokenGroup.OVxx
            element.mvXx != null -> CaosScriptVarTokenGroup.MVxx
            else -> CaosScriptVarTokenGroup.UNKNOWN
        }
    }

    @JvmStatic
    fun getVarIndex(element:CaosScriptVarToken) : Int? {
        return element.stub?.varIndex
                ?: element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()
    }


    @JvmStatic
    fun isVariant(element: CaosScriptCommandToken, variants:List<String>, strict:Boolean) : Boolean {
        val thisVariant = element.containingCaosFile.variant
        if (thisVariant.isEmpty())
            return !strict
        return thisVariant in variants
    }

    @JvmStatic
    fun getName(element: CaosScriptCommandToken) : String {
        return element.stub?.text ?: element.text
    }

    @JvmStatic
    fun setName(element:CaosScriptCommandToken, newName:String) : PsiElement {
        val newElement = CaosScriptPsiElementFactory.createCommandTokenElement(element.project, newName)
        return element.replace(newElement)
    }

    @JvmStatic
    fun getReference(element:CaosScriptCommandToken) : CaosScriptCommandTokenReference {
        return CaosScriptCommandTokenReference(element)
    }

    @JvmStatic
    fun getPreviousCommands(element:CaosScriptCommandCall) : List<CaosScriptCommandCall> {
        val parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
                ?: return emptyList()
        return parent.codeBlock?.caosElementList?.mapNotNull {
            it.commandCall
        }.orEmpty()
    }

}