package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.documentation.CaosScriptPresentationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
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
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import icons.CaosScriptIcons
import javax.swing.Icon


const val UNDEF = "{UNDEF}"

/**
 * Object for assigning additional commands to PSI Elements during code generation
 */
@Suppress("UNUSED_PARAMETER", "SpellCheckingInspection")
object CaosScriptPsiImplUtil {

    /**
     * Gets the enclosing code block for a code block
     */
    @JvmStatic
    fun getCodeBlock(codeBlock: CaosScriptCodeBlock): CaosScriptCodeBlock = codeBlock

    // ============================== //
    // ====== getCommandToken() ===== //
    // ============================== //

    /**
     * Gets command token for RValue
     */
    @JvmStatic
    fun getCommandToken(rvalue: CaosScriptRvalue): CaosScriptIsCommandToken? {
        return rvalue.rvaluePrime?.commandToken
        // TODO an incomplete command token still be returned
                ?: rvalue.rvaluePrefixIncompletes
    }

    /**
     * Gets command token for rvalue prime
     */
    @JvmStatic
    fun getCommandToken(rvaluePrime: CaosScriptRvaluePrime): CaosScriptIsCommandToken? {
        return rvaluePrime.firstChild as? CaosScriptIsCommandToken
    }

    /**
     * Gets command token for a TARG {agent} command call
     */
    @JvmStatic
    fun getCommandToken(element: CaosScriptCTarg): CaosScriptIsCommandToken? {
        return element.cKwTarg
    }

    /**
     * Generic command token getter for self referencing Command tokens
     */
    @JvmStatic
    fun getCommandToken(element: CaosScriptIsCommandToken): CaosScriptIsCommandToken {
        return element
    }

    /**
     * Gets command token for enum commands
     * ie ENUM/ETCH/ESEE/EPAS
     */
    @JvmStatic
    fun getCommandToken(element: CaosScriptEnumHeaderCommand): CaosScriptIsCommandToken {
        return element.cEnum ?: element.cEcon!!
    }


    /**
     * Gets command token for named game vars
     * ie NAME/GAME/MAME/EAME
     */
    @JvmStatic
    fun getCommandToken(namedGameVar: CaosScriptNamedGameVar): CaosScriptIsCommandToken {
        return namedGameVar.namedGameVarKw
    }

    /**
     * Gets command token for a command call
     */
    @JvmStatic
    fun getCommandToken(call: CaosScriptCommandCall): CaosScriptIsCommandToken? {
        if (call.firstChild is CaosScriptCommandElement) {
            return (call.firstChild as CaosScriptCommandElement).commandToken
        }
        return call.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: call.getChildOfType(CaosScriptCommandElement::class.java)?.commandToken
    }

    /**
     * Gets command token from assignment style command call
     * ie SETV/SETS/NEGV, etc
     */
    @JvmStatic
    fun getCommandToken(assignment: CaosScriptCAssignment): CaosScriptIsCommandToken? {
        return assignment.getChildOfType(CaosScriptIsCommandToken::class.java)
    }

    /**
     * Get command token in lvalue
     */
    @JvmStatic
    fun getCommandToken(lvalue: CaosScriptLvalue): CaosScriptIsCommandToken? {
        return lvalue.getChildOfType(CaosScriptIsCommandToken::class.java)
    }

    /**
     * Gets command token for RTAR
     */
    @JvmStatic
    fun getCommandToken(element: CaosScriptCRtar): CaosScriptCKwRtar {
        return element.cKwRtar
    }

    /**
     * Gets command token for SSFC
     */
    @JvmStatic
    fun getCommandToken(element: CaosScriptCSsfc): CaosScriptIsCommandToken {
        return element.cKwSsfc
    }

    // ============================== //
    // ===== getCommandString() ===== //
    // ============================== //

    /**
     * Gets a command string for the named game var
     * ie GAME "label", or NAME "label", or MAME "???"
     */
    @JvmStatic
    fun getCommandString(namedGameVar: CaosScriptNamedGameVar): String {
        return namedGameVar.stub?.type?.token ?: namedGameVar.namedGameVarKw.commandString
    }

    /**
     * Gets command string from a COAS def command word
     */
    @JvmStatic
    fun getCommandString(word: CaosDefCommandWord): String {
        return word.text
    }

    /**
     * A generic getter for command string in command elements
     */
    @JvmStatic
    fun getCommandString(command: CaosScriptCommandElement): String {

        // Call getString on closest specific getString method
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

    /**
     * Gets a command string for an RValue call
     */
    @JvmStatic
    fun getCommandString(element: CaosScriptRvalue): String? {
        element.stub?.commandString.nullIfUndefOrBlank()?.let {
            return it
        }
        element.rvaluePrime?.commandToken?.text.nullIfUndefOrBlank()?.let {
            return it
        }
        element.incomplete?.text.nullIfEmpty()?.let {
            return it
        }
        return null
    }


    /**
     * Gets a command string for an lvalue
     */
    @JvmStatic
    fun getCommandString(element: CaosScriptLvalue): String? {
        return element.stub?.commandString
                ?: element.commandToken?.text
    }

    /**
     * Gets a command string from a command call.
     */
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

    /**
     * Gets command string for a repeat statement
     * ie REPS
     */
    @JvmStatic
    fun getCommandString(reps: CaosScriptRepeatStatement): String {
        return reps.repsHeader.cReps.text
    }

    /**
     * Gets a command sstring for a loop statement
     * ie. LOOP
     */
    @JvmStatic
    fun getCommandString(element: CaosScriptLoopStatement): String {
        return element.cLoop.text
    }

    /**
     * Gets command string for a command token directly
     */
    @JvmStatic
    fun getCommandString(commandToken: CaosScriptIsCommandToken): String {
        return commandToken.text
    }

    // ============================== //
    // === getCommandStrinUpper() === //
    // ============================== //

    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(command: CaosScriptCommandElement): String? {
        return command.commandString?.toUpperCase()
    }

    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(command: CaosScriptCAssignment): String? {
        return command.commandString.toUpperCase()
    }

    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptRvalue): String? {
        return getCommandString(element)?.toUpperCase()
    }

    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptLvalue): String? {
        return getCommandString(element)?.toUpperCase()
    }

    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptIsCommandToken): String {
        return element.text.toUpperCase()
    }


    /**
     * Gets command string as uppercase
     */
    @JvmStatic
    fun getCommandStringUpper(element: CaosScriptCommandCall): String? {
        return element.stub?.commandUpper.nullIfUndefOrBlank() ?: getCommandString(element).toUpperCase()
    }

    // ============================== //
    // ==== getCommandDefintion() === //
    // ============================== //

    /**
     * Caching key for command definitions getter
     */
    private val COMMAND_DEFINITION_KEY = Key<Pair<CaosVariant, CaosCommand?>>("com.badahori.creatures.plugins.intellij.agenteering.caos.COMMAND_DEFINITION")

    /**
     * Gets the command definitions object from a CAOS command element
     * Uses userdata caches to save requests
     */
    @JvmStatic
    fun getCommandDefinition(element: CaosScriptCommandElement): CaosCommand? {
        // Ensure token exists
        val token = element.commandString
                ?: return null
        // Return command definition if any
        return getCommandDefinition(element, token)
    }

    /**
     * Gets the command definitions object from a CAOS command token
     * Uses userdata caches to save requests
     * Implemented separately from CAOS command element as
     * some commands are given the command token only
     */
    @JvmStatic
    fun getCommandDefinition(element: CaosScriptIsCommandToken): CaosCommand? {
        // Return command definition if any
        return getCommandDefinition(element, element.text)
    }

    /**
     * Gets a command definitions object from an element, and a command token
     * Is generic to handle both command elements themselves, and their tokens directly
     */
    @JvmStatic
    fun getCommandDefinition(element: PsiElement, tokenText: String): CaosCommand? {
        // Ensure that a variant has been set for this element,
        // if not, there is no way to ensure a proper return
        val variant = element.variant
                ?: return null

        // Check if definition has been cached
        element.getUserData(COMMAND_DEFINITION_KEY)?.let { (variantForCommand, command) ->
            // Ensure that cached value is actually for this variant
            // Necessary in case a variant has changed since cache
            if (variantForCommand == variant)
            // return cached value
                return command
        }

        // Get the enclosing command type such as RValue, LValue or Command
        val commandType = element.getEnclosingCommandType()

        val needsFilter = commandType == CaosCommandType.RVALUE
                && variant == CaosVariant.CV
                && (tokenText like "CHAR" || tokenText like "TRAN")

        val numArguments = if (needsFilter)
            element.getSelfOrParentOfType(CaosScriptCommandElement::class.java)
                    ?.arguments
                    ?.size
        else
            null

        // Fetch command definition from CAOS lib
        val command = getCommandDefinition(variant, commandType, tokenText, numArguments)

        // Cache value, even if none was found
        element.putUserData(COMMAND_DEFINITION_KEY, Pair(variant, command))

        // Return found definition
        return command
    }


    /**
     * Token RValues do not represent commands, but rather files and subroutines
     * @return null, as no command is actually being referenced
     */
    @JvmStatic
    fun getCommandDefinition(element: CaosScriptTokenRvalue): CaosCommand? = null

    /**
     * Gets a command definition from the CAOS lib
     */
    private fun getCommandDefinition(
            variant: CaosVariant,
            commandType: CaosCommandType,
            tokenIn: String,
            numArguments:Int? = null
    ): CaosCommand? {
        val lib = CaosLibs[variant]
        val token = tokenIn.replace(SPACES_REGEX, " ")


        // Get filter for command type
        val isCommandType:(command:CaosCommand) -> Boolean = when (commandType) {
            CaosCommandType.RVALUE -> {command -> command.rvalue }
            CaosCommandType.LVALUE -> { command -> command.lvalue }
            CaosCommandType.COMMAND -> { command -> command.isCommand }
            CaosCommandType.CONTROL_STATEMENT -> { command -> command.isCommand }
            CaosCommandType.UNDEFINED -> {_ -> false}
        }
        // Choose filter function based on whether number of args need to be validated
        // Args only need to be validated when in variant CV and token is CHAR or TRAN
        val filter:(command:CaosCommand) -> Boolean = if (numArguments != null) {
            {command ->
                isCommandType(command) && command.command like token && command.parameters.size >= numArguments
            }
        } else {
            {command ->
                isCommandType(command) && command.command like token
            }
        }

        return lib.commands.filter(isCommandType).firstOrNull(filter)
    }

    // ============================== //
    // ======== isVariant() ========= //
    // ============================== //

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

    // ============================== //
    // ======== LOOP Support ======== //
    // ============================== //

    @JvmStatic
    fun isEver(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.cEver != null
    }

    @JvmStatic
    fun isUntil(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator?.cUntl != null
    }

    // ============================== //
    // ======== VAR support ========= //
    // ============================== //

    /**
     * Gets the var group for this var token
     * ie VARx, OVxx, MVxx
     */
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

    /**
     * Gets the variables numbered index
     * ie 10 for "va10", 22 for "ov22"
     */
    @JvmStatic
    fun getVarIndex(element: CaosScriptVarToken): Int? {
        /*return element.stub?.varIndex
                ?: element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()*/
        return element.text.replace("[a-zA-Z]".toRegex(), "").toIntOrNull()
    }

    // ============================== //
    // ======== Get/Set Name ======== //
    // ============================== //

    /**
     * Gets name for use in PsiNamedElement
     */
    @JvmStatic
    fun getName(element: CaosScriptIsCommandToken): String {
        return element.text
    }

    /**
     * Sets name for use in PsiNamedElement
     */
    @JvmStatic
    fun setName(element: CaosScriptIsCommandToken, newName: String): PsiElement {
        CaosScriptPsiElementFactory.createCommandTokenElement(element.project, newName)?.let {
            element.replace(it)
        }
        return element
    }

    /**
     * Gets name for use in PsiNamedElement
     */
    @JvmStatic
    fun getName(element: CaosScriptVarToken): String {
        return element.text.toUpperCase()
    }

    /**
     * Dummy set name method
     */
    @JvmStatic
    fun setName(element: CaosScriptVarToken, newName: String): PsiElement {
        return element
    }

    /**
     * Set variable name in GAME/NAME/EAME/MAME
     */
    @JvmStatic
    fun setName(element: CaosScriptNamedGameVar, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory
                .createStringRValue(element.project, newName, '"')
        return element.rvalue?.replace(newNameElement) ?: element
    }

    /**
     * Gets variable name in GAME/NAME/EAME/MAME
     */
    @JvmStatic
    fun getName(element: CaosScriptNamedGameVar): String {
        return element.stub?.key ?: element.rvalue?.stringValue ?: element.rvalue?.text ?: ""
    }


    /**
     * Gets name value for quote string
     */
    @JvmStatic
    fun getName(element: CaosScriptQuoteStringLiteral): String {
        return element.stringValue
    }

    /**
     * Sets the text for a caos string
     * Useful for GAME/NAME/MAME/EAME string values
     */
    @JvmStatic
    fun setName(element: CaosScriptQuoteStringLiteral, newName: String): PsiElement {
        // Create a new string element
        val newNameElement = CaosScriptPsiElementFactory
                .createStringRValue(element.project, newName, '"')
        // Actually replace string value
        return (element.parent as? CaosScriptRvalue)
                ?.replace(newNameElement)
                ?: element
    }

    /**
     * Gets name for use in PsiNamedElement
     */
    @JvmStatic
    fun getName(element: CaosScriptToken): String {
        return element.text
    }

    /**
     * Sets token value
     */
    @JvmStatic
    fun setName(element: CaosScriptToken, newName: String): PsiElement {
        val newNameElement = CaosScriptPsiElementFactory.createTokenElement(element.project, newName)
                ?: element
        return element.replace(newNameElement)
    }

    /**
     * PsiNamedElement.getName()
     */
    @JvmStatic
    fun getName(element: CaosScriptEventNumberElement): String {
        return element.text
    }

    /**
     * PsiNamedElement.setName dummy method
     */
    @JvmStatic
    fun setName(element: CaosScriptEventNumberElement, newName: String): PsiElement {
        return element
    }

    /**
     * PsiNamedElement.getName()
     */
    @JvmStatic
    fun getName(element: CaosScriptRvalue): String {
        return element.text
    }

    /**
     * PsiNamedElement.setName() dummy method
     */
    @JvmStatic
    fun setName(element: CaosScriptRvalue, newName: String): PsiElement {
        return element
    }

    /**
     * PsiNamedElement.getName()
     */
    @JvmStatic
    fun getName(name: CaosScriptSubroutineName): String {
        return name.text
    }

    /**
     * PsiNamedElement.setName()
     */
    @JvmStatic
    fun setName(name: CaosScriptSubroutineName, newName: String): PsiElement {
        val variant = name.variant
                ?: return name
        val newElement = CaosScriptPsiElementFactory.createSubroutineNameElement(name.project, variant, newName)
                ?: return name
        return name.replace(newElement)
    }

    /**
     * Gets name for use in PsiNamedElement
     */
    @JvmStatic
    fun getName(element: CaosScriptSubroutine): String {
        return element.stub?.name ?: element.subroutineHeader.subroutineName?.name
        ?: element.subroutineHeader.subroutineName?.text ?: ""
    }

    @JvmStatic
    fun setName(element:CaosScriptSubroutine, newName: String) : PsiElement {
        val pointer = SmartPointerManager.createPointer(element)
        val subroutineName = element.subroutineHeader.subroutineName
                ?: return element
        subroutineName.name = newName
        return pointer.element ?: element
    }

    /**
     * PsiNamedIdentifierOwner.getNameIdentifier()
     */
    @JvmStatic
    fun getNameIdentifier(element: CaosScriptSubroutine): PsiElement? {
        return element.subroutineHeader.subroutineName
    }


    /**
     * PsiNamedIdentifierOwner.getNameIdentifier()
     */
    @JvmStatic
    fun getNameIdentifier(element: CaosScriptNamedGameVar): PsiElement? {
        return element.rvalue?.quoteStringLiteral ?: element.rvalue
    }


    // ============================== //
    // ======== Get Reference ======= //
    // ============================== //

    /**
     * Gets reference object for a command token
     */
    @JvmStatic
    fun getReference(element: CaosScriptIsCommandToken): CaosScriptCommandTokenReference {
        return CaosScriptCommandTokenReference(element)
    }

    /**
     * Gets reference for an rvalue
     */
    @JvmStatic
    fun getReference(element: CaosScriptRvalue): CaosScriptExpressionReference {
        return CaosScriptExpressionReference(element)
    }

    /**
     * Gets reference for an event number
     */
    @JvmStatic
    fun getReference(element: CaosScriptEventNumberElement): CaosScriptEventNumberReference {
        return CaosScriptEventNumberReference(element)
    }

    /**
     * Get reference for VAR token
     */
    @JvmStatic
    fun getReference(element: CaosScriptVarToken): CaosScriptVarTokenReference {
        return CaosScriptVarTokenReference(element)
    }

    /**
     * Get reference for subroutine name
     */
    @JvmStatic
    fun getReference(name: CaosScriptSubroutineName): CaosScriptSubroutineNameReference {
        return CaosScriptSubroutineNameReference(name)
    }


    @JvmStatic
    fun getUseScope(element:CaosScriptSubroutine) : SearchScope {
        val scope = element.getParentOfType(CaosScriptScriptElement::class.java)
                ?: return GlobalSearchScope.fileScope(element.containingFile)
        return LocalSearchScope(scope)
    }


    @JvmStatic
    fun getUseScope(element:CaosScriptSubroutineName) : SearchScope {
        val scope = element.getParentOfType(CaosScriptScriptElement::class.java)
                ?: return GlobalSearchScope.fileScope(element.containingFile)
        return LocalSearchScope(scope)
    }

    @JvmStatic
    fun getReference(element:CaosScriptQuoteStringLiteral) : CaosScriptQuoteStringLiteralReference {
        return CaosScriptQuoteStringLiteralReference(element)
    }

    // ============================== //
    // ===== Get Inferred Type ====== //
    // ============================== //

    /**
     * Gets inferred type for an rvalue prime statement
     * ie AGENT, INTEGER, FLOAT, STRING
     */
    @JvmStatic
    fun getInferredType(element: CaosScriptRvaluePrime): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(element) ?: CaosExpressionValueType.UNKNOWN
    }

    /**
     * Gets inferred type for an rvalue-like element
     * ie AGENT, INTEGER, FLOAT, STRING
     */
    @JvmStatic
    fun getInferredType(rvalue: CaosScriptRvalueLike): CaosExpressionValueType {
        if (rvalue !is CaosScriptRvalue) {
            // If not true rvalue, then can only be token_rvalue.
            // Simply return text as token
            return CaosExpressionValueType.TOKEN
        }
        // If is rvalue, and stub has type, return it
        (rvalue as? CaosScriptRvalue)?.stub?.type?.let {
            return it
        }

        // If project is not dumb, and this rvalue is a variable
        // Return its resolved value
        if (!DumbService.isDumb(rvalue.project)) {
            val variable: CaosScriptIsVariable? = rvalue.varToken as? CaosScriptIsVariable
                    ?: rvalue.namedGameVar
            if (variable != null) {
                CaosScriptInferenceUtil.getInferredType(variable)?.let {
                    return it
                }
            }
        }

        // Index is dumb, so return generic VARIABLE type
        (rvalue.varToken)?.let {
            return CaosExpressionValueType.VARIABLE
        }

        // Named game vars are not resolved, so return VARIABLE type
        rvalue.namedGameVar?.let {
            return CaosExpressionValueType.VARIABLE
        }

        // Get rvalue command definition if needed
        rvalue.commandDefinition?.let {
            return it.returnType
        }

        // Is Number
        rvalue.number?.let { number ->
            number.float?.let {
                return CaosExpressionValueType.FLOAT
            }
            number.int?.let {
                return CaosExpressionValueType.INT
            }
        }

        // Is C1 String
        rvalue.c1String?.let {
            return CaosExpressionValueType.C1_STRING
        }

        // Is Quote literal
        rvalue.quoteStringLiteral?.let {
            return CaosExpressionValueType.STRING
        }

        // Is Token
        rvalue.token?.text?.let {
            return CaosExpressionValueType.TOKEN
        }

        // Is Animation String
        rvalue.animationString?.let {
            return CaosExpressionValueType.ANIMATION
        }

        // Is Byte String
        rvalue.byteString?.text?.let {
            return CaosExpressionValueType.BYTE_STRING
        }

        // No type was inferred. Return
        return CaosExpressionValueType.UNKNOWN
    }

    /**
     * Get inferred type for subroutine name
     * @return TOKEN
     */
    @JvmStatic
    fun getInferredType(subroutineName: CaosScriptSubroutineName): CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
    }

    /**
     * Gets inferred type for a variable
     */
    @JvmStatic
    fun getInferredType(varToken: CaosScriptIsVariable): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(varToken) ?: CaosExpressionValueType.VARIABLE
    }

    /**
     * Gets inferred type for an rvalue
     */
    @JvmStatic
    fun getInferredType(element: CaosScriptRvalue): CaosExpressionValueType {
        return CaosScriptInferenceUtil.getInferredType(element)
    }

    /**
     * Gets an inferred type for an lvalue
     */
    @JvmStatic
    fun getInferredType(element: CaosScriptLvalue): CaosExpressionValueType {
        element.varToken?.let {
            return CaosScriptInferenceUtil.getInferredType(it) ?: CaosExpressionValueType.VARIABLE
        }
        element.namedGameVar?.let {
            return CaosExpressionValueType.VARIABLE
        }
        return element.commandDefinition?.returnType
                ?: CaosExpressionValueType.UNKNOWN
    }


    /**
     * Gets the inferred type for a token value
     */
    @JvmStatic
    fun getInferredType(element: CaosScriptTokenRvalue): CaosExpressionValueType {
        return CaosExpressionValueType.TOKEN
    }

    // ============================== //
    // ====== Control Support ======= //
    // ============================== //

    /**
     * Checks for end to quote string
     */
    @JvmStatic
    fun isClosed(stringIn: CaosScriptQuoteStringLiteral): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_DOUBLE_QUOTE
    }

    /**
     * Checks for close to CHAR literal
     */
    @JvmStatic
    fun isClosed(stringIn: CaosScriptCharacter): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_SINGLE_QUOTE
    }

    /**
     * Checks for close of C1 string, ie. ']'
     */
    @JvmStatic
    fun isClosed(stringIn: CaosScriptC1String): Boolean {
        return stringIn.lastChild?.elementType == CaosScriptTypes.CaosScript_CLOSE_BRACKET
    }


    /**
     * Checks that DOIF statement has close
     */
    @JvmStatic
    fun isClosed(element: CaosScriptDoifStatementStatement): Boolean {
        // If last child is not a DOIF statement statement,
        // then something else follows it such as ELIF, ELSE or ENDI
        return element.parent.lastChild != element
    }

    /**
     * Checks that an ELSE statent has a close
     */
    @JvmStatic
    fun isClosed(element: CaosScriptElseIfStatement): Boolean {
        // If last child is not a ELIF statement,
        // then it is closed by an ELIF, ELSE or ENDI statement
        return element.parent.lastChild != element
    }

    /**
     * Checks that ELSE statement has a close
     */
    @JvmStatic
    fun isClosed(element: CaosScriptElseStatement): Boolean {
        // If this ELSE statement is not last child,
        // Then it is closed by an ENDI
        return element.parent.lastChild != element
    }

    /**
     * Checks that ENUM statement has a closing NEXT (or possibly erroneous NSCN)
     */
    @JvmStatic
    fun isClosed(element: CaosScriptEnumNextStatement): Boolean {
        return element.cNext != null || element.cNscn != null
    }

    /**
     * Checks that ESCN statement has a closing NSCN (or possibly erroneous NEXT)
     */
    @JvmStatic
    fun isClosed(element: CaosScriptEnumSceneryStatement): Boolean {
        return element.cNscn != null || element.cNext != null
    }

    /**
     * Checks that event script has a terminator
     */
    @JvmStatic
    fun isClosed(element: CaosScriptEventScript): Boolean {
        return element.scriptTerminator != null
    }

    /**
     * Checks that an install script has a terminator
     */
    @JvmStatic
    fun isClosed(element: CaosScriptInstallScript): Boolean {
        return element.scriptTerminator != null
    }

    /**
     * Checks that an LOOP statement has a terminating EVER/UNTL
     */
    @JvmStatic
    fun isClosed(element: CaosScriptLoopStatement): Boolean {
        return element.loopTerminator != null
    }

    /**
     * Checks that an macro script has a terminating ENDM
     */
    @JvmStatic
    fun isClosed(element: CaosScriptMacro): Boolean {
        return element.scriptTerminator != null
    }

    /**
     * Checks that a REPS statement has a terminating REPE
     */
    @JvmStatic
    fun isClosed(element: CaosScriptRepeatStatement): Boolean {
        return element.cRepe != null
    }

    /**
     * Checks that a remove script has a terminating ENDM
     */
    @JvmStatic
    fun isClosed(element: CaosScriptRemovalScript): Boolean {
        return element.scriptTerminator != null
    }

    /**
     * Checkst that a subroutine has a closing NEXT statement
     */
    @JvmStatic
    fun isClosed(element: CaosScriptSubroutine): Boolean {
        return element.retnKw != null
    }

    // ============================== //
    // ========= Arguments ========== //
    // ============================== //

    /**
     * Gets the expected type of an argument
     */
    @JvmStatic
    fun getExpectedType(element: CaosScriptLvalue): CaosExpressionValueType {
        return CaosExpressionValueType.VARIABLE
    }

    /**
     * Gets index of an LValue in a CAOS command
     */
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


    /**
     * Gets the index of an Argument in a command call
     */
    @JvmStatic
    fun getIndex(element: CaosScriptArgument): Int {
        // find closest element with command element parent
        var lastParent: PsiElement? = element
        while (lastParent != null && lastParent.parent !is CaosScriptCommandElement) {
            lastParent = lastParent.parent
        }
        // Get command element parent
        val parent = lastParent?.parent as? CaosScriptCommandElement
                ?: return 0

        // Get argument index if parent is not an argument
        if (lastParent !is CaosScriptArgument) {
            return when (element.parent) {
                is CaosScriptFamily -> 0
                is CaosScriptGenus -> 1
                is CaosScriptSpecies -> 2
                else -> 0
            }
        }
        // Returns the arguments index based on the parent directly beneath the command element
        return parent.arguments.indexOf(lastParent)
    }


    /**
     * Gets index of subroutine
     * Always zero
     */
    @JvmStatic
    fun getIndex(element: CaosScriptSubroutineName): Int {
        return 0
    }


    /**
     * Gets arguments for a generic command element
     */
    @JvmStatic
    fun getArguments(command: CaosScriptCommandElement): List<CaosScriptArgument> {
        val base = (command as? CaosScriptRvalue)?.rvaluePrime ?: command
        return base.getChildrenOfType(CaosScriptArgument::class.java)
    }

    /**
     * Gets arguments for an enum statement
     */
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

    /**
     * Gets arguments for a ESCN statement
     */
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

    /**
     * Gets argument values for a command call
     */
    @JvmStatic
    fun getArgumentValues(command: CaosScriptCommandCall): List<CaosExpressionValueType> {
        return command.stub?.argumentValues ?: command.arguments.map { it.inferredType }
    }

    /**
     * Gets argument values for an rvalue command call
     */
    @JvmStatic
    fun getArgumentValues(rvalue: CaosScriptRvalue): List<CaosExpressionValueType> {
        return rvalue.stub?.argumentValues ?: rvalue.arguments.map { it.inferredType }
    }

    /**
     * Gets argument value for an lvalue
     */
    @JvmStatic
    fun getArgumentValues(lvalue: CaosScriptLvalue): List<CaosExpressionValueType> {
        return lvalue.stub?.argumentValues ?: lvalue.arguments.map { it.inferredType }
    }

    /**
     * Gets arguments values for a command element
     */
    @JvmStatic
    fun getArgumentValues(element: CaosScriptCommandElement): List<CaosExpressionValueType> {
        return element.arguments.map {
            it.inferredType
        }
    }

    // ============================== //
    // ========= Block Type ========= //
    // ============================== //

    /**
     * Get block type for DOIF
     */
    @JvmStatic
    fun getBlockType(doif: CaosScriptDoifStatementStatement): CaosScriptBlockType = CaosScriptBlockType.DOIF


    /**
     * Get block type for ELIF
     */
    @JvmStatic
    fun getBlockType(elif: CaosScriptElseIfStatement): CaosScriptBlockType = CaosScriptBlockType.ELIF

    /**
     * Get block type for ELSE
     */
    @JvmStatic
    fun getBlockType(doElse: CaosScriptElseStatement): CaosScriptBlockType = CaosScriptBlockType.ELSE

    /**
     * Get block type for ESCN
     */
    @JvmStatic
    fun getBlockType(escn: CaosScriptEnumSceneryStatement): CaosScriptBlockType = CaosScriptBlockType.ESCN


    /**
     * Get block type for LOOP
     */
    @JvmStatic
    fun getBlockType(loop: CaosScriptLoopStatement): CaosScriptBlockType = CaosScriptBlockType.LOOP

    /**
     * Get block type for REPS
     */
    @JvmStatic
    fun getBlockType(reps: CaosScriptRepeatStatement): CaosScriptBlockType = CaosScriptBlockType.REPS


    /**
     * Get block type for MACRO
     */
    @JvmStatic
    fun getBlockType(scrp: CaosScriptMacro): CaosScriptBlockType = CaosScriptBlockType.MACRO

    /**
     * Get block type for SCRP
     */
    @JvmStatic
    fun getBlockType(script: CaosScriptEventScript): CaosScriptBlockType = CaosScriptBlockType.SCRP


    /**
     * Get block type for RSCR
     */
    @JvmStatic
    fun getBlockType(script: CaosScriptRemovalScript): CaosScriptBlockType = CaosScriptBlockType.RSCR


    /**
     * Get block type for ISCR
     */
    @JvmStatic
    fun getBlockType(script: CaosScriptInstallScript): CaosScriptBlockType = CaosScriptBlockType.ISCR

    /**
     * Get block type for SUBR
     */
    @JvmStatic
    fun getBlockType(subr: CaosScriptSubroutine): CaosScriptBlockType = CaosScriptBlockType.SUBR

    /**
     * Get block type for ENUM
     */
    @JvmStatic
    fun getBlockType(enumStatement: CaosScriptEnumNextStatement): CaosScriptBlockType = CaosScriptBlockType.ENUM

    // ============================== //
    // ======= Value Getters ======== //
    // ============================== //

    /**
     * Gets return type for an RValue command call
     */
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

    /**
     * Gets the byte string as an array of ints
     * TODO: delete?
     */
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

    /**
     * Gets the token value for an Rvalue
     * @return null as token value is no longer allowed
     */
    @JvmStatic
    fun getToken(element: CaosScriptRvalue): CaosScriptToken? {
        return null
    }

    /**
     * Gets the string value within the brackets of a C1 string
     */
    @JvmStatic
    fun getStringValue(string: CaosScriptC1String): String {
        return string.textLiteral?.text ?: ""
    }

    /**
     * Gets the string value between the quotes of a double quoted string
     */
    @JvmStatic
    fun getStringValue(stringIn: CaosScriptQuoteStringLiteral): String {
        return stringIn.stringText?.text ?: ""
    }

    /**
     * Gets the char value of a char literal
     */
    @JvmStatic
    fun getStringValue(stringIn: CaosScriptCharacter): String {
        return stringIn.charChar?.text ?: ""
    }

    /**
     * Gets the scope of an element
     */
    @JvmStatic
    fun getScope(element: CaosScriptCompositeElement): CaosScope {
        return element.getSelfOrParentOfType(CaosScriptHasCodeBlock::class.java)?.scope()
                ?: rootScope(element.containingCaosFile!!)
    }

    /**
     * Gets the family in an agent classifier as an int
     */
    @JvmStatic
    fun getFamily(script: CaosScriptEventScript): Int {
        return script.stub?.family
                ?: script.classifier?.family?.text?.toIntSafe()
                ?: -1
    }

    /**
     * Gets the genus in an agent classifier as an int
     */
    @JvmStatic
    fun getGenus(script: CaosScriptEventScript): Int {
        return script.stub?.genus
                ?: script.classifier?.genus?.text?.toIntSafe()
                ?: -1
    }

    /**
     * Gets the species in an agent classifier as an int
     */
    @JvmStatic
    fun getSpecies(script: CaosScriptEventScript): Int {
        return script.stub?.species
                ?: script.classifier?.species?.text?.toIntSafe()
                ?: -1
    }


    /**
     * Gets the event number in an event script
     */
    @JvmStatic
    fun getEventNumber(script: CaosScriptEventScript): Int {
        return try {
            script.stub?.eventNumber ?: script.eventNumberElement?.text?.toInt() ?: -1
        } catch (e: Exception) {
            -1
        }
    }


    /**
     * Gets the assignment of in a CAOS assignment command
     * ie. SETV/NEGV/ADDV/SUBV
     */
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

    /**
     * Gets the VAR type for a named game var
     * ie NAME/GAME/EAME/MAME
     */
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

    /**
     * Gets the key or text component for a named game variable
     */
    @JvmStatic
    fun getKey(element: CaosScriptNamedGameVar): String? {
        return element.rvalue?.stringValue
    }

    /**
     * Gets the key or text component for a named game variable
     */
    @JvmStatic
    fun getKeyType(element: CaosScriptNamedGameVar): CaosExpressionValueType {
        return element.stub?.keyType ?: element.rvalue?.let {rvalue ->
            CaosScriptInferenceUtil.getInferredType(rvalue, false)
        } ?: CaosExpressionValueType.UNKNOWN
    }

    /**
     * Returns whether this rvalue is a number type
     */
    @JvmStatic
    fun isNumeric(expression: CaosScriptRvalue): Boolean {
        return expression.number?.let {
            it.int != null || it.float != null
        } ?: return false
    }

    /**
     * Returns whether this rvalue is an integer type
     */
    @JvmStatic
    fun isInt(expression: CaosScriptRvalue): Boolean {
        return expression.number?.int != null
    }

    /**
     * Returns whether this rvalue is a float type
     */
    @JvmStatic
    fun isFloat(expression: CaosScriptRvalue): Boolean {
        return expression.number?.float != null
    }

    /**
     * Returns the int value of this rvalue
     */
    @JvmStatic
    fun getIntValue(expression: CaosScriptRvalue): Int? {
        return expression.number?.int?.text?.toInt()
                ?: expression.number?.binaryLiteral?.text?.let { binaryToInteger(it) }?.toInt()
                //?: expression.number?.character?.charChar?.text?.get(0)?.toInt()
    }

    /**
     * Returns the float value of this rvalue
     */
    @JvmStatic
    fun getFloatValue(expression: CaosScriptRvalue): Float? {
        return expression.number?.let {
            (it.int ?: it.float)?.text?.toFloat()
        }
    }

    /**
     * Returns whether this rvalue represents any string type
     */
    @JvmStatic
    fun isString(expression: CaosScriptRvalue): Boolean {
        return expression.c1String != null
                || expression.byteString != null
                || expression.animationString != null
                || expression.quoteStringLiteral != null
                || expression.token != null
    }

    /**
     * Returns whether this rvalue is a C1 String
     */
    @JvmStatic
    fun isC1String(expression: CaosScriptRvalueLike): Boolean {
        return expression.c1String != null
    }

    /**
     * Returns whether this rvalue is a byte string
     */
    @JvmStatic
    fun isByteString(expression: CaosScriptRvalueLike): Boolean {
        return expression.byteString != null
    }

    /**
     * Returns whether this rvalue is a quote string
     */
    @JvmStatic
    fun isQuoteString(expression: CaosScriptRvalueLike): Boolean {
        return expression.quoteStringLiteral != null
    }


    /**
     * Returns whether this rvalue is a token
     */
    @JvmStatic
    fun isToken(expression: CaosScriptRvalueLike): Boolean {
        return expression.token != null
    }


    /**
     * Returns the string value of this rvalue
     */
    @JvmStatic
    fun getStringValue(expression: CaosScriptRvalueLike): String? {
        (expression.c1String ?: expression.animationString ?: expression.byteString)?.let {
            return it.text.trim('[', ']')
        }
        return expression.token?.text ?: expression.quoteStringLiteral?.stringValue
    }

    /**
     * Gets the values list value for this rvalue, based on command, parameters and this rvalues value
     */
    @JvmStatic
    fun getParameterValuesListValue(element: CaosScriptRvalueLike): CaosValuesListValue? {
        // Values for lists can only be inferred by literal values
        if (element !is CaosScriptRvalue || !(element.isNumeric || element.isString))
            return null

        // Gets the command from the parent of this rvalue
        val containingCommand = element.parent as? CaosScriptCommandElement
                ?: return null

        // Get command definition for its enclosing command parent
        val commandDefinition = containingCommand.commandDefinition
                ?: return null

        // Get this arguments index in its parent command
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

    /**
     * Get range of RNDV command's random values
     */
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

    /**
     * Check that RNDV min/max are in correct order
     * Needed for inspection in C1
     */
    @JvmStatic
    fun isRndvValuesInOrder(element: CaosScriptCRndv, onNull: Boolean): Boolean {
        val values = getRndvIntRange(element)
        val min = values.first ?: return onNull
        val max = values.second ?: return onNull
        return min < max
    }

    /**
     * Gets the requested width of the DDE: PICT command call
     */
    @JvmStatic
    fun getWidth(dimensions: CaosScriptPictDimensionLiteral): Int {
        dimensions.text.toCharArray().let {
            if (it.isEmpty())
                return -1
            return it[0].toInt()
        }
    }

    /**
     * Gets the requested height of the DDE: PICT command call
     */
    @JvmStatic
    fun getHeight(dimensions: CaosScriptPictDimensionLiteral): Int {
        dimensions.text.toCharArray().let {
            if (it.size < 3)
                return -1
            return it[2].toInt()
        }
    }

    /**
     * Gets the requested width/height of the DDE: PICT command call as a pair of ints
     */
    @JvmStatic
    fun getDimensions(dimensions: CaosScriptPictDimensionLiteral): Pair<Int, Int> {
        dimensions.text.toCharArray().let {
            if (it.size < 3)
                return Pair(-1, -1)
            return Pair(it[0].toInt(), it[2].toInt())
        }
    }

    /**
     * Gets last assignment to a CAOS variable
     */
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

    /**
     * Check that this var token is equivalent to another element taking into account var type and index
     */
    @JvmStatic
    fun isEquivalentTo(element: CaosScriptVarToken, another: PsiElement): Boolean {
        if (another is CaosDefCommandWord) {
            return element.varGroup.value.equalsIgnoreCase(another.text)
        }
        return another is CaosScriptVarToken && another.text.equalsIgnoreCase(element.text)
    }

    // ============================== //
    // ======== Presentation ======== //
    // ============================== //
    /**
     * Gets presentation for the command token
     */
    @JvmStatic
    fun getPresentation(element: CaosScriptIsCommandToken): ItemPresentation {
        return CaosScriptPresentationUtil.getPresentation(element)
    }

    /**
     * Gets presentation for the command token
     */
    @JvmStatic
    fun getPresentation(element: CaosScriptNamedGameVar): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? {
                return if (element.keyType == CaosExpressionValueType.STRING)
                    "${element.varType.token} \"${element.key}\""
                else
                    "\"${element.key}\""
            }

            override fun getLocationString(): String? {
                return (element.containingFile ?: element.originalElement?.containingFile)?.name
            }

            override fun getIcon(unused: Boolean): Icon? {
                return CaosScriptIcons.LVALUE
            }

        }
    }

    /**
     * Gets descriptive text for a family parameter
     */
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

    /**
     * Gets descriptive text for a genus parameter
     */
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

    /**
     * Gets descriptive text for a species parameter
     */
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

    /**
     * Gets descriptive text for a classifier
     */
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

    /**
     * Gets descriptive text for an event script
     */
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

}

/**
 * Gets the type of enclosing command
 * ie RValue/LValue/Command
 */
fun PsiElement.getEnclosingCommandType(): CaosCommandType {
    // Next Element to check
    var element: PsiElement? = this

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
            is CaosScriptErrorCommand -> CaosCommandType.COMMAND
            is CaosScriptErrorRvalue -> CaosCommandType.RVALUE
            is CaosDefCodeBlock -> CaosCommandType.UNDEFINED
            else -> {
                element = element.parent
                continue
            }
        }
    }
    return CaosCommandType.UNDEFINED
}

/**
 * Gets the word case of a psi element
 */
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

/**
 * Returns null if it is blank or is equal to UNDEF
 */
fun String?.nullIfUndefOrBlank(): String? {
    return if (this == null || this == UNDEF || this.isBlank())
        null
    else
        this
}

/**
 * Simplifies getting a command string as upper case regardless of actual psi class implementation
 */
val CaosScriptCommandLike.commandStringUpper: String? get() = commandString?.toUpperCase()


/**
 * Gets number of nested code blocks an element is in
 */
fun getDepth(element: PsiElement): Int {
    var depth = 0
    var parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
    while (parent != null) {
        depth++
        parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
    }
    return depth
}
