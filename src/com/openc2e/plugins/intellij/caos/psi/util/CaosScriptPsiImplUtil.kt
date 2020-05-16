package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptElementTypeFactory
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference
import com.openc2e.plugins.intellij.caos.references.CaosScriptSubroutineNameReference
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty

private val EXTRACT_NUMBER_REGEX = "[^0-9.+-]".toRegex()

object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    @JvmStatic
    fun getCommandElement(call: CaosScriptCommandCall): CaosScriptCommandElement? {
        return call.getParentOfType(CaosScriptCommandElement::class.java)
    }

    @JvmStatic
    fun getCommandElement(parent:CaosScriptCommandElementParent) : CaosScriptCommandLike? {
        return parent.getChildOfType(CaosScriptCommandLike::class.java)
    }

    @JvmStatic
    fun getCommand(call: CaosScriptCommandCall): CaosScriptCommandElement? {
        return call.getChildOfType(CaosScriptCommandElement::class.java)
                ?: call.getChildOfType(CaosScriptCommandElementParent::class.java)?.command
    }

    @JvmStatic
    fun getCommandString(word: CaosDefCommandWord): String {
        return word.text
    }

    @JvmStatic
    fun getCommandString(rvalue: CaosScriptRvalue): String {
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
    fun getCommandString(call: CaosScriptCommandCall): String {
        return call.stub?.command
                ?: getCommandTokens(call).joinToString(" ")
    }

    @JvmStatic
    fun getCommandTokens(call: CaosScriptCommandCall): List<String> {
        return call.stub?.commandTokens ?: call.commandElement?.commandToken?.text?.split(" ")
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
        return element.loopTerminator?.cEver != null
    }

    @JvmStatic
    fun isUntil(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.cUntl != null
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
    fun getName(name: CaosScriptSubroutineName): String {
        return name.text
    }

    @JvmStatic
    fun setName(name: CaosScriptSubroutineName, newName: String): PsiElement {
        val newElement = CaosScriptPsiElementFactory.createSubroutineNameElement(name.project, newName)
                ?: name
        return name.replace(newElement)
    }

    @JvmStatic
    fun getReference(name: CaosScriptSubroutineName): CaosScriptSubroutineNameReference {
        return CaosScriptSubroutineNameReference(name)
    }

    @JvmStatic
    fun getRVar(assignment: CaosScriptCAssignment): CaosVar? {
        assignment.stub?.rvalue?.let {
            return it
        }
        return assignment.expectsDecimal?.rvalue?.let {
            return toCaosVar(it)
        }
    }
    @JvmStatic
    fun getLVar(assignment: CaosScriptCAssignment): CaosVar? {
        assignment.stub?.lvalue?.let {
            return it
        }
        return assignment.lvalue?.let {
            return toCaosVar(it)
        }
    }

    @JvmStatic
    fun toCaosVar(rvalue:CaosScriptRvalue) : CaosVar {
        val text = rvalue.text
        (rvalue.varToken)?.let {
            return toCaosVar(it)
        }
        (rvalue.namedConstant)?.let {
            return toCaosVar(it)
        }
        (rvalue.namedVar)?.let {
            return CaosVar.NamedVar(it.text)
        }
        rvalue.literal?.let { literal ->

        }
        rvalue.commandToken?.text?.let {
            return CaosVar.CaosCommandCall(it)
        }

        rvalue.byteString?.text?.let {
            return CaosVar.CaosLiteral.CaosByteString(it)
        }
        rvalue.animationString?.let {
            return CaosVar.CaosLiteral.CaosAnimationString(it.text, it.animR != null)
        }
        return CaosVar.CaosLiteralVal
    }

    @JvmStatic
    fun toCaosVar(namedConstant: CaosScriptNamedConstant) : CaosVar.ConstVal {
        return CaosVar.ConstVal(namedConstant.text)
    }

    @JvmStatic
    fun toCaosVar(namedVar: CaosScriptNamedVar) : CaosVar {
        return namedVar.stub?.caosVar ?: CaosVar.NamedVar(namedVar.text)
    }

    @JvmStatic
    fun toCaosVar(literal: CaosScriptLiteral) : CaosVar {
        literal.intValue()?.let {
            return CaosVar.CaosLiteral.CaosInt(it)
        }
        literal.floatValue()?.let {
            return CaosVar.CaosLiteral.CaosFloat(it)
        }
        literal.stringLiteral?.stringValue?.let {
            return CaosVar.CaosLiteral.CaosString(it)
        }
        LOGGER.severe("Failed to locate caos literal type.")
        return CaosVar.CaosLiteralVal
    }

    @JvmStatic
    fun toCaosVar(varToken:CaosScriptVarToken) : CaosVar {
        val text = varToken.text
        val number = Integer.parseInt(text.replace(EXTRACT_NUMBER_REGEX, ""))
        return when {
            varToken.varX != null -> CaosVar.CaosNumberedVar.CaosVaXXVar(text, number, true)
            varToken.obvX != null -> CaosVar.CaosNumberedVar.CaosOvXXVar(text, number, true)
            varToken.vaXx != null -> CaosVar.CaosNumberedVar.CaosVaXXVar(text, number, false)
            varToken.ovXx != null -> CaosVar.CaosNumberedVar.CaosOvXXVar(text, number, false)
            varToken.mvXx != null -> CaosVar.CaosNumberedVar.CaosMvXXVar(text, number)
            else -> {
                val validTypes = when (varToken.containingCaosFile?.variant?.toUpperCase()) {
                    null -> "???"
                    "C1" -> "[varX,obvX]"
                    "C2" -> "[varX,vaXX,obvX,ovXX]"
                    else -> "[vaXX,ovXX,mvXX]"
                }
                throw Exception("Unexpected caos variable encountered. Expected types are: $validTypes. Found '$text'")
            }
        }
    }

    @JvmStatic
    fun toCaosVar(lvalue:CaosScriptLvalue) : CaosVar {
        lvalue.stub?.caosVar ?: lvalue.namedVar?.let {
            return CaosVar.NamedVar(it.nVar.text)
        }
        (lvalue.varToken)?.let {
            return toCaosVar(it)
        }
        (lvalue.namedConstant)?.let {
            return toCaosVar(it)
        }
        (lvalue.namedVar)?.let {
            return CaosVar.NamedVar(it.text)
        }
        lvalue.commandToken.text?.let {
            return CaosVar.CaosCommandCall(it)
        }
        return CaosVar.CaosLiteralVal
    }

    @JvmStatic
    fun getRValueString(assignment: CaosScriptCAssignment): String {
        return assignment.stub?.rvalue?.text ?: assignment.expectsDecimal?.rvalue?.text ?: ""
    }


    @JvmStatic
    fun getBlockType(doif: CaosScriptDoifStatementStatement): CaosScriptBlockType = CaosScriptBlockType.DOIF


    @JvmStatic
    fun getBlockType(elif: CaosScriptElseIfStatement): CaosScriptBlockType = CaosScriptBlockType.ELIF

    @JvmStatic
    fun getBlockType(doElse: CaosScriptElseStatement): CaosScriptBlockType = CaosScriptBlockType.ELSE

    @JvmStatic
    fun getBlockType(escn: CaosScriptEnumSceneryStatement): CaosScriptBlockType = CaosScriptBlockType.ESCN

    @JvmStatic
    fun getBlockType(loop: CaosScriptLoopStatement): CaosScriptBlockType = CaosScriptBlockType.LOOP

    @JvmStatic
    fun getBlockType(reps: CaosScriptRepeatStatement): CaosScriptBlockType = CaosScriptBlockType.REPS

    @JvmStatic
    fun getBlockType(scrp: CaosScriptMacro): CaosScriptBlockType = CaosScriptBlockType.MACRO

    @JvmStatic
    fun getBlockType(script:CaosScriptEventScript) : CaosScriptBlockType = CaosScriptBlockType.SCRP

    @JvmStatic
    fun getCommandToken(lvalue: CaosScriptLvalue) : CaosScriptIsCommandToken? {
        return lvalue.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    @JvmStatic
    fun getScope(element:CaosScriptCompositeElement) : CaosScope {
        return element.getSelfOrParentOfType(CaosScriptHasCodeBlock::class.java)?.scope() ?: rootScope(element.containingCaosFile!!)
    }

    @JvmStatic
    fun getBlockType(subr: CaosScriptSubroutine): CaosScriptBlockType = CaosScriptBlockType.SUBR

    @JvmStatic
    fun getBlockType(enumStatement: CaosScriptEnumNextStatement): CaosScriptBlockType = CaosScriptBlockType.ENUM

    @JvmStatic
    fun getNumberValue(assignment: CaosScriptConstantAssignment) : Float? {
        return (assignment.int?.text ?: assignment.float?.text)?.nullIfEmpty()?.toFloat()
    }


    @JvmStatic
    fun getOp(assignment:CaosScriptCAssignment) : CaosOp {
        assignment.cKwNegv?.let { return CaosOp.NEGV}
        val assigmentOperator = assignment.commandToken
        assigmentOperator.kSetv?.let { return CaosOp.SETV }
        assigmentOperator.kAddv?.let { return CaosOp.ADDV }
        assigmentOperator.kSubv?.let { return CaosOp.SUBV }
        assigmentOperator.kDivv?.let { return CaosOp.DIVV }
        assigmentOperator.kMulv?.let { return CaosOp.MULV }
        assigmentOperator.kModv?.let { return CaosOp.MODV }
        assigmentOperator.kAndv?.let { return CaosOp.ANDV }
        assigmentOperator.kOrrv?.let { return CaosOp.ORRV }
        return CaosOp.UNDEF
    }

    @JvmStatic
    fun getName(name:CaosScriptNamedVar) : String {
        return name.text.substring(1)
    }

    @JvmStatic
    fun setName(name:CaosScriptNamedVar, newName: String) : PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createNamedVar(name.project, newName)
                ?: return name
        return name.replace(newNameElement)
    }

    @JvmStatic
    fun getName(constant: CaosScriptNamedConstant) : String {
        return constant.text.substring(1)
    }

    @JvmStatic
    fun setName(constant:CaosScriptNamedConstant, newName: String) : PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createNamedConst(constant.project, newName)
                ?: return constant
        return constant.replace(newNameElement)
    }

    @JvmStatic
    fun getName(constantName:CaosScriptConstantName) : String {
        return constantName.text
    }

    @JvmStatic
    fun setName(constantName:CaosScriptConstantName, newName: String) : PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createConstantName(constantName.project, newName)
                ?: return constantName
        return constantName.replace(newNameElement)
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

@Suppress("UNCHECKED_CAST")
private fun <PsiT:PsiElement> CaosScriptCompositeElement.getSelfOrParentOfType(clazz:Class<PsiT>): PsiT? {
    if (clazz.isInstance(this))
        return this as PsiT
    return this.getParentOfType(clazz)
}


val PsiElement.elementType: IElementType get() = node.elementType