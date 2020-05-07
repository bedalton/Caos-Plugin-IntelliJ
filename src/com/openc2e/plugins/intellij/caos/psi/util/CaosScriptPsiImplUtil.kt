package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference
import com.openc2e.plugins.intellij.caos.references.CaosScriptSubroutineNameReference
import com.openc2e.plugins.intellij.caos.utils.orFalse

object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    @JvmStatic
    fun getCommandElement(call: CaosScriptCommandCall): CaosScriptCommandElement? {
        return call.getParentOfType(CaosScriptCommandElement::class.java)
    }

    @JvmStatic
    fun getCommand(call: CaosScriptCommandCall) : CaosScriptCommandElement? {
        return call.getChildOfType(CaosScriptCommandElement::class.java)
    }

    @JvmStatic
    fun getCommandString(word:CaosDefCommandWord) : String {
        return word.text
    }

    @JvmStatic
    fun getCommandString(rvalue:CaosScriptRvalue) : String {
        return rvalue.text
    }

    @JvmStatic
    fun getCommandToken(rvalue: CaosScriptRvalue): CaosScriptIsCommandToken? {
        return rvalue.getChildOfType(CaosScriptCommandElement::class.java)
                ?.commandToken
    }

    @JvmStatic
    fun getCommandString(command: CaosScriptCommandElement): String {
        return getCommandTokens(command).joinToString(" ")
    }

    @JvmStatic
    fun getParameterTypes(command: CaosScriptCommandCall): List<CaosScriptExpressionType> {
        return command.stub?.parameterTypes ?: command.getChildrenOfType(CaosScriptArgument::class.java).map {
            getType(it)
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
                ?: call.command?.commandToken?.text?.split(" ")
                        .orEmpty()
    }

    @JvmStatic
    fun getParametersLength(command: CaosScriptCommandCall): Int {
        return command.stub?.numParameters ?: command.getChildrenOfType(CaosScriptArgument::class.java).size
    }

    @JvmStatic
    fun getCommandTokens(command: CaosScriptCommandElement): List<String> {
        return command.commandString.split(" ")
    }

    @JvmStatic
    fun getCommandString(commandToken: CaosScriptIsCommandToken): String {
        return commandToken.text
    }

    @JvmStatic
    fun getIndex(commandToken: CaosScriptIsCommandToken): Int {
        return 0
    }

    @JvmStatic
    fun getIndex(commandToken: CaosScriptRvalue): Int {
        return commandToken.getParentOfType(CaosScriptCommandCall::class.java)
                ?.getChildrenOfType(CaosScriptArgument::class.java)
                ?.indexOf(commandToken)
                ?: return 0
    }

    @JvmStatic
    fun getIndex(commandToken: CaosScriptLvalue): Int {
        return commandToken.getParentOfType(CaosScriptCommandCall::class.java)
                ?.getChildrenOfType(CaosScriptArgument::class.java)
                ?.indexOf(commandToken)
                ?: return 0
    }

    @JvmStatic
    fun getType(argument: CaosScriptArgument): CaosScriptExpressionType {
        val expression = argument.getChildOfType(CaosScriptExpression::class.java)
        if (expression != null)
            return getType(expression)
        if (DumbService.isDumb(argument.project))
            return CaosScriptExpressionType.COMMAND
        val index = argument.index
        val command: CaosScriptIsCommandToken
        if (argument is CaosScriptLvalue) {
            command = argument.commandToken
        } else if (argument is CaosScriptRvalue)
            command = argument.commandToken ?: return CaosScriptExpressionType.COMMAND
        else
            return CaosScriptExpressionType.COMMAND
        val resolved = command.reference
                .multiResolve(false)
                .mapNotNull {
                    val temp = (it.element as? CaosDefCompositeElement)
                            ?: return@mapNotNull null
                    temp.getParentOfType(CaosDefCommandDefElement::class.java)
                }
        val type = resolved
                .filter {
                    if (argument is CaosScriptLvalue)
                        it.isLvalue
                    else
                        it.isRvalue
                }.mapNotNull {
                    it.parameterStructs.getOrNull(index)
                }.firstOrNull()
                ?: return CaosScriptExpressionType.COMMAND
        return when (type.type.type.toLowerCase()) {
            "integer" -> CaosScriptExpressionType.INT
            "float" -> CaosScriptExpressionType.FLOAT
            "[string_literal]", "[string]" -> CaosScriptExpressionType.BRACKET_STRING
            "string" -> CaosScriptExpressionType.STRING
            "variable" -> CaosScriptExpressionType.VARIABLE
            "token" -> CaosScriptExpressionType.TOKEN
            "eq" -> CaosScriptExpressionType.EQ
            else -> CaosScriptExpressionType.UNKNOWN
        }
    }

    @JvmStatic
    fun getType(expression: CaosScriptExpression): CaosScriptExpressionType {
        val type = expression.stub?.type
        if (type != null)
            return type
        return when {
            expression.animationString != null -> CaosScriptExpressionType.BRACKET_STRING
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
    fun isEver(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.kEver != null
    }

    @JvmStatic
    fun isUntil(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.kUntl != null
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
    fun getVarGroup(element: CaosScriptVarToken): CaosScriptVarTokenGroup {
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
    fun getVarIndex(element: CaosScriptVarToken): Int? {
        return element.stub?.varIndex
                ?: element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()
    }

    @JvmStatic
    fun isVariant(element: CaosScriptIsCommandToken, variants: List<String>, strict: Boolean): Boolean {
        val thisVariant = (element as? CaosScriptCompositeElement)?.containingCaosFile?.variant ?: ""
        if (thisVariant.isEmpty())
            return !strict
        return thisVariant in variants
    }

    @JvmStatic
    fun isVariant(element: CaosScriptCompositeElement, variants: List<String>, strict: Boolean): Boolean {
        val thisVariant = element.containingCaosFile?.variant ?: ""
        if (thisVariant.isEmpty())
            return !strict
        return thisVariant in variants
    }

    @JvmStatic
    fun getName(element: CaosScriptIsCommandToken): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: CaosScriptIsCommandToken, newName: String): PsiElement {
        val newElement = CaosScriptPsiElementFactory.createCommandTokenElement(element.project, newName)
        return element.replace(newElement)
    }

    @JvmStatic
    fun getReference(element: CaosScriptIsCommandToken): CaosScriptCommandTokenReference {
        return CaosScriptCommandTokenReference(element)
    }

    @JvmStatic
    fun getName(name:CaosScriptSubroutineName) : String {
        return name.text
    }

    @JvmStatic
    fun setName(name:CaosScriptSubroutineName, newName:String) : PsiElement {
        val newElement = CaosScriptPsiElementFactory.createSubroutineNameElement(name.project, newName)
                ?: name
        return name.replace(newElement)
    }

    @JvmStatic
    fun getReference(name:CaosScriptSubroutineName) : CaosScriptSubroutineNameReference {
        return CaosScriptSubroutineNameReference(name)
    }

    /*
    @JvmStatic
    fun getPreviousCommandCalls(element:CaosScriptCommandCall) : List<CaosScriptCommandCall> {
        val parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
                ?: return emptyList()
        val blockCommands = parent.codeBlock?.caosElementList?.mapNotNull {
            it.commandCall
        } ?: return emptyList()
        return emptyList();
    }*/

}


val PsiElement.elementType:IElementType get() = node.elementType