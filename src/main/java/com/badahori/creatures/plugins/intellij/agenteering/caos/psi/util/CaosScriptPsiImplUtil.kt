package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.documentation.CaosScriptPresentationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

const val UNDEF = "{UNDEF}"

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
    fun getCommandToken(namedGameVar: CaosScriptNamedGameVar): CaosScriptIsCommandToken {
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
    fun getCommandDefinition(element: CaosScriptCommandElement): CaosCommand? {
        element.getUserData(COMMAND_DEFINITION_KEY)?.let {
            return it
        }
        val variant = element.variant
                ?: return null
        val token = element.commandString
                ?: return null
        val commandType = element.getEnclosingCommandType()
        val command = getCommandDefinition(variant, commandType, token)
        if (command != null)
            element.putUserData(COMMAND_DEFINITION_KEY, command)
        return command
    }

    @JvmStatic
    fun getCommandDefinition(element: CaosScriptIsCommandToken): CaosCommand? {
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


    fun getCommandDefinition(element: CaosScriptTokenRvalue): CaosCommand? = null

    private fun getCommandDefinition(
            variant: CaosVariant,
            commandType: CaosCommandType,
            tokenIn: String
    ): CaosCommand? {
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
    fun getName(element: CaosScriptNamedGameVar): String? {
        return element.stub?.key ?: element.rvalue?.text
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
    fun getInferredType(rvalue: CaosScriptRvalueLike): CaosExpressionValueType {
        if (rvalue !is CaosScriptRvalue) {
            // If not true rvalue, then can only be token_rvalue.
            // Simply return text as token
            return CaosExpressionValueType.TOKEN
        }
        (rvalue as? CaosScriptRvalue)?.stub?.type?.let {
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
            return CaosExpressionValueType.VARIABLE
        }

        rvalue.namedGameVar?.let {
            return CaosExpressionValueType.VARIABLE
        }

        rvalue.commandDefinition?.let {
            return it.returnType
        }

        rvalue.number?.let { number ->
            number.float?.let {
                return CaosExpressionValueType.FLOAT
            }
            number.int?.let {
                return CaosExpressionValueType.INT
            }
        }

        rvalue.c1String?.let {
            return CaosExpressionValueType.C1_STRING
        }
        rvalue.quoteStringLiteral?.let {
            return CaosExpressionValueType.STRING
        }

        rvalue.token?.text?.let {
            return CaosExpressionValueType.TOKEN
        }

        rvalue.animationString?.let {
            return CaosExpressionValueType.ANIMATION
        }

        rvalue.byteString?.text?.let {
            return CaosExpressionValueType.BYTE_STRING
        }
        rvalue.namedGameVar?.let {
            return CaosExpressionValueType.VARIABLE
        }

        return CaosExpressionValueType.UNKNOWN
    }

    @JvmStatic
    fun getInferredType(subroutineName:CaosScriptSubroutineName) : CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
    }

    @JvmStatic
    fun getReturnType(rvaluePrime: CaosScriptRvaluePrime): CaosExpressionValueType {
        rvaluePrime.stub?.caosVar?.let { return it }
        return rvaluePrime.commandToken?.let {
            val commandString = it.commandString
            if (commandString.toLowerCase() == "null")
                return CaosExpressionValueType.NULL
            rvaluePrime.variant?.let { variant ->
                CaosLibs[variant][CaosCommandType.RVALUE][commandString]
                        ?.returnType
            }
        } ?: CaosExpressionValueType.UNKNOWN
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
    fun getToken(element: CaosScriptRvalue): CaosScriptToken? {
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
        element.namedGameVar?.let {
            return CaosExpressionValueType.VARIABLE
        }
        return element.commandDefinition?.returnType
                ?: CaosExpressionValueType.UNKNOWN
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
    fun getArgumentValues(command: CaosScriptCommandCall): List<CaosExpressionValueType> {
        return command.stub?.argumentValues ?: command.arguments.map { it.inferredType }
    }

    @JvmStatic
    fun getArgumentValues(rvalue: CaosScriptRvalue): List<CaosExpressionValueType> {
        return rvalue.stub?.argumentValues ?: rvalue.arguments.map { it.inferredType }
    }

    @JvmStatic
    fun getArgumentValues(lvalue: CaosScriptLvalue): List<CaosExpressionValueType> {
        return lvalue.stub?.argumentValues ?: lvalue.arguments.map { it.inferredType }
    }

    @JvmStatic
    fun getArgumentValues(element: CaosScriptCommandElement): List<CaosExpressionValueType> {
        return element.arguments.map {
            it.inferredType
        }
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
        assignment.cKwAssignAlter?.let {
            it.kNegv?.let { return CaosOp.NEGV }
            it.kNotv?.let { return CaosOp.NOTV }
            it.kAbsv?.let { return CaosOp.ABSV }
        }
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
    fun getKey(element: CaosScriptNamedGameVar) : String? {
        return element.rvalue?.stringValue
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
    fun getParameterValuesListValue(element: CaosScriptRvalueLike): CaosValuesListValue? {
        val containingCommand = element.parent as? CaosScriptCommandElement
                ?: return null

        // Values for lists can only be inferred by literal values
        if (element !is CaosScriptRvalue || !(element.isNumeric || element.isString))
            return null

        val commandDefinition = containingCommand.commandDefinition
                ?: return null
        val index = element.index

        // Get parameter if any is available
        val parameter = commandDefinition.parameters.getOrNull(index)
        if (parameter == null) {
            LOGGER.severe("Command for Rvalue was resolved to incorrect command versions, or BNF does not match command definition for ${commandDefinition.command}")
            return null
        }

        // Get variant for values list filter
        val variant = element.variant
                ?: return null

        // Get values list for variant
        val valuesList = parameter.valuesList[variant]
                ?: return null

        // Get value if any based on element text
        return valuesList[element.text]
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
    // Next Element to check
    var element:PsiElement? = this

    // Loop through element and parents to
    // find first instance of a command type element
    while (element != null) {
        return when (element) {
            is CaosScriptCommandCall -> CaosCommandType.COMMAND
            is CaosScriptRvaluePrime -> CaosCommandType.RVALUE
            is CaosScriptRvalue -> CaosCommandType.RVALUE
            is CaosScriptLvalue -> CaosCommandType.LVALUE
            is CaosScriptEqualityExpression -> CaosCommandType.RVALUE
            is CaosScriptHasCodeBlock -> CaosCommandType.CONTROL_STATEMENT
            else -> {
                element = element.parent
                continue
            }
        }
    }
    return CaosCommandType.UNDEFINED
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

val PsiElement.isFolded:Boolean get() {
    val editor = editor
            ?: return false
    val startOffset = startOffset
    return editor.foldingModel.allFoldRegions.any {
        !it.isExpanded && startOffset in it.startOffset .. it.endOffset
    }
}
val PsiElement.isNotFolded:Boolean get() {
    val editor = editor
            ?: return false
    val startOffset = startOffset
    return editor.foldingModel.allFoldRegions.none {
        !it.isExpanded && startOffset in it.startOffset .. it.endOffset
    }
}