package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.ValuesListEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.documentation.CaosScriptPresentationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.Case
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.floor

const val UNDEF = "{UNDEF}"

private val EXTRACT_NUMBER_REGEX = "[^0-9.+-]".toRegex()

@Suppress("UNUSED_PARAMETER", "SpellCheckingInspection")
object CaosScriptPsiImplUtil {

    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    @JvmStatic
    fun getCommandElement(call: CaosScriptCommandCall): CaosScriptCommandElement? {
        return call.firstChild as? CaosScriptCommandElement ?: call
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
    fun getCommandStringUpper(word: CaosDefCommandWord): String {
        return getCommandString(word).toUpperCase()
    }

    @JvmStatic
    fun getCommandToken(rvalue: CaosScriptRvalue): CaosScriptIsCommandToken? {
        return rvalue.rvaluePrime?.commandToken
                ?: rvalue.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    @JvmStatic
    fun getCommandToken(rvaluePrime: CaosScriptRvaluePrime): CaosScriptIsCommandToken? {
        return rvaluePrime.getChildOfType(CaosScriptIsCommandToken::class.java)
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptCTarg): CaosScriptIsCommandToken? {
        return element.cKwTarg
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptIsCommandToken): CaosScriptIsCommandToken {
        return element
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptEnumHeaderCommand): CaosScriptIsCommandToken {
        return element.cEnum ?: element.cEcon!!
    }

    @JvmStatic
    fun getInferredType(element: CaosScriptRvaluePrime): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(element) ?: CaosExpressionValueType.UNKNOWN
    }

    @JvmStatic
    fun getCommandString(namedGameVar: CaosScriptNamedGameVar): String {
        return namedGameVar.stub?.type?.token ?: namedGameVar.namedGameVarKw.commandString
    }

    @JvmStatic
    fun getCommandToken(namedGameVar: CaosScriptNamedGameVar) : CaosScriptIsCommandToken {
        return namedGameVar.namedGameVarKw
    }

    @JvmStatic
    fun getCommandString(command: CaosScriptCommandElement): String {
        return when (command.getEnclosingCommandType()) {
            CaosCommandType.COMMAND -> command
                    .getSelfOrParentOfType(CaosScriptCommandCall::class.java)
                    ?.let { getCommandString(it) }
                    ?: command.getChildOfType(CaosScriptIsCommandToken::class.java)
                            ?.let { getCommandString(it) }
            CaosCommandType.RVALUE -> command
                    .getSelfOrParentOfType(CaosScriptRvalue::class.java)
                    ?.let { getCommandString(it) }
            CaosCommandType.LVALUE -> command
                    .getSelfOrParentOfType(CaosScriptLvalue::class.java)
                    ?.let { getCommandString(it) }
            CaosCommandType.CONTROL_STATEMENT -> (command as? CaosScriptIsCommandToken
                    ?: command.getChildOfType(CaosScriptIsCommandToken::class.java))
                    ?.let { getCommandString(it) }
            CaosCommandType.UNDEFINED -> null
        } ?: UNDEF
    }

    @JvmStatic
    fun getCommandStringUpper(command: CaosScriptCommandElement): String? {
        return command.commandString?.toUpperCase()
    }


    @JvmStatic
    fun getCommandStringUpper(command: CaosScriptCAssignment): String? {
        return command.commandString.toUpperCase()
    }

    @JvmStatic
    fun getCommandString(element: CaosScriptRvalue): String? {
        element.stub?.commandString.nullIfUndefOrBlank()?.let {
            return it
        }
        element.stub?.caosVar?.commandString.nullIfUndefOrBlank()?.let {
            return it
        }
        element.incomplete?.text.nullIfEmpty()?.let {
            return it
        }
        element.rvaluePrime?.commandToken?.text.nullIfUndefOrBlank()?.let {
            return it
        }
        return null
    }

    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptRvalue): String? {
        return getCommandString(element)?.toUpperCase()
    }

    @JvmStatic
    fun getCommandString(element: CaosScriptLvalue): String? {
        return element.stub?.commandString
                ?: element.commandToken?.text
    }

    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptLvalue): String? {
        return getCommandString(element)?.toUpperCase()
    }

    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptIsCommandToken): String {
        return element.text.toUpperCase()
    }

    @JvmStatic
    fun getCommandString(call: CaosScriptCommandCall): String {
        return call.stub?.command.nullIfUndefOrBlank()
                ?: getCommandToken(call)
                        ?.text
                        .orEmpty()
                        .split(" ")
                        .filterNot { it.isEmpty() }
                        .joinToString(" ")
    }

    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptCommandCall): String? {
        return element.stub?.commandUpper.nullIfUndefOrBlank() ?: getCommandString(element).toUpperCase()
    }

    @JvmStatic
    fun getCommandString(reps: CaosScriptRepeatStatement): String {
        return reps.repsHeader.cReps.text
    }

    @JvmStatic
    fun getCommandString(element: CaosScriptLoopStatement): String {
        return element.cLoop.text
    }

    private val COMMAND_DEFINITION_KEY = Key<CaosCommand>("com.badahori.creatures.plugins.intellij.agenteering.caos.COMMAND_DEFINITION")

    @JvmStatic
    fun getCommandDefinition(element:CaosScriptCommandElement) : CaosCommand? {
        element.getUserData(COMMAND_DEFINITION_KEY)?.let {
            return it
        }
        val variant = element.variant
                ?: return null
        val token = element.commandString
                ?: return null
        val commandType = element.getEnclosingCommandType()
        val command =  getCommandDefinition(variant, commandType, token)
        if (command != null)
            element.putUserData(COMMAND_DEFINITION_KEY, command)
        return command
    }

    @JvmStatic
    fun getCommandDefinition(element:CaosScriptIsCommandToken) : CaosCommand? {
        element.getUserData(COMMAND_DEFINITION_KEY)?.let {
            return it
        }
        val variant = element.variant
                ?: return null
        val commandType = element.getEnclosingCommandType()
        val command = getCommandDefinition(variant, commandType, element.text)
        if (command != null)
            element.putUserData(COMMAND_DEFINITION_KEY, command)
        return command
    }


    fun getCommandDefinition(element:CaosScriptTokenRvalue) : CaosCommand? = null

    private fun getCommandDefinition(
            variant:CaosVariant,
            commandType:CaosCommandType,
            tokenIn:String
    ) : CaosCommand? {
        val lib = CaosLibs[variant]
        val token = tokenIn.replace(spacesRegex, " ")
        return lib[commandType][token]
    }

    @JvmStatic
    fun getCommandToken(call: CaosScriptCommandCall): CaosScriptIsCommandToken? {
        if (call.firstChild is CaosScriptCommandElement) {
            return (call.firstChild as CaosScriptCommandElement).commandToken
        }
        return call.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: call.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    @JvmStatic
    fun getCommandTokens(call: CaosScriptCommandCall): List<String> {
        return call.stub?.commandTokens?.let { if (it.firstOrNull() == UNDEF) null else it }
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
    fun getCommandString(commandToken: CaosScriptIsCommandToken): String {
        return commandToken.text
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
        /*val group = element.stub?.varGroup
        if (group != null)
            return group*/
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
        /*return element.stub?.varIndex
                ?: element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()*/
        return element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()
    }

    @JvmStatic
    fun isVariant(element: CaosScriptIsCommandToken, variants: List<CaosVariant>, strict: Boolean): Boolean {
        val thisVariant = (element as? CaosScriptCompositeElement)?.containingCaosFile?.variant
                ?: return !strict
        if (thisVariant == CaosVariant.UNKNOWN) {
            return !strict
        }
        return thisVariant in variants
    }

    @JvmStatic
    fun isVariant(element: CaosScriptCompositeElement, variants: List<CaosVariant>, strict: Boolean): Boolean {
        val thisVariant = element.containingCaosFile?.variant
                ?: return !strict
        if (thisVariant == CaosVariant.UNKNOWN)
            return !strict
        return thisVariant in variants
    }

    @JvmStatic
    fun getName(element: CaosScriptIsCommandToken): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: CaosScriptIsCommandToken, newName: String): PsiElement {
        CaosScriptPsiElementFactory.createCommandTokenElement(element.project, newName)?.let {
            element.replace(it)
        }
        return element
    }

    @JvmStatic
    fun getName(element: CaosScriptVarToken): String {
        return element.text.toUpperCase()
    }

    @JvmStatic
    fun setName(element: CaosScriptVarToken, newName: String): PsiElement {
        CaosScriptPsiElementFactory.createCommandTokenElement(element.project, newName)?.let {
            element.replace(it)
        }
        return element
    }

    @JvmStatic
    fun getKey(element: CaosScriptNamedGameVar): CaosVar {
        return element.stub?.key ?: element.rvalue?.toCaosVar() ?: CaosVar.CaosVarNone
    }

    @JvmStatic
    fun getName(element: CaosScriptNamedGameVar): String? {
        val value = element.key
        return if (value is CaosVar.CaosLiteral.CaosString) {
            value.value
        } else {
            value.text
        }
    }

    @JvmStatic
    fun getName(element: CaosScriptSubroutine): String {
        return element.stub?.name ?: element.subroutineHeader.subroutineName?.name
        ?: element.subroutineHeader.subroutineName?.text ?: ""
    }

    @JvmStatic
    fun setName(element: CaosScriptNamedGameVar, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory
                .createStringRValue(element.project, newName, '"')
        return element.rvalue?.replace(newNameElement) ?: element
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
    fun getReference(element: CaosScriptRvalue): CaosScriptExpressionReference {
        return CaosScriptExpressionReference(element)
    }

    @JvmStatic
    fun getReference(element: CaosScriptEventNumberElement): CaosScriptEventNumberReference {
        return CaosScriptEventNumberReference(element)
    }

    @JvmStatic
    fun getName(element: CaosScriptEventNumberElement): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: CaosScriptEventNumberElement, newName: String): PsiElement {
        return element
    }

    @JvmStatic
    fun getName(element: CaosScriptRvalue): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: CaosScriptRvalue, newName: String): PsiElement {
        return element
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
        return assignment.rvalue?.toCaosVar()
    }

    @JvmStatic
    fun getRVar(assignment: CaosScriptCAssignment): CaosVar? {
        assignment.stub?.rvalue?.let {
            return it
        }
        assignment.rvalue?.let {
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
    fun toCaosVar(rvalue: CaosScriptRvalueLike): CaosVar {
        if (rvalue !is CaosScriptRvalue) {
            // If not true rvalue, then can only be token_rvalue.
            // Simply return text as token
            return CaosVar.CaosLiteral.CaosToken(rvalue.text)
        }
        (rvalue as? CaosScriptRvalue)?.stub?.caosVar?.let {
            return it
        }
        (rvalue as? CaosScriptTokenRvalue)?.stub?.caosVar?.let {
            return it
        }
        if (!DumbService.isDumb(rvalue.project)) {
            val variable: CaosScriptIsVariable? = rvalue.varToken as? CaosScriptIsVariable
                    ?: rvalue.namedGameVar
            if (variable != null) {
                CaosScriptInferenceUtil.getInferredValue(variable)?.let {
                    return it
                }
            }
        }
        (rvalue.varToken)?.let {
            return toCaosVar(it)
        }

        rvalue.commandToken?.let {
            if (!DumbService.isDumb(it.project)) {
                return CaosVar.CaosCommandCall(it.text, getReturnType(it, rvalue.arguments.size))
            }
            return CaosVar.CaosCommandCall(it.text)
        }

        rvalue.namedGameVar?.let {
            return it.toCaosVar() ?: CaosVar.CaosLiteralVal
        }

        rvalue.rvaluePrime?.let { rvaluePrime ->
            return toCaosVar(rvaluePrime)
        }

        rvalue.number?.let { number ->
            number.float?.let {
                return CaosVar.CaosLiteral.CaosFloat(it.text.toFloat())
            }
            number.int?.let {
                return try {
                    CaosVar.CaosLiteral.CaosInt(it.text.toLong())
                } catch (e: Exception) {
                    CaosVar.CaosLiteral.CaosInt(Long.MAX_VALUE)
                }
            }
        }

        rvalue.c1String?.let {
            return CaosVar.CaosLiteral.CaosC1String(it.stringValue)
        }
        rvalue.quoteStringLiteral?.let {
            return CaosVar.CaosLiteral.CaosString(it.stringValue)
        }

        rvalue.token?.text?.let {
            return CaosVar.CaosLiteral.CaosToken(it)
        }

        rvalue.animationString?.let {
            return CaosVar.CaosLiteral.CaosAnimationString(it.text, getAnimation(rvalue))
        }

        rvalue.byteString?.text?.let {
            return CaosVar.CaosLiteral.CaosByteString(getStringValue(rvalue) ?: "", rvalue.byteStringArray
                    ?: emptyList())
        }
        rvalue.namedGameVar?.let {
            return it.toCaosVar() ?: CaosVar.CaosLiteralVal
        }

        return CaosVar.CaosVarNone
    }

    @JvmStatic
    fun toCaosVar(rvaluePrime: CaosScriptRvaluePrime): CaosVar {
        rvaluePrime.stub?.caosVar?.let { return it }
        return rvaluePrime.commandToken?.let {
            val commandString = it.commandString
            if (commandString.toLowerCase() == "null")
                return CaosVar.CaosVarNull
            if (!DumbService.isDumb(rvaluePrime.project)) {
                return CaosVar.CaosCommandCall(commandString, getReturnType(it, rvaluePrime.getChildrenOfType(CaosScriptArgument::class.java).size))
            }
            return CaosVar.CaosCommandCall(commandString)
        } ?: CaosVar.CaosLiteralVal
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
                val validTypes = when (varToken.containingCaosFile?.variant) {
                    CaosVariant.C1 -> "[varX,obvX]"
                    CaosVariant.C2 -> "[varX,vaXX,obvX,ovXX]"
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
        (lvalue.varToken)?.let {
            return toCaosVar(it)
        }

        (lvalue.namedGameVar)?.let { parent ->
            val varName = when (parent.commandStringUpper) {
                "NAME" -> CaosVar.CaosNamedGameVar.NameVar(parent.rvalue?.text ?: UNDEF)
                "MAME" -> CaosVar.CaosNamedGameVar.MameVar(parent.rvalue?.text ?: UNDEF)
                "GAME" -> CaosVar.CaosNamedGameVar.GameVar(parent.rvalue?.text ?: UNDEF)
                "EAME" -> CaosVar.CaosNamedGameVar.EameVar(parent.rvalue?.text ?: UNDEF)
                else -> null
            }
            varName?.let {
                return it
            }
        }
        (lvalue.lKwRR)?.let {
            if (it.kGame != null) {
                val arguments = lvalue.arguments
                if (arguments.size == 2) {
                    return try {
                        val first = arguments[0].text.toInt()
                        val second = arguments[1].text.toInt()
                        CaosVar.CaosNamedGameVar.C2GameVar(first, second)
                    } catch (e: Exception) {
                        CaosVar.CaosNamedGameVar.C2GameVar(-1, -1)
                    }
                }
            }
        }
        lvalue.commandToken?.let {
            if (!DumbService.isDumb(it.project)) {
                return CaosVar.CaosCommandCall(it.text, getReturnType(it, lvalue.arguments.size))
            }
            return CaosVar.CaosCommandCall(it.text)
        }
        return CaosVar.CaosLiteralVal
    }

    private fun getReturnType(token: CaosScriptIsCommandToken, numberOfArgs: Int): CaosExpressionValueType {
        val variant = token.variant
                ?: return CaosExpressionValueType.UNKNOWN
        val commandType = token.getEnclosingCommandType()
        return CaosLibs[variant][commandType][token.commandString]?.returnType
                ?: CaosExpressionValueType.UNKNOWN
    }

    @JvmStatic
    fun getByteStringArray(expression: CaosScriptRvalue): List<Int>? {
        val variant = expression.containingCaosFile?.variant
                ?: return null
        expression.byteString?.let { stringValue ->
            if (variant.isNotOld) {
                return try {
                    stringValue.byteStringPoseElementList.map {
                        it.text.toInt()
                    }
                } catch (e: Exception) {
                    null
                }
            }
            return stringValue.byteStringPoseElementList.firstOrNull()?.text.orEmpty().toCharArray().map { ("$it").toInt() }
        }
        return null
    }

    @JvmStatic
    fun getAnimation(expression: CaosScriptRvalue): CaosAnimation? {
        val variant = expression.containingCaosFile?.variant
                ?: return null
        if (variant.isNotOld) {
            val poseList = expression.byteStringArray
                    ?: return null
            if (poseList.lastOrNull() == 255) {
                return CaosAnimation.Animation(poseList.subList(0, poseList.lastIndex), true, 0)
            } else if (poseList.getOrNull(poseList.lastIndex - 1) == 255) {
                return CaosAnimation.Animation(poseList.subList(0, poseList.lastIndex - 1), true, poseList.last())
            }
        }
        val stringValue = expression.stringValue
                ?: return null
        if (!"[0-9]+R?".toRegex().matches(stringValue)) {
            return null
        }
        val charsRaw = expression.text.toCharArray().toList()
        val poses: List<Int>
        val repeats = charsRaw.last().toUpperCase() == 'R'
        val repeatsFrom = if (repeats) 0 else null
        poses = if (repeats) {
            charsRaw.subList(0, charsRaw.lastIndex).map { it.toInt() }
        } else {
            charsRaw.map { it.toInt() }
        }
        return CaosAnimation.Animation(poses, repeats, repeatsFrom)
    }

    private fun getCVPlusAnimation(stringValue: String): CaosAnimation? {
        if ("[^0-9 ]".toRegex().matches(stringValue)) {
            return null
        }
        val posesRaw = stringValue.split(" ").map { it.toInt() }
        var repeats = false
        var repeatsFrom: Int? = null
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
                } else if (posesRaw[posesRaw.lastIndex - 1] == 255) {
                    repeats = true
                    repeatsFrom = posesRaw.last()
                }
            }
        }

        val poses: List<Int> = if (repeats) {
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
    fun getCreatureAnimation(expression: CaosScriptRvalue): CaosAnimation? {
        val variant = expression.containingCaosFile?.variant
                ?: return null
        val stringValue = expression.stringValue
                ?: return null
        if (stringValue.isEmpty())
            return CaosAnimation.Animation(emptyList(), false, null)
        if (variant.isNotOld) {
            return getCVPlusAnimation(stringValue)
        }
        val charsRawTemp = stringValue.toCharArray().toList()
        val repeats = charsRawTemp.last().toUpperCase() == 'R'
        val repeatsFrom = if (repeats) 0 else null
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
            "${charsRaw[base]}${charsRaw[base + 1]}${charsRaw[base + 2]}".toInt()
        }
        return CaosAnimation.Animation(poses, repeats, repeatsFrom)
    }

    @JvmStatic
    fun getToken(element:CaosScriptRvalue) : CaosScriptToken? {
        return null
    }

    @JvmStatic
    fun getStringValue(string: CaosScriptC1String): String {
        return string.textLiteral?.text ?: ""
    }

    @JvmStatic
    fun getStringValue(stringIn: CaosScriptQuoteStringLiteral): String {
        return stringIn.stringText?.text ?: ""
    }

    @JvmStatic
    fun getStringValue(stringIn: CaosScriptCharacter): String {
        return stringIn.charChar?.text ?: ""
    }

    @JvmStatic
    fun isClosed(stringIn: CaosScriptQuoteStringLiteral): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_DOUBLE_QUOTE
    }

    @JvmStatic
    fun isClosed(stringIn: CaosScriptCharacter): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_SINGLE_QUOTE
    }

    @JvmStatic
    fun isClosed(stringIn: CaosScriptC1String): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_CLOSE_BRACKET
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptLvalue): CaosExpressionValueType {
        return CaosExpressionValueType.VARIABLE
    }

    @JvmStatic
    fun getIndex(commandToken: CaosScriptIsCommandToken): Int {
        return 0
    }

    @JvmStatic
    fun getIndex(element: CaosScriptLvalue): Int {
        var lastParent: PsiElement? = element
        while (lastParent != null && lastParent.parent !is CaosScriptCommandElement) {
            lastParent = lastParent.parent
        }
        val parent = lastParent?.parent as? CaosScriptCommandElement
                ?: return 0
        if (lastParent !is CaosScriptArgument)
            return 0
        return parent.arguments.indexOf(lastParent)
    }


    @JvmStatic
    fun getIndex(element: CaosScriptArgument): Int {
        var lastParent: PsiElement? = element
        while (lastParent != null && lastParent.parent !is CaosScriptCommandElement) {
            lastParent = lastParent.parent
        }
        val parent = lastParent?.parent as? CaosScriptCommandElement
                ?: return 0
        if (lastParent !is CaosScriptArgument) {
            return when (element.parent) {
                is CaosScriptFamily -> 0
                is CaosScriptGenus -> 1
                is CaosScriptSpecies -> 2
                else -> 0
            }
        }
        return parent.arguments.indexOf(lastParent)
    }

    @JvmStatic
    fun getInferredType(varToken: CaosScriptIsVariable): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(varToken)
    }

    @JvmStatic
    fun getInferredType(element: CaosScriptRvalue): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(element)
    }

    @JvmStatic
    fun getInferredType(element: CaosScriptLvalue): CaosExpressionValueType {
        element.varToken?.let {
            return CaosScriptInferenceUtil.getInferredType(it)
        }
        val caosVar = toCaosVar(element)
        return (caosVar as? CaosVar.CaosCommandCall)?.returnType ?: caosVar.simpleType
    }


    @JvmStatic
    fun getInferredType(element: CaosScriptTokenRvalue): CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
    }


    @JvmStatic
    fun getArguments(command: CaosScriptCommandElement): List<CaosScriptArgument> {
        val base = (command as? CaosScriptRvalue)?.rvaluePrime ?: command
        return base.getChildrenOfType(CaosScriptArgument::class.java)
    }

    @JvmStatic
    fun getArguments(command: CaosScriptEnumHeaderCommand): List<CaosScriptArgument> {
        command.classifier?.let {
            return listOfNotNull(
                    it.family.rvalue,
                    it.genus?.rvalue,
                    it.species?.rvalue
            )
        }
        return listOfNotNull(command.rvalue as? CaosScriptArgument)
    }

    @JvmStatic
    fun getArguments(command: CaosScriptEscnHeader): List<CaosScriptArgument> {
        command.classifier?.let {
            return listOfNotNull(
                    it.family.rvalue,
                    it.genus?.rvalue,
                    it.species?.rvalue
            )
        }
        return listOf()
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
        return element.arguments.map {
            it.toCaosVar()
        }
    }

    @JvmStatic
    fun toCaosVar(element: CaosScriptSubroutineName): CaosVar {
        return CaosVar.CaosLiteral.CaosToken(element.text)
    }

    @JvmStatic
    fun getIndex(element: CaosScriptSubroutineName): Int {
        return 0
    }

    @JvmStatic
    fun getExpectedType(element: CaosScriptSubroutineName): CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
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
    fun getBlockType(script: CaosScriptRemovalScript): CaosScriptBlockType = CaosScriptBlockType.RSCR

    @JvmStatic
    fun getBlockType(script: CaosScriptInstallScript): CaosScriptBlockType = CaosScriptBlockType.ISCR

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
        return try {
            script.stub?.family ?: script.classifier?.family?.text?.toInt() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    @JvmStatic
    fun getGenus(script: CaosScriptEventScript): Int {
        return try {
            script.stub?.genus ?: script.classifier?.genus?.text?.toInt() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    @JvmStatic
    fun getSpecies(script: CaosScriptEventScript): Int {
        return try {
            script.stub?.species ?: script.classifier?.species?.text?.toInt() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    @JvmStatic
    fun getEventNumber(script: CaosScriptEventScript): Int {
        return try {
            script.stub?.eventNumber ?: script.eventNumberElement?.text?.toInt() ?: -1
        } catch (e: Exception) {
            -1
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
    fun getVarType(element: CaosScriptNamedGameVar): CaosScriptNamedGameVarType {
        element.namedGameVarKw.let {
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

    @JvmStatic
    fun isClosed(element: CaosScriptDoifStatementStatement): Boolean {
        return element.parent.lastChild != element
    }

    @JvmStatic
    fun isClosed(element: CaosScriptElseIfStatement): Boolean {
        return element.parent.lastChild != element
    }

    @JvmStatic
    fun isClosed(element: CaosScriptElseStatement): Boolean {
        return element.parent.lastChild != element
    }

    @JvmStatic
    fun isClosed(element: CaosScriptEnumNextStatement): Boolean {
        return element.cNext != null || element.cNscn != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptEnumSceneryStatement): Boolean {
        return element.cNext != null || element.cNscn != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptEventScript): Boolean {
        return element.scriptTerminator != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptInstallScript): Boolean {
        return element.scriptTerminator != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptMacro): Boolean {
        return element.scriptTerminator != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptRepeatStatement): Boolean {
        return element.cRepe != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptRemovalScript): Boolean {
        return element.scriptTerminator != null
    }

    @JvmStatic
    fun isClosed(element: CaosScriptSubroutine): Boolean {
        return element.retnKw != null
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
    fun isNumeric(expression: CaosScriptRvalue): Boolean {
        return expression.number?.let {
            it.int != null || it.float != null
        } ?: return false
    }

    @JvmStatic
    fun isInt(expression: CaosScriptRvalue): Boolean {
        return expression.number?.int != null
    }

    @JvmStatic
    fun isFloat(expression: CaosScriptRvalue): Boolean {
        return expression.number?.float != null
    }

    @JvmStatic
    fun getIntValue(expression: CaosScriptRvalue): Int? {
        return expression.number?.int?.text?.toInt()
    }

    @JvmStatic
    fun getFloatValue(expression: CaosScriptRvalue): Float? {
        return expression.number?.let {
            (it.int ?: it.float)?.text?.toFloat()
        }
    }

    @JvmStatic
    fun isString(expression: CaosScriptRvalue): Boolean {
        return expression.c1String != null
                || expression.byteString != null
                || expression.animationString != null
                || expression.quoteStringLiteral != null
                || expression.token != null
    }


    @JvmStatic
    fun isC1String(expression: CaosScriptRvalueLike): Boolean {
        return expression.c1String != null
    }


    @JvmStatic
    fun isByteString(expression: CaosScriptRvalueLike): Boolean {
        return expression.byteString != null
    }

    @JvmStatic
    fun isQuoteString(expression: CaosScriptRvalueLike): Boolean {
        return expression.quoteStringLiteral != null
    }

    @JvmStatic
    fun isToken(expression: CaosScriptRvalueLike): Boolean {
        return expression.token != null
    }

    @JvmStatic
    fun getStringValue(expression: CaosScriptRvalueLike): String? {
        (expression.c1String ?: expression.animationString ?: expression.byteString)?.let {
            return it.text.trim('[', ']')
        }
        return expression.token?.text ?: expression.quoteStringLiteral?.stringValue
    }

    @JvmStatic
    fun getParameterListValue(element: CaosScriptRvalueLike): CaosDefValuesListValueStruct? {
        val containingCommand = element.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return null


        // Values for lists can only be inferred by literal values
        if (element !is CaosScriptRvalue || !(element.isNumeric || element.isString))
            return null

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
                ?.getSelfOrParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement::class.java)
                ?: return null

        // Find the type def list name
        val valuesList = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(element.index)
                ?.type
                ?.valuesList
                ?: return null

        // Get variant
        val variant = element.containingCaosFile?.variant
                ?: return null
        // Find list in index
        val list = CaosDefValuesListElementsByNameIndex
                .Instance[valuesList, element.project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
                ?: return null
        // Get actual value for literal in list
        return list.valuesListValues.firstOrNull {
            when (it.equality) {
                ValuesListEq.EQUAL -> it.key == element.text
                ValuesListEq.NOT_EQUAL -> it.key != element.text
                ValuesListEq.GREATER_THAN -> try {
                    element.text.toInt() > it.key.replace("[^0-9]".toRegex(), "").toInt()
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    @JvmStatic
    fun getRndvIntRange(element: CaosScriptCRndv): Pair<Int?, Int?> {
        element.stub?.let {
            return Pair(it.min, it.max)
        }
        val val1 = try {
            element.minElement?.text?.toInt()
        } catch (e: Exception) {
            null
        }
        val val2 = try {
            element.maxElement?.text?.toInt()
        } catch (e: Exception) {
            null
        }
        return Pair(val1, val2)
    }

    @JvmStatic
    fun isRndvValuesInOrder(element: CaosScriptCRndv, onNull: Boolean): Boolean {
        val values = getRndvIntRange(element)
        val min = values.first ?: return onNull
        val max = values.second ?: return onNull
        return min < max
    }

    @JvmStatic
    fun getWidth(dimensions: CaosScriptPictDimensionLiteral): Int {
        dimensions.text.toCharArray().let {
            if (it.isEmpty())
                return -1
            return it[0].toInt()
        }
    }

    @JvmStatic
    fun getHeight(dimensions: CaosScriptPictDimensionLiteral): Int {
        dimensions.text.toCharArray().let {
            if (it.size < 3)
                return -1
            return it[2].toInt()
        }
    }

    @JvmStatic
    fun getDimensions(dimensions: CaosScriptPictDimensionLiteral): Pair<Int, Int> {
        dimensions.text.toCharArray().let {
            if (it.size < 3)
                return Pair(-1, -1)
            return Pair(it[0].toInt(), it[2].toInt())
        }
    }

    @JvmStatic
    fun getLastAssignment(varToken: CaosScriptVarToken): CaosScriptIsCommandToken? {
        val lastPos = varToken.startOffset
        val text = varToken.text
        val scope = varToken.getParentOfType(CaosScriptHasCodeBlock::class.java)?.scope()
        val assignment = PsiTreeUtil.collectElementsOfType(varToken.containingFile, CaosScriptCAssignment::class.java).filter {
            it.endOffset < lastPos && it.lvalue?.text == text && it.sharesScope(scope)
        }.maxBy {
            it.startOffset
        } ?: return null
        val rvalue = (assignment.arguments.lastOrNull() as? CaosScriptRvalue)
                ?: return null
        rvalue.rvaluePrime?.let { return it.commandToken }
        return rvalue.varToken?.let {
            getLastAssignment(it)
        }
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptCRtar): CaosScriptCKwRtar {
        return element.cKwRtar
    }

    @JvmStatic
    fun isEquivalentTo(element: CaosScriptVarToken, another: PsiElement): Boolean {
        if (another is CaosDefCommandWord) {
            return element.varGroup.value.equalsIgnoreCase(another.text)
        }
        return another is CaosScriptVarToken && another.text.equalsIgnoreCase(element.text)
    }

    @JvmStatic
    fun getDescriptiveText(family: CaosScriptFamily): String {
        (family.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        (family.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        return "Family: ${family.text}"
    }

    @JvmStatic
    fun getDescriptiveText(genus: CaosScriptGenus): String {
        (genus.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        (genus.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        return "Genus: ${genus.text}"
    }

    @JvmStatic
    fun getDescriptiveText(species: CaosScriptSpecies): String {
        (species.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        (species.parent?.parent as? CaosScriptEventScript)?.let { parent ->
            return getDescriptiveText(parent)
        }
        return "Species: ${species.text}"
    }

    @JvmStatic
    fun getDescriptiveText(classifier: CaosScriptClassifier, prefixIn: String? = null): String {
        val classifierText = "${classifier.family} ${classifier.genus} ${classifier.species}"
        return prefixIn.nullIfEmpty()?.let { prefix ->
            return if (prefix.endsWith(" "))
                "$prefix$classifierText"
            else
                "$prefix $classifierText"

        } ?: classifierText
    }

    @JvmStatic
    fun getDescriptiveText(eventScript: CaosScriptEventScript): String {
        val classifierText = eventScript.classifier?.let { classifier ->
            getDescriptiveText(classifier)
        } ?: return "Event Script"
        val eventNumber = eventScript.eventNumber
        if (eventNumber < 0)
            return "scrp $classifierText {?}"
        return "srcp $classifierText $eventNumber"
    }

    @JvmStatic
    fun getCommandToken(element: CaosScriptCSsfc): CaosScriptIsCommandToken {
        return element.cKwSsfc
    }
}

@Suppress("UNCHECKED_CAST")
private fun <PsiT : PsiElement> CaosScriptCompositeElement.getSelfOrParentOfType(clazz: Class<PsiT>): PsiT? {
    if (clazz.isInstance(this))
        return this as PsiT
    return this.getParentOfType(clazz)
}


val PsiElement.elementType: IElementType get() = node.elementType


@Suppress("SpellCheckingInspection")
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

fun PsiElement.getEnclosingCommandType(): CaosCommandType {

    // If expression or Equality, it can only be Rvalue
    // Getting base element will skip this possibility, so check first
    if (isOrHasParentOfType(CaosScriptRvalue::class.java) || this.hasParentOfType(CaosScriptEqualityExpression::class.java))
        return CaosCommandType.RVALUE
    val parent: PsiElement? = getSelfOrParentOfType(CaosScriptBaseCommandElement::class.java)
    if (parent == null || (parent !is CaosScriptCommandElement || parent is CaosScriptNamedGameVar)) {
        if (isOrHasParentOfType(CaosScriptIncomplete::class.java)) {
            return CaosCommandType.COMMAND
        }
        if (isOrHasParentOfType(CaosScriptDoifStatement::class.java))
            return CaosCommandType.CONTROL_STATEMENT
        if (isOrHasParentOfType(CaosScriptEnumHeaderCommand::class.java))
            return CaosCommandType.CONTROL_STATEMENT
        if (isOrHasParentOfType(CaosScriptEnumSceneryStatement::class.java))
            return CaosCommandType.CONTROL_STATEMENT
        if (isOrHasParentOfType(CaosScriptRepsHeader::class.java))
            return CaosCommandType.CONTROL_STATEMENT
        if (isOrHasParentOfType(CaosScriptLoopStatement::class.java))
            return CaosCommandType.CONTROL_STATEMENT
        if (this is CaosScriptIsCommandToken || children.firstOrNull() is CaosScriptIsCommandToken) {
            return CaosCommandType.COMMAND
        }
        LOGGER.info("Failed to understand command type with value: $text. ${parent?.text?.let { "With parent: $it" } ?: "Without parent"}")
        return CaosCommandType.UNDEFINED
    }
    return when (parent) {
        is CaosScriptLvalue -> CaosCommandType.LVALUE
        is CaosScriptRvalueLike -> CaosCommandType.RVALUE
        is CaosScriptCommandCall -> CaosCommandType.COMMAND
        is CaosScriptRvaluePrime -> CaosCommandType.RVALUE
        is CaosScriptEnumNextStatement -> CaosCommandType.CONTROL_STATEMENT
        is CaosScriptCAssignment -> CaosCommandType.COMMAND
        is CaosScriptCGsub -> CaosCommandType.COMMAND
        is CaosScriptCRndv -> CaosCommandType.COMMAND
        is CaosScriptCTarg -> CaosCommandType.COMMAND
        is CaosScriptEscnHeader -> CaosCommandType.CONTROL_STATEMENT
        is CaosScriptRepsHeader -> CaosCommandType.CONTROL_STATEMENT
        is CaosScriptSubroutineHeader -> CaosCommandType.CONTROL_STATEMENT
        else ->  {
            LOGGER.info("Failed to understand command type with a command element parent ${parent.commandStringUpper};")
            parent.parent?.getEnclosingCommandType() ?: CaosCommandType.UNDEFINED
        }
    }
}

enum class CaosCommandType(val value: String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    CONTROL_STATEMENT("Control Statement"),
    UNDEFINED("???");
}

fun <PsiT : PsiElement> PsiElement.getSelfOrParentOfType(parentClass: Class<PsiT>): PsiT? {
    if (parentClass.isInstance(this))
        return parentClass.cast(this)
    return PsiTreeUtil.getParentOfType(this, parentClass)
}

fun <PsiT : PsiElement> PsiElement.getParentOfType(parentClass: Class<PsiT>): PsiT? {
    return PsiTreeUtil.getParentOfType(this, parentClass)
}

fun <PsiT : PsiElement> PsiElement.getChildOfType(parentClass: Class<PsiT>): PsiT? {
    return PsiTreeUtil.getChildOfType(this, parentClass)
}

fun <PsiT : PsiElement> PsiElement.getChildrenOfType(parentClass: Class<PsiT>): List<PsiT> {
    return PsiTreeUtil.getChildrenOfType(this, parentClass)?.filterNotNull().orEmpty()
}

fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass: Class<PsiT>): Boolean {
    if (parentClass.isInstance(this))
        return true
    return PsiTreeUtil.getParentOfType(this, parentClass) != null
}

val spacesRegex = "\\s+".toRegex()

val PsiElement.case: Case
    get() {
        val chars = this.text.toCharArray()
        if (chars.size < 2)
            return Case.LOWER_CASE
        if (chars[0] == chars[0].toLowerCase()) {
            return Case.LOWER_CASE
        }
        if (chars[1] == chars[1].toLowerCase()) {
            return Case.CAPITAL_FIRST
        }
        return Case.UPPER_CASE
    }

fun String?.nullIfUndefOrBlank(): String? {
    return if (this == null || this == UNDEF || this.isBlank())
        null
    else
        this
}

val CaosScriptCommandLike.commandStringUpper: String? get() = commandString?.toUpperCase()

val PsiElement.endOffsetInParent: Int
    get() {
        return startOffsetInParent + textLength
    }


val PsiElement.endOffset get() = textRange.endOffset
val PsiElement.startOffset get() = textRange.startOffset


fun getDepth(element: PsiElement): Int {
    var depth = 0
    var parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
    while (parent != null) {
        depth++
        parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
    }
    return depth
}
