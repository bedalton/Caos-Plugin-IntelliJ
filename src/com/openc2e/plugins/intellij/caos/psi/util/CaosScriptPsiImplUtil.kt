package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.caos.def.stubs.api.isVariant
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.caos.hints.CaosScriptPresentationUtil
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.references.*
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType
import kotlin.math.floor

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
    fun getCommandToken(rvalue: CaosScriptRvalue): CaosScriptIsCommandToken? {
        return rvalue.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: rvalue.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptCTarg): CaosScriptIsCommandToken? {
        return element.cKwTarg
    }

    @JvmStatic
    fun getCommandString(command: CaosScriptCommandElement): String {
        val type = command.getEnclosingCommandType()
        return when (type) {
            CaosCommandType.COMMAND -> command
                    .getSelfOrParentOfType(CaosScriptCommandCall::class.java)
                    ?.let { getCommandString(it) }
            CaosCommandType.RVALUE -> command
                    .getSelfOrParentOfType(CaosScriptRvalue::class.java)
                    ?.let { getCommandString(it) }
            CaosCommandType.LVALUE -> command
                    .getSelfOrParentOfType(CaosScriptLvalue::class.java)
                    ?.let { getCommandString(it) }
            CaosCommandType.UNDEFINED -> null
        } ?: UNDEF
    }

    @JvmStatic
    fun getCommandString(element: CaosScriptRvalue): String {
        return (element.stub?.caosVar ?: element.toCaosVar()).text
    }

    @JvmStatic
    fun getCommandString(element: CaosScriptLvalue): String {
        return (element.stub?.caosVar ?: element.toCaosVar()).text
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
        var lastParent: PsiElement? = commandToken.parent
        while (lastParent != null && lastParent.parent !is CaosScriptCommandElement) {
            lastParent = lastParent.parent
        }
        val parent = lastParent as? CaosScriptCommandElement
                ?: return 0
        if (lastParent !is CaosScriptArgument)
            return 0
        return parent.arguments.indexOf(lastParent)
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
    fun getKey(element: CaosScriptNamedGameVar): CaosVar {
        return element.stub?.key ?: element.expectsValue?.toCaosVar() ?: CaosVar.CaosVarNone
    }

    @JvmStatic
    fun getName(element: CaosScriptNamedGameVar): String? {
        val value = element.key
        if (value is CaosVar.CaosLiteral.CaosString) {
            return value.value
        }
        return null
    }

    @JvmStatic
    fun setName(element: CaosScriptNamedGameVar, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createStringElement(element.project, newName)
                ?: return element
        return element.expectsValue?.rvalue?.replace(newNameElement) ?: element
    }

    @JvmStatic
    fun getName(element: CaosScriptToken): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: CaosScriptToken, newName: String): PsiElement {
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
        assignment.getChildOfType(CaosScriptExpectsValueOfType::class.java)
                ?.rvalue?.let {
                    toCaosVar(it)
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

        rvalue.expression?.let {
            return toCaosVar(it)
        }

        rvalue.namedGameVar?.let {
            return it.toCaosVar() ?: CaosVar.CaosLiteralVal
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
    fun toCaosVar(expression: CaosScriptExpression): CaosVar {
        (expression.varToken)?.let {
            return toCaosVar(it)
        }
        (expression.namedConstant)?.let {
            return toCaosVar(it)
        }
        (expression.namedVar)?.let {
            return CaosVar.NamedVar(it.text)
        }
        expression.rvaluePrime?.let { rvaluePrime ->
            rvaluePrime.getChildOfType(CaosScriptIsCommandToken::class.java)?.let {
                return CaosVar.CaosCommandCall(it.text)
            }
            return CaosVar.CaosVarNull
        }


        expression.byteString?.text?.let {
            return CaosVar.CaosLiteral.CaosByteString(expression.stringValue ?: "", expression.byteStringArray)
        }
        expression.animationString?.let {
            return CaosVar.CaosLiteral.CaosAnimationString(it.text, expression.animation)
        }

        expression.number?.let { number ->
            number.decimal?.let {
                return CaosVar.CaosLiteral.CaosFloat(it.text.toFloat())
            }
            number.int?.let {
                return CaosVar.CaosLiteral.CaosInt(it.text.toInt())
            }
        }

        expression.token?.text?.let {
            return CaosVar.CaosLiteral.CaosToken(it)
        }

        expression.namedGameVar?.let {
            return it.toCaosVar() ?: CaosVar.CaosLiteralVal
        }

        expression.c1String?.let {
            return CaosVar.CaosLiteral.CaosC1String(it.stringValue)
        }

        expression.quoteStringLiteral?.let {
            return CaosVar.CaosLiteral.CaosC1String(it.stringValue)
        }

        expression.rvaluePrime?.let { rvaluePrime ->
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
    fun getByteStringArray(expression: CaosScriptExpression) : List<Int> {
        val variant = expression.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        expression.stringValue?.let { stringValue ->
            if (variant !in listOf("C1", "C2")) {
                return stringValue.split(" ").map { it.toInt() }.orEmpty()
            }
            return stringValue.toCharArray().map { it.toInt() }
        }
        return emptyList()
    }

    @JvmStatic
    fun getAnimation(expression: CaosScriptExpression) : CaosAnimation? {
        val variant = expression.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        val stringValue = expression.stringValue
                ?: return null
        if (stringValue.isEmpty())
            return CaosAnimation.Animation(emptyList(), false, null)
        if (variant !in listOf("C1", "C2")) {
            return getCVPlusAnimation(stringValue)
        }
        val charsRaw = stringValue.toCharArray().toList()
        val poses:List<Int>
        val repeats = charsRaw.last().toUpperCase() == 'R'
        val repeatsFrom = if (repeats) 0 else null
        if (repeats) {
            poses = charsRaw.subList(0, charsRaw.lastIndex).map { it.toInt() }
        } else {
            poses = charsRaw.map { it.toInt() }
        }
        return CaosAnimation.Animation(poses, repeats, repeatsFrom)
    }

    private fun getCVPlusAnimation(stringValue:String) : CaosAnimation? {
        val posesRaw = stringValue.split(" ").map { it.toInt() }
        var repeats:Boolean = false
        var repeatsFrom:Int? = null
        when (posesRaw.size) {
            0 -> return CaosAnimation.Animation(emptyList(), false, null)
            1 -> if (posesRaw[0] == 255) {
                return CaosAnimation.ErrorAnimation("Repeated animation has no body")
            }
            2 -> {
                if (posesRaw[0] == 255) {
                    repeats = true
                    repeatsFrom = posesRaw[1]
                } else if (posesRaw[1] == 255) {
                    return CaosAnimation.ErrorAnimation("Repeated animation has no body")
                }
            }
            else -> {
                if (posesRaw.last() == 255) {
                    repeats = true
                    repeatsFrom = 0
                }
                else if (posesRaw[posesRaw.lastIndex - 1] == 255) {
                    repeats = true
                    repeatsFrom = posesRaw.last()
                }
            }
        }

        val poses:List<Int> = if (repeats) {
            if (repeatsFrom != null) {
                posesRaw.subList(0, posesRaw.lastIndex - 1)
            } else {
                posesRaw.subList(0, posesRaw.lastIndex)
            }
        } else {
            posesRaw
        }
        if (repeats && repeatsFrom == null)
            repeatsFrom = 0
        return CaosAnimation.Animation(poses, repeats, repeatsFrom)
    }

    @JvmStatic
    fun getCreatureAnimation(expression: CaosScriptExpression) : CaosAnimation? {
        val variant = expression.containingCaosFile?.variant ?: CaosScriptProjectSettings.variant
        val stringValue = expression.stringValue
                ?: return null
        if (stringValue.isEmpty())
            return CaosAnimation.Animation(emptyList(), false, null)
        if (variant !in listOf("C1", "C2")) {
            return getCVPlusAnimation(stringValue)
        }
        val charsRawTemp = stringValue.toCharArray().toList()
        val repeats = charsRawTemp.last().toUpperCase() == 'R'
        var repeatsFrom = if (repeats) 0 else null
        val charsRaw = if (repeats) {
            charsRawTemp.subList(0, charsRawTemp.lastIndex)
        } else {
            charsRawTemp
        }
        val numPoses = floor(charsRaw.size / 3.0f).toInt()
        if (charsRaw.size % 3 != 0) {
            return CaosAnimation.ErrorAnimation("Creature animation must be a multiple of 3")
        }
        val poses = (0 until numPoses).map {
            val base = it * 3
            "${charsRaw[base]}${charsRaw[base + 1]}${charsRaw[base+2]}".toInt()
        }
        return CaosAnimation.Animation(poses, repeats, repeatsFrom)
    }

    @JvmStatic
    fun getStringValue(string: CaosScriptC1String): String {
        return string.textLiteral?.text ?: ""
    }

    @JvmStatic
    fun getStringValue(stringIn: CaosScriptQuoteStringLiteral): String {
        var string = stringIn.text
        if (string.startsWith("\""))
            string = string.substring(1)
        if (string.endsWith("\""))
            string = string.substring(0, string.length - 2)
        return string
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
    fun getExpectedType(element: CaosScriptExpectsAgent): CaosExpressionValueType {
        return CaosExpressionValueType.AGENT
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptLvalue): CaosExpressionValueType {
        return CaosExpressionValueType.VARIABLE
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsAgent): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsQuoteString): CaosExpressionValueType {
        return CaosExpressionValueType.STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsQuoteString): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsByteString): CaosExpressionValueType {
        return CaosExpressionValueType.BYTE_STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsByteString): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsC1String): CaosExpressionValueType {
        return CaosExpressionValueType.C1_STRING
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsC1String): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsInt): CaosExpressionValueType {
        return CaosExpressionValueType.INT
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsInt): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsFloat): CaosExpressionValueType {
        return CaosExpressionValueType.FLOAT
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsFloat): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsDecimal): CaosExpressionValueType {
        return CaosExpressionValueType.DECIMAL
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsDecimal): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsValue): CaosExpressionValueType {
        return CaosExpressionValueType.ANY
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsValue): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptExpectsToken): CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptExpectsToken): CaosVar {
        return element.stub?.caosVar ?: element.rvalue.toCaosVar()
    }

    @JvmStatic
    fun getIndex(element: CaosScriptArgument): Int {
        val commandElement = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return -1
        val arguments = commandElement.arguments
        var temp:PsiElement? = element
        while(temp != null && temp.parent != commandElement) {
            val index = arguments.indexOf(temp)
            if (index >= 0)
                return index
            temp = temp.parent
        }
        return -1;
    }


    @JvmStatic
    fun getArguments(command: CaosScriptCommandElement): List<CaosScriptArgument> {
        val base = (command as? CaosScriptRvalue)?.rvaluePrime ?: command
        return base.getChildrenOfType(CaosScriptArgument::class.java)
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
        return lvalue.getChildOfType(CaosScriptIsCommandToken::class.java)
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
    fun getFamily(script: CaosScriptEventScript): Int {
        return script.stub?.family ?: script.classifier?.family?.text?.toInt() ?: -1
    }

    @JvmStatic
    fun getGenus(script: CaosScriptEventScript): Int {
        return script.stub?.genus ?: script.classifier?.genus?.text?.toInt() ?: -1
    }

    @JvmStatic
    fun getSpecies(script: CaosScriptEventScript): Int {
        return script.stub?.species ?: script.classifier?.species?.text?.toInt() ?: -1
    }

    @JvmStatic
    fun getEventNumber(script: CaosScriptEventScript): Int {
        return script.stub?.eventNumber ?: script.eventNumberElement?.text?.toInt() ?: -1
    }

    @JvmStatic
    fun getValue(assignment: CaosScriptConstantAssignment): CaosNumber {
        assignment.constantValue?.let { value ->
            value.int?.let {
                return CaosNumber.CaosIntNumber(it.text.toInt())
            }
            value.float?.let {
                return CaosNumber.CaosFloatNumber(it.text.toFloat())
            }
        }
        return CaosNumber.Undefined
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
    fun setName(assignment: CaosScriptNamedVarAssignment, newName: String): PsiElement {
        val newElement = CaosScriptPsiElementFactory.createNamedVar(assignment.project, newName)
                ?: return assignment
        return assignment.namedVar.replace(newElement)
    }


    @JvmStatic
    fun getValue(assignment: CaosScriptNamedVarAssignment): CaosVar? {
        return assignment.varToken?.let {
            toCaosVar(it)
        }
    }

    @JvmStatic
    fun getOp(assignment: CaosScriptCAssignment): CaosOp {
        assignment.cKwNegv?.let { return CaosOp.NEGV }
        assignment.cKwSetv?.let { return CaosOp.SETV }
        assignment.cKwSetv?.let { return CaosOp.SETV }
        assignment.cKwAssignAgent?.let { return CaosOp.SETV }
        assignment.cKwAssignString?.let { return CaosOp.SETS }
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
    fun getReference(element: CaosScriptNamedVar): CaosScriptNamedVarReference {
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
    fun getReference(constant: CaosScriptNamedConstant): CaosScriptNamedConstReference {
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

    @JvmStatic
    fun getReference(element: CaosScriptVarToken): CaosScriptVarTokenReference {
        return CaosScriptVarTokenReference(element)
    }

    @JvmStatic
    fun getPresentation(element: CaosScriptIsCommandToken): ItemPresentation {
        return CaosScriptPresentationUtil.getPresentation(element)
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

    @JvmStatic
    fun isNumeric(expression: CaosScriptExpression): Boolean {
        return expression.number?.let {
            it.int != null || it.decimal != null
        } ?: return false
    }

    @JvmStatic
    fun isInt(expression: CaosScriptExpression): Boolean {
        return expression.number?.int != null
    }

    @JvmStatic
    fun isFloat(expression: CaosScriptExpression): Boolean {
        return expression.number?.decimal != null
    }

    @JvmStatic
    fun getIntValue(expression: CaosScriptExpression): Int? {
        return expression.number?.int?.text?.toInt()
    }

    @JvmStatic
    fun getFloatValue(expression: CaosScriptExpression): Float? {
        return expression.number?.let {
            (it.int ?: it.decimal)?.text?.toFloat()
        }
    }

    @JvmStatic
    fun getConstValue(expression: CaosScriptExpression): CaosNumber? {
        val project = expression.project
        if (DumbService.isDumb(project))
            return null
        val assignment = (expression.namedConstant
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element)
                ?.getSelfOrParentOfType(CaosScriptConstantAssignment::class.java)
                ?: return null

        return assignment.value
    }

    @JvmStatic
    fun isString(expression: CaosScriptExpression): Boolean {
        return expression.c1String != null
                || expression.byteString != null
                || expression.animationString != null
                || expression.quoteStringLiteral != null
                || expression.token != null
    }


    @JvmStatic
    fun isC1String(expression: CaosScriptExpression): Boolean {
        return expression.c1String != null
    }


    @JvmStatic
    fun isByteString(expression: CaosScriptExpression): Boolean {
        return expression.byteString != null
    }

    @JvmStatic
    fun isQuoteString(expression: CaosScriptExpression): Boolean {
        return expression.quoteStringLiteral != null
    }

    @JvmStatic
    fun isToken(expression: CaosScriptExpression): Boolean {
        return expression.token != null
    }

    fun isVar(expression: CaosScriptExpression): Boolean {
        return expression.varToken != null
                || expression.namedGameVar != null
                || expression.namedVar != null
    }

    fun isConst(expression: CaosScriptExpression): Boolean {
        return expression.namedConstant != null
    }

    @JvmStatic
    fun getStringValue(expression: CaosScriptExpression): String? {
        (expression.c1String ?: expression.animationString ?: expression.byteString)?.let {
            return it.text.trim('[', ']')
        }
        return expression.token?.text ?: expression.quoteStringLiteral?.stringValue
    }

    @JvmStatic
    fun getParameterListValue(element: CaosScriptExpression): CaosDefTypeDefValueStruct? {
        element.getParentOfType(CaosScriptExpectsValueOfType::class.java)?.let {
            return getParameterListValue(it)
        }
        val containingCommand = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return null

        // Values for lists can only be inferred by literal values
        if (!(element.isNumeric || element.isString))
            return null

        // Get the referenced parent command
        val reference = containingCommand
                .commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null

        // Find the type def list name
        val typeDef = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(0)
                ?.type
                ?.typedef
                ?: return null

        // Get variant
        val variant = element.containingCaosFile?.variant
                ?: return null
        // Find list in index
        val list = CaosDefTypeDefinitionElementsByNameIndex
                .Instance[typeDef, element.project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
                ?: return null
        // Get actual value for literal in list
        return list.keys.firstOrNull { it.key == element.text }
    }

    @JvmStatic
    fun getParameterListValue(element: CaosScriptExpectsValueOfType): CaosDefTypeDefValueStruct? {
        val containingCommand = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return null

        // Values for lists can only be inferred by literal values
        if (element.toCaosVar() !is CaosVar.CaosLiteral)
            return null

        // Get the referenced parent command
        val reference = containingCommand
                .commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null

        // Find the type def list name
        val typeDef = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(element.index)
                ?.type
                ?.typedef
                ?: return null

        // Get variant
        val variant = element.containingCaosFile?.variant
                ?: return null
        // Find list in index
        val list = CaosDefTypeDefinitionElementsByNameIndex
                .Instance[typeDef, element.project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
                ?: return null
        // Get actual value for literal in list
        return list.keys.firstOrNull { it.key == element.text }
    }

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

fun PsiElement.getEnclosingCommandType(): CaosCommandType {
    var parent: PsiElement? = parent
            ?: return CaosCommandType.UNDEFINED
    while (parent != null && parent !is CaosScriptCommandElement && parent !is CaosScriptCodeBlockLine) {
        parent = parent.parent
    }
    if (parent == null || parent !is CaosScriptCommandElement) {
        if (isOrHasParentOfType(CaosScriptIncomplete::class.java))
            return CaosCommandType.COMMAND
        if (this.isOrHasParentOfType(CaosScriptExpression::class.java) || this.hasParentOfType(CaosScriptEqualityExpression::class.java))
            return CaosCommandType.RVALUE
        return CaosCommandType.UNDEFINED
    }
    return when (parent) {
        is CaosScriptCommandCall -> CaosCommandType.COMMAND
        is CaosScriptRvalue -> CaosCommandType.RVALUE
        is CaosScriptLvalue -> CaosCommandType.LVALUE
        else -> parent.getEnclosingCommandType()
    }
}

enum class CaosCommandType(val value: String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    UNDEFINED("???")
}

fun <PsiT : PsiElement> PsiElement.getSelfOrParentOfType(parentClass: Class<PsiT>): PsiT? {
    if (parentClass.isInstance(this))
        return parentClass.cast(this)
    return PsiTreeUtil.getParentOfType(this, parentClass)
}

fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass: Class<PsiT>): Boolean {
    if (parentClass.isInstance(this))
        return true
    return PsiTreeUtil.getParentOfType(this, parentClass) != null
}