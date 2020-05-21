package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.references.CaosScriptCommandTokenReference
import com.openc2e.plugins.intellij.caos.references.CaosScriptNamedConstReference
import com.openc2e.plugins.intellij.caos.references.CaosScriptNamedVarReference
import com.openc2e.plugins.intellij.caos.references.CaosScriptSubroutineNameReference
import com.openc2e.plugins.intellij.caos.utils.isOrHasParentOfType

const val UNDEF = "{UNDEF}"

private val EXTRACT_NUMBER_REGEX = "[^0-9.+-]".toRegex()

@Suppress("UNUSED_PARAMETER")
object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    @JvmStatic
    fun getCommandElement(call: CaosScriptCommandCall): CaosScriptCommandElement? {
        return call
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
    fun getCommandToken(element: CaosScriptCTarg): CaosScriptIsCommandToken? {
        return element.cKwTarg
    }

    @JvmStatic
    fun getCommandString(command: CaosScriptCommandElement): String {
        return getCommandTokens(command).joinToString(" ")
    }

    @JvmStatic
    fun getCommandString(call: CaosScriptCommandCall): String {
        return call.stub?.command
                ?: getCommandToken(call)
                        ?.text
                        .orEmpty()
                        .split(" ")
                        .filterNot { it.isEmpty() }
                        .joinToString(" ")
    }

    @JvmStatic
    fun getCommandToken(call: CaosScriptCommandCall): CaosScriptIsCommandToken? {
        val token = call.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: call.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
        if (token == null) {
            LOGGER.severe("Failed to get command token from call: ${call.text}")
        }
        return token
    }

    @JvmStatic
    fun getCommandTokens(call: CaosScriptCommandCall): List<String> {
        return call.stub?.commandTokens
                ?: getCommandToken(call)
                        ?.text
                        .orEmpty()
                        .split(" ")
                        .filterNot { it.isEmpty() }
    }

    @JvmStatic
    fun getCommandToken(assignment: CaosScriptCAssignment): CaosScriptIsCommandToken? {
        return assignment.getChildOfType(CaosScriptIsCommandToken::class.java)
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
    fun getIndex(commandToken: CaosScriptLvalue): Int {
        return commandToken.getParentOfType(CaosScriptCommandCall::class.java)
                ?.getChildrenOfType(CaosScriptArgument::class.java)
                ?.indexOf(commandToken)
                ?: return 0
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
        if (thisVariant.isEmpty()) {
            return !strict
        }
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
    fun getKey(element:CaosScriptNamedGameVar) : CaosVar {
        return element.stub?.key ?: element.expectsValue?.toCaosVar() ?: CaosVar.CaosVarNone
    }

    @JvmStatic
    fun getName(element:CaosScriptNamedGameVar) : String? {
        val value = element.key
        if (value is CaosVar.CaosLiteral.CaosString) {
            return value.value
        }
        return null
    }

    @JvmStatic
    fun setName(element:CaosScriptNamedGameVar, newName:String) : PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createStringElement(element.project, newName)
                ?: return element
        return element.expectsValue?.rvalue?.replace(newNameElement) ?: element
    }

    @JvmStatic
    fun getName(element:CaosScriptToken) : String {
        return element.text
    }

    @JvmStatic
    fun setName(element:CaosScriptToken, newName: String) : PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createTokenElement(element.project, newName)
                ?: element
        return element.replace(newNameElement)
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
    fun getRVar(assignment: CaosScriptCTarg): CaosVar? {
        return assignment.expectsAgent?.rvalue?.toCaosVar()
    }

    @JvmStatic
    fun getRVar(assignment: CaosScriptCAssignment): CaosVar? {
        assignment.stub?.rvalue?.let {
            return it
        }
        assignment.expectsValue?.let {
            return toCaosVar(it.rvalue)
        }
        assignment.rvalueDecimalOrInt?.let { intOrDecimal ->
            intOrDecimal.expectsDecimal?.let {
                toCaosVar(it.rvalue)
            }
            intOrDecimal.expectsInt?.let {
                toCaosVar(it.rvalue)
            }
        }
        assignment.expectsDecimal?.let {
            return toCaosVar(it.rvalue)
        }
        assignment.expectsString?.let {
            return toCaosVar(it.rvalue)
        }
        assignment.expectsAgent?.let {
            return toCaosVar(it.rvalue)
        }
        return null
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
    fun toCaosVar(rvalue: CaosScriptRvalue): CaosVar {
        rvalue.stub?.caosVar?.let {
            return it
        }
        (rvalue.varToken)?.let {
            return toCaosVar(it)
        }
        (rvalue.namedConstant)?.let {
            return toCaosVar(it)
        }
        (rvalue.namedVar)?.let {
            return CaosVar.NamedVar(it.text)
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

        rvalue.number?.let { number ->
            number.decimal?.let {
                return CaosVar.CaosLiteral.CaosFloat(it.text.toFloat())
            }
            number.int?.let {
                return CaosVar.CaosLiteral.CaosInt(it.text.toInt())
            }
        }

        rvalue.namedGameVar?.let {
            return it.toCaosVar() ?: CaosVar.CaosLiteralVal
        }

        rvalue.stringLiteral?.let {
            return CaosVar.CaosLiteral.CaosString(it.stringValue)
        }

        rvalue.rvaluePrime?.let { rvaluePrime ->
            return rvaluePrime.getChildOfType(CaosScriptIsCommandToken::class.java)?.let {
                val commandString = it.commandString
                if (commandString.toLowerCase() == "null")
                    return CaosVar.CaosVarNull
                return CaosVar.CaosCommandCall(commandString)
            } ?: CaosVar.CaosLiteralVal
        }

        return CaosVar.CaosVarNone
    }

    @JvmStatic
    fun toCaosVar(namedConstant: CaosScriptNamedConstant): CaosVar.ConstVal {
        return CaosVar.ConstVal(namedConstant.text)
    }

    @JvmStatic
    fun toCaosVar(namedVar: CaosScriptNamedVar): CaosVar {
        return CaosVar.NamedVar(namedVar.text)
    }

    @JvmStatic
    fun toCaosVar(varToken: CaosScriptVarToken): CaosVar {
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
    fun toCaosVar(lvalue: CaosScriptLvalue): CaosVar {
        lvalue.stub?.caosVar?.let {
            return it
        }
        lvalue.namedVar?.let {
            return CaosVar.NamedVar(it.nVar.text)
        }
        (lvalue.varToken)?.let {
            return toCaosVar(it)
        }
        (lvalue.namedVar)?.let {
            return CaosVar.NamedVar(it.text)
        }
        lvalue.commandToken?.text?.let {
            return CaosVar.CaosCommandCall(it)
        }
        LOGGER.info("Failed to understand lvalue: ${lvalue.text}, first child = ${lvalue.firstChild?.elementType}")
        return CaosVar.CaosLiteralVal
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsAgent): CaosScriptExpectedType {
        return CaosScriptExpectedType.AGENT
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptLvalue): CaosScriptExpectedType {
        return CaosScriptExpectedType.VARIABLE
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsAgent): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsString): CaosScriptExpectedType {
        return CaosScriptExpectedType.STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsString): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsByteString): CaosScriptExpectedType {
        return CaosScriptExpectedType.BYTE_STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsByteString): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsC1String): CaosScriptExpectedType {
        return CaosScriptExpectedType.C1_STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsC1String): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsInt): CaosScriptExpectedType {
        return CaosScriptExpectedType.INT
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsInt): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsFloat): CaosScriptExpectedType {
        return CaosScriptExpectedType.FLOAT
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsFloat): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsDecimal): CaosScriptExpectedType {
        return CaosScriptExpectedType.DECIMAL
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsDecimal): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsValue): CaosScriptExpectedType {
        return CaosScriptExpectedType.ANY
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsValue): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsToken): CaosScriptExpectedType {
        return CaosScriptExpectedType.TOKEN
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsToken): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getIndex(element: CaosScriptArgument): Int {
        val parent = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return -1
        return parent.arguments.indexOf(element)
    }

    @JvmStatic
    fun getArguments(command: CaosScriptCommandElement): List<CaosScriptArgument> {
        return command.getChildrenOfType(CaosScriptArgument::class.java)
    }

    @JvmStatic
    fun getArgumentValues(command: CaosScriptCommandCall): List<CaosVar> {
        return command.stub?.argumentValues ?: command.arguments.map { it.toCaosVar() }
    }

    @JvmStatic
    fun getArgumentValues(rvalue: CaosScriptRvalue): List<CaosVar> {
        return rvalue.stub?.argumentValues ?: rvalue.arguments.map { it.toCaosVar() }
    }

    @JvmStatic
    fun getArgumentValues(lvalue: CaosScriptLvalue): List<CaosVar> {
        return lvalue.stub?.argumentValues ?: lvalue.arguments.map { it.toCaosVar() }
    }

    @JvmStatic
    fun getArgumentValues(element: CaosScriptCommandElement): List<CaosVar> {
        return element.getChildrenOfType(CaosScriptArgument::class.java).map {
            it.toCaosVar()
        }
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
    fun getBlockType(script: CaosScriptEventScript): CaosScriptBlockType = CaosScriptBlockType.SCRP

    @JvmStatic
    fun getCommandToken(lvalue: CaosScriptLvalue): CaosScriptIsCommandToken? {
        return lvalue.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    @JvmStatic
    fun getScope(element: CaosScriptCompositeElement): CaosScope {
        return element.getSelfOrParentOfType(CaosScriptHasCodeBlock::class.java)?.scope()
                ?: rootScope(element.containingCaosFile!!)
    }

    @JvmStatic
    fun getBlockType(subr: CaosScriptSubroutine): CaosScriptBlockType = CaosScriptBlockType.SUBR

    @JvmStatic
    fun getBlockType(enumStatement: CaosScriptEnumNextStatement): CaosScriptBlockType = CaosScriptBlockType.ENUM

    @JvmStatic
    fun getFamily(script:CaosScriptEventScript) : Int {
        return script.stub?.family ?: script.classifier?.family?.text?.toInt() ?: -1
    }
    @JvmStatic
    fun getGenus(script:CaosScriptEventScript) : Int {
        return script.stub?.genus?: script.classifier?.genus?.text?.toInt() ?: -1
    }
    @JvmStatic
    fun getSpecies(script:CaosScriptEventScript) : Int {
        return script.stub?.species ?: script.classifier?.species?.text?.toInt() ?: -1
    }
    @JvmStatic
    fun getEventNumber(script:CaosScriptEventScript) : Int {
        return script.stub?.eventNumber ?: script.eventNumberElement?.text?.toInt() ?: -1
    }

    @JvmStatic
    fun getValue(assignment: CaosScriptConstantAssignment): CaosVar? {
        assignment.constantValue?.let { value ->
            value.float?.let {
                return CaosVar.CaosLiteral.CaosFloat(it.text.toFloat())
            }
            value.int?.let {
                return CaosVar.CaosLiteral.CaosInt(it.text.toInt())
            }
            value.varToken?.let {
                return toCaosVar(it)
            }
        }
        return CaosVar.CaosVarNone
    }

    @JvmStatic
    fun getName(assignment: CaosScriptConstantAssignment): String {
        return assignment.stub?.name ?: assignment.namedConstant.text.substring(1) ?: "UNDEF"
    }

    @JvmStatic
    fun getName(assignment: CaosScriptNamedVarAssignment): String {
        return assignment.stub?.name ?: assignment.namedVar.text.substring(1) ?: "UNDEF"
    }

    @JvmStatic
    fun setName(assignment: CaosScriptNamedVarAssignment, newName:String) : PsiElement {
        val newElement = CaosScriptPsiElementFactory.createNamedVar(assignment.project, newName)
                ?: return assignment
        return assignment.namedVar.replace(newElement)
    }


    @JvmStatic
    fun getValue(assignment: CaosScriptNamedVarAssignment) : CaosVar? {
        return assignment.varToken?.let {
            toCaosVar(it)
        }
    }

    @JvmStatic
    fun getOp(assignment: CaosScriptCAssignment): CaosOp {
        assignment.cKwNegv?.let { return CaosOp.NEGV }
        assignment.cKwSetv?.let { return CaosOp.SETV }
        assignment.kSeta?.let { return CaosOp.SETV }
        assignment.kSets?.let { return CaosOp.SETV }
        assignment.cKwAssignNumber?.let {
            it.kAddv?.let { return CaosOp.ADDV }
            it.kAndv?.let { return CaosOp.ANDV }
            it.kModv?.let { return CaosOp.MODV }
            it.kMulv?.let { return CaosOp.MULV }
            it.kDivv?.let { return CaosOp.DIVV }
            it.kOrrv?.let { return CaosOp.ORRV }
            it.kSubv?.let { return CaosOp.SUBV }
        }
        return CaosOp.UNDEF
    }

    @JvmStatic
    fun getName(name: CaosScriptNamedVar): String {
        return name.text.substring(1)
    }

    @JvmStatic
    fun setName(name: CaosScriptNamedVar, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createNamedVar(name.project, newName)
                ?: return name
        return name.replace(newNameElement)
    }

    @JvmStatic
    fun getReference(element: CaosScriptNamedVar) : CaosScriptNamedVarReference {
        return CaosScriptNamedVarReference(element)
    }

    @JvmStatic
    fun getName(constant: CaosScriptNamedConstant): String {
        return constant.text.substring(1)
    }

    @JvmStatic
    fun setName(constant: CaosScriptNamedConstant, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createNamedConst(constant.project, newName)
                ?: return constant
        return constant.replace(newNameElement)
    }

    @JvmStatic
    fun getReference(constant: CaosScriptNamedConstant) : CaosScriptNamedConstReference {
        return CaosScriptNamedConstReference(constant)
    }

    @JvmStatic
    fun getVarType(element: CaosScriptNamedGameVar): CaosScriptNamedGameVarType {
        element.caosNamedVarKw.let {
            it.kName?.let {
                return CaosScriptNamedGameVarType.NAME
            }
            it.kMame?.let {
                return CaosScriptNamedGameVarType.MAME
            }
            it.kGame?.let {
                return CaosScriptNamedGameVarType.GAME
            }
            it.kEame?.let {
                return CaosScriptNamedGameVarType.EAME
            }
        }
        return CaosScriptNamedGameVarType.UNDEF
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptNamedGameVar): CaosVar? {
        val name = element.name
                ?: return null
        return when (element.varType) {
            CaosScriptNamedGameVarType.EAME -> CaosVar.CaosNamedGameVar.EameVar(name)
            CaosScriptNamedGameVarType.MAME -> CaosVar.CaosNamedGameVar.MameVar(name)
            CaosScriptNamedGameVarType.GAME -> CaosVar.CaosNamedGameVar.GameVar(name)
            CaosScriptNamedGameVarType.NAME -> CaosVar.CaosNamedGameVar.NameVar(name)
            CaosScriptNamedGameVarType.UNDEF -> null
        }
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
private fun <PsiT : PsiElement> CaosScriptCompositeElement.getSelfOrParentOfType(clazz: Class<PsiT>): PsiT? {
    if (clazz.isInstance(this))
        return this as PsiT
    return this.getParentOfType(clazz)
}


val PsiElement.elementType: IElementType get() = node.elementType


enum class CaosScriptNamedGameVarType(val value: Int, val token: String) {
    UNDEF(-1, com.openc2e.plugins.intellij.caos.psi.util.UNDEF),
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

fun PsiElement.getEnclosingCommandType() : CaosCommandType {
    var parent:PsiElement? = parent
            ?: return CaosCommandType.UNDEFINED
    while (parent != null && parent !is CaosScriptCommandElement && parent !is CaosScriptCodeBlockLine) {
        parent = parent.parent
    }
    if (parent == null || parent !is CaosScriptCommandElement)
        return CaosCommandType.UNDEFINED
    return when (parent) {
        is CaosScriptCommandCall -> CaosCommandType.COMMAND
        is CaosScriptRvalue -> CaosCommandType.RVALUE
        is CaosScriptLvalue -> CaosCommandType.LVALUE
        else -> {
            parent.getEnclosingCommandType()
        }
    }
}

enum class CaosCommandType(val value:String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    UNDEFINED("???")
}