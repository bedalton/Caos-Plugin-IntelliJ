package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting.CaosDefCodeBlockStringEscaper
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.references.CaosDefDocCommentHashtagReference
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.references.CaosDefValuesListNameReference
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.references.CaosDefValuesListValueKeyReference
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.references.CaosDefVariableLinkReference
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.ValuesListEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosScriptCommandTokenReference
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.usageView.UsageViewUtil
import icons.CaosScriptIcons
import javax.swing.Icon


@Suppress("UNUSED_PARAMETER")
object CaosDefPsiImplUtil {

    @Suppress("MemberVisibilityCanBePrivate")
    const val AnyType: String = "value"
    const val UnknownReturn: String = "???"
    val UnknownReturnType = CaosDefReturnTypeStruct(type = CaosDefVariableTypeStruct(type = UnknownReturn))
    val AnyTypeType: CaosDefVariableTypeStruct = CaosDefVariableTypeStruct(type = AnyType)

    @JvmStatic
    fun isVariant(header: CaosDefHeader, variant: CaosVariant): Boolean {
        return variant in getVariants(header)
    }

    @JvmStatic
    fun getVariants(header: CaosDefHeader): List<CaosVariant> {
        return header.variantList.mapNotNull { variantListItem ->
            val variantCode = variantListItem.variantCode.text.trim() // Needs trim as lexer adds space??
            CaosVariant.fromVal(variantCode).let {
                if (it != CaosVariant.UNKNOWN)
                    it
                else
                    null
            }
        }
    }

    @JvmStatic
    fun isVariant(command: CaosDefCommandDefElement, variant: CaosVariant): Boolean {
        return variant in getVariants(command)
    }

    @JvmStatic
    fun getVariants(command: CaosDefCommandDefElement): List<CaosVariant> {
        return command.stub?.variants ?: (command.containingFile as CaosDefFile).variants
    }

    @JvmStatic
    fun isLvalue(command: CaosDefCommandDefElement): Boolean {
        command.stub?.lvalue?.let {
            return it
        }
        return command.docComment?.lvalueList.orEmpty().isNotEmpty()
    }

    @JvmStatic
    fun isRvalue(command: CaosDefCommandDefElement): Boolean {
        command.stub?.rvalue?.let {
            return it
        }
        return command.docComment?.rvalueList?.isNotEmpty()
                ?: command.returnTypeString.let { it != "command" && it != UnknownReturn }
    }

    @JvmStatic
    fun getRequiresOwner(command: CaosDefCommandDefElement): Boolean {
        command.stub?.requiresOwner?.let {
            return it
        }
        return command.docComment?.requiresOwner ?: false
    }

    @JvmStatic
    fun getRequiresOwner(docComment: CaosDefDocComment): Boolean {
        return docComment.ownrList.isNotEmpty()
    }

    @JvmStatic
    fun getSimpleType(element: CaosDefParameter): CaosExpressionValueType {
        return element.stub?.simpleType ?: getSimpleType(element.variableType)
    }

    @JvmStatic
    fun getSimpleType(element: CaosDefVariableType): CaosExpressionValueType {
        val simpleName = (element.typeLiteral ?: element.bracketString)?.text
                ?: return CaosExpressionValueType.UNKNOWN
        return CaosExpressionValueType.fromSimpleName(simpleName)
    }

    @JvmStatic
    fun getReturnTypeString(command: CaosDefCommandDefElement): String {
        val stub = command.stub
        if (stub != null)
            return stub.returnType.type.type
        return command.returnType?.variableType?.typeLiteral?.text
                ?: command.returnType?.variableType?.bracketString?.text
                ?: UnknownReturn
    }

    @JvmStatic
    fun getNamespace(@Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate") command: CaosDefCommandDefElement): String? {
        return null
    }

    @JvmStatic
    fun getCommandName(command: CaosDefCommandDefElement): String {
        return command.stub?.command ?: command.command.commandWordList.joinToString(" ") { it.text }
    }

    @JvmStatic
    fun getCommandString(commandWord: CaosDefCommandWord): String {
        return commandWord.text
    }

    @JvmStatic
    fun getCommandWords(command: CaosDefCommandDefElement): List<String> {
        return command.stub?.commandWords ?: command.command.commandWordList.map { it.text }
    }

    @JvmStatic
    fun getParameterStructs(command: CaosDefCommandDefElement): List<CaosDefParameterStruct> {
        val comment = command.docComment
                ?: return command.parameterList.map { it.toStruct() }
        return command.parameterList.map {
            comment.getParameterStruct(it.parameterName) ?: it.toStruct()
        }
    }

    @JvmStatic
    fun getParameterStruct(command: CaosDefCommandDefElement, name: String): CaosDefParameterStruct? {
        return getParameterStructs(command).firstOrNull { it.name == name }
    }

    @JvmStatic
    fun getIntRange(fromElement: CaosDefFromTo): Pair<Int, Int>? {
        val from = fromElement.fromInt.intValue
        val to = fromElement.toInt?.intValue
                ?: return null
        return Pair(from, to)
    }

    @JvmStatic
    fun getIntRange(fromElement: CaosDefFromUntil): Pair<Int, Int>? {
        val from = fromElement.fromInt.intValue
        val to = fromElement.untilInt?.intValue
                ?: return null
        return Pair(from, to)
    }

    private val STAR_FIRST_REGEX = "^\\s*\\*\\s*".toRegex()

    @JvmStatic
    fun getComment(command: CaosDefCommandDefElement): String? {
        return command.stub?.comment ?: command
                .docComment
                ?.docCommentFrontComment
                ?.children
                ?.flatMap {
                    it.text.split("\n")
                }
                ?.joinToString("\n") {
                    it.replace(STAR_FIRST_REGEX, "")
                }
                ?.replace("[ ]+".toRegex(), " ")
    }

    @JvmStatic
    fun getParameterStruct(docComment: CaosDefDocComment, variableName: String): CaosDefParameterStruct? {
        return getParameterStructs(docComment).firstOrNull {
            it.name == variableName
        }
    }

    @JvmStatic
    fun getEnclosingDocComment(element: CaosDefCompositeElement): CaosDefDocComment? {
        val parentCommandDefinition = element.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null
        return parentCommandDefinition.docComment
    }

    @JvmStatic
    fun toStruct(parameter: CaosDefParameter): CaosDefParameterStruct {
        val stub = parameter.stub
        val parameterName = stub?.parameterName ?: getParameterName(parameter)
        val type = stub?.type ?: CaosDefVariableTypeStruct(type = getParameterType(parameter))
        return CaosDefParameterStruct(
                parameterNumber = (parameter.parent as? CaosDefCommandDefElement)?.parameterList?.indexOf(parameter)
                        ?: -1,
                name = parameterName,
                type = type,
                comment = null
        )
    }

    @JvmStatic
    fun getParameterName(parameter: CaosDefParameter): String {
        return parameter.variableName.text
    }

    @JvmStatic
    fun getParameterType(parameter: CaosDefParameter): String {
        return parameter.variableType.text
    }

    @JvmStatic
    fun getParameterStructs(docComment: CaosDefDocComment?): List<CaosDefParameterStruct> {
        return docComment?.docCommentParamList?.mapNotNull {
            getParameterStruct(it)
        } ?: emptyList()
    }

    @JvmStatic
    fun getParameterStruct(paramComment: CaosDefDocCommentParam): CaosDefParameterStruct? {
        val parameterNameLink = paramComment.variableLink
                ?: return null
        val parameterName = getVariableName(parameterNameLink)
        val parameterTypeName = paramComment.docCommentVariableType?.toStruct()
        val comment = paramComment.docCommentParamText?.text
        val parameterList = paramComment
                .getParentOfType(CaosDefCommandDefElement::class.java)
                ?.parameterList
        val parameterNumber = parameterList
                ?.firstOrNull {
                    it.parameterName == parameterName
                }?.let {
                    parameterList.indexOf(it)
                }
                ?: -1
        return CaosDefParameterStruct(
                parameterNumber = parameterNumber,
                name = parameterName,
                type = parameterTypeName ?: AnyTypeType,
                comment = comment
        )
    }

    @JvmStatic
    fun getVariableNameTextRangeInLink(variableLink: CaosDefVariableLink): TextRange {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        return if (linkTextLength > 2)
            TextRange.create(1, variableLink.text.length - 1)
        else
            TextRange.EMPTY_RANGE
    }

    @JvmStatic
    fun getVariableName(variableLink: CaosDefVariableLink): String {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        if (linkTextLength > 2)
            return linkText.substringFromEnd(1, 1)
        return ""
    }

    @JvmStatic
    fun getReturnTypeStruct(element: CaosDefDocComment): CaosDefReturnTypeStruct? {
        val stub = element.stub
        if (stub?.returnType != null)
            return stub.returnType
        return element.docCommentReturnList
                .mapNotNull { it.toStruct() }
                .firstOrNull {
                    it.type.type != AnyType
                }
    }

    @JvmStatic
    fun toStruct(returnElement: CaosDefDocCommentReturn): CaosDefReturnTypeStruct? {
        val type = returnElement.docCommentVariableType?.toStruct() ?: CaosDefVariableTypeStruct(
                type = AnyType
        )
        val comment = returnElement.docCommentParamText?.text
        return CaosDefReturnTypeStruct(
                type = type,
                comment = comment
        )
    }

    @JvmStatic
    fun getReturnTypeStruct(commandDefElement: CaosDefCommandDefElement): CaosDefReturnTypeStruct? {
        val comment = commandDefElement.docComment
        val commentReturnItem = comment?.returnTypeStruct
        if (commentReturnItem != null && commentReturnItem.type.type.nullIfEmpty() != null) {
            return commentReturnItem
        }
        val typeString = commandDefElement.returnType?.text
                ?: return null

        return CaosDefReturnTypeStruct(
                type = CaosDefVariableTypeStruct(type = typeString),
                comment = null
        )
    }

    @JvmStatic
    fun getSimpleReturnType(commandDefElement: CaosDefCommandDefElement): CaosExpressionValueType {
        commandDefElement.stub?.simpleReturnType?.let {
            return it
        }
        val returnTypeText = commandDefElement.stub?.returnType?.type?.type
                ?: commandDefElement.returnType?.variableType?.let { it.typeLiteral ?: it.bracketString }?.text
                ?: return CaosExpressionValueType.UNKNOWN
        return CaosExpressionValueType.fromSimpleName(returnTypeText)
    }

    @JvmStatic
    fun toStruct(element: CaosDefDocCommentVariableType): CaosDefVariableTypeStruct? {
        val type = element.typeLiteral?.text
                ?: return null
        val typeDef = element.valuesListName?.text?.substring(1)
        val typeNote = element.typeNoteStatement?.typeNote?.text?.substring(1)
        var intRange: Pair<Int, Int>? = null
        var length: Int? = null
        when {
            element.fromUntil != null -> {
                intRange = element.fromUntil?.intRange
            }
            element.fromTo != null -> {
                intRange = element.fromTo?.intRange
            }
            element.variableLength != null -> {
                length = element.variableLength?.intValue
            }
        }
        val fileTypes = element.variableFileType?.text?.substring(6)?.split("/")
        return CaosDefVariableTypeStruct(
                type = type,
                noteText = typeNote,
                intRange = intRange,
                length = length,
                valuesList = typeDef,
                fileTypes = fileTypes
        )
    }

    @JvmStatic
    fun isCommand(element: CaosDefCommandDefElement): Boolean {
        element.stub?.isCommand?.let {
            return it
        }
        return element.returnTypeString.toLowerCase() == "command"
    }

    @JvmStatic
    fun getIntValue(element: CaosDefVariableLength): Int? {
        return element.int.intValue
    }

    @JvmStatic
    fun getIntValue(element: CaosDefInt): Int {
        return element.text.toInt()
    }


    @JvmStatic
    fun getTypeName(element: CaosDefValuesListElement): String {
        return element.stub?.typeName ?: element.valuesListName.text.substring(1)
    }

    @JvmStatic
    fun getTypeNoteString(element: CaosDefValuesListElement): String? {
        return element.stub?.typeNote ?: element.typeNoteStatement?.typeNote?.text
    }

    @JvmStatic
    fun isBitflags(element: CaosDefValuesListElement): Boolean {
        return element.stub?.isBitflags ?: getTypeNoteString(element)?.equalsIgnoreCase("BitFlags").orFalse()
    }

    @JvmStatic
    fun getValuesListValues(element: CaosDefValuesListElement): List<CaosDefValuesListValueStruct> {
        return element.stub?.keys ?: element.valuesListValueList.mapNotNull {
            it.toStruct()
        }
    }


    @JvmStatic
    fun getValueForKey(element: CaosDefValuesListElement, key: String): CaosDefValuesListValueStruct? {
        return element.valuesListValues.firstOrNull {
            when (it.equality) {
                ValuesListEq.EQUAL -> it.key == key
                ValuesListEq.NOT_EQUAL -> it.key != key
                ValuesListEq.GREATER_THAN -> try {
                    key.toInt() > it.key.replace("[^0-9]".toRegex(), "").toInt()
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    @JvmStatic
    fun getAllKeys(element: CaosDefValuesListElement, key: String): List<String> {
        return element.valuesListValues.map { it.key }
    }

    @JvmStatic
    fun toStruct(element: CaosDefValuesListValue): CaosDefValuesListValueStruct {
        return CaosDefValuesListValueStruct(
                key = element.stub?.key ?: element.key,
                value = element.stub?.value ?: element.value,
                equality = element.equality,
                description = element.stub?.description ?: element.description
        )
    }

    @JvmStatic
    fun getEquality(element: CaosDefValuesListValue): ValuesListEq {
        element.stub?.equality?.let { return it }
        if (element.key.isEmpty()) {
            return ValuesListEq.EQUAL
        }
        return element.key.trim().substring(0, 1).let {
            when (it) {
                "!" -> ValuesListEq.NOT_EQUAL
                ">" -> ValuesListEq.GREATER_THAN
                else -> ValuesListEq.EQUAL
            }
        }
    }

    @JvmStatic
    fun getKey(element: CaosDefValuesListValue): String {
        return element.stub?.key ?: element.valuesListValueKey.text
    }

    @JvmStatic
    fun getValue(element: CaosDefValuesListValue): String {
        return element.stub?.value ?: element.valuesListValueName?.text?.trim() ?: UnknownReturn
    }

    @JvmStatic
    fun getDescription(element: CaosDefValuesListValue): String? {
        return element.stub?.description ?: element.valuesListValueDescription?.text
    }

    @JvmStatic
    fun getPresentation(element: CaosDefDocCommentHashtag): ItemPresentation? {
        val parentDeclaration = element.getParentOfType(CaosDefCommandDefElement::class.java)
        val text = parentDeclaration?.fullCommand ?: UsageViewUtil.createNodeText(element)
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return text
            }

            override fun getLocationString(): String {
                return element.containingFile.name
            }

            override fun getIcon(b: Boolean): Icon? {
                return CaosScriptIcons.HASHTAG
            }
        }
    }

    @JvmStatic
    fun getFullCommand(command: CaosDefCommandDefElement): String {
        val tokens = mutableListOf(command.commandName, wrapParameterType(command.returnTypeString))
        for (param in command.parameterStructs) {
            tokens.add(param.name)
            tokens.add(wrapParameterType(param.type.type))
        }
        return tokens.joinToString(" ")
    }

    private fun wrapParameterType(type: String): String {
        if (type.substring(0, 1) == "[" || type.substring(0, 1) == "(")
            return type
        return "($type)"
    }

    @JvmStatic
    fun getReference(command: CaosDefCommandWord): CaosScriptCommandTokenReference {
        return CaosScriptCommandTokenReference(command)
    }

    @JvmStatic
    fun getName(variableName: CaosDefVariableName): String {
        return variableName.text
    }

    @JvmStatic
    fun setName(variableName: CaosDefVariableName, newNameString: String): PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getVariableNameElement(variableName.project, newNameString)
        return variableName.replace(newNameElement)
    }

    @JvmStatic
    fun getName(variableLink: CaosDefVariableLink): String {
        return variableLink.variableName
    }

    @JvmStatic
    fun setName(variableLink: CaosDefVariableLink, newNameString: String): PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getVariableLinkElement(variableLink.project, newNameString)
        return variableLink.replace(newNameElement)
    }

    @JvmStatic
    fun getName(hashtag: CaosDefDocCommentHashtag): String {
        return hashtag.stub?.hashtag ?: hashtag.text.substring(1)
    }

    @JvmStatic
    fun setName(hashtag: CaosDefDocCommentHashtag, newNameString: String): PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .createHashTag(hashtag.project, newNameString)
                ?: return hashtag
        return hashtag.replace(newNameElement)
    }

    @JvmStatic
    fun setName(element:CaosDefValuesListValueKey, newNameString:String) : PsiElement {
        if (newNameString.isEmpty())
            return element
        val newNameElement = CaosDefPsiElementFactory
                .createNewValuesListValueKey(element.project, newNameString)
                ?: return element
        return element.replace(newNameElement)
    }

    @JvmStatic
    fun getName(element:CaosDefValuesListValueKey) : String {
        return element.text.trim()
    }


    @JvmStatic
    fun getName(element: CaosDefCommandWord): String {
        return element.text.trim()
    }

    @JvmStatic
    fun setName(element: CaosDefCommandWord, newNameString: String): PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getCommandWordElement(element.project, newNameString)
        return element.replace(newNameElement)
    }

    @JvmStatic
    fun getName(element: CaosDefValuesListName): String {
        return element.text.substring(1)
    }

    @JvmStatic
    fun setName(element: CaosDefValuesListName, newNameString: String): PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getValuesListName(element.project, newNameString)
        return element.replace(newNameElement)
    }

    @JvmStatic
    fun getPresentationText(element:CaosDefValuesListValueKey) : String {
        return (element.parent as? CaosDefValuesListValue)?.let {
            getPresentationText(it)
        } ?: "Value ${element.text}"
    }

    @JvmStatic
    fun getPresentationText(element:CaosDefValuesListValue) : String {
        val valueListValueName
                = element.getParentOfType(CaosDefValuesListElement::class.java)
                ?.valuesListName
                ?.let {
                    " : @$it"
                }
                ?: ""
        val key = element.stub?.key ?: element.valuesListValueKey.text
        val name = element.stub?.value ?: element.valuesListValueName?.text ?: return key
        return "$key = $name$valueListValueName)"
    }



    @JvmStatic
    fun getReference(hashtag: CaosDefDocCommentHashtag): CaosDefDocCommentHashtagReference {
        return CaosDefDocCommentHashtagReference(hashtag)
    }

    @JvmStatic
    fun getReference(element:CaosDefValuesListValueKey) : CaosDefValuesListValueKeyReference {
        return CaosDefValuesListValueKeyReference(element)
    }

    @JvmStatic
    fun getTextOffset(variableLink: CaosDefVariableLink): Int {
        return 1
    }

    @JvmStatic
    fun getIndex(element: CaosDefCommandWord): Int {
        val parent = element.parent as? CaosDefCommand
                ?: return 0
        val index = parent.commandWordList.indexOf(element)
        return if (index >= 0)
            index
        else
            0
    }

    @JvmStatic
    fun getOffsetRange(element: CaosDefVariableLink): TextRange {
        val length = element.variableName.length
        return if (length < 3)
            TextRange.EMPTY_RANGE
        else
            TextRange(1, length + 1)
    }

    @JvmStatic
    fun getReference(element: CaosDefVariableLink): CaosDefVariableLinkReference {
        return CaosDefVariableLinkReference(element)
    }


    @JvmStatic
    fun getReference(element: CaosDefValuesListName): CaosDefValuesListNameReference {
        return CaosDefValuesListNameReference(element)
    }

    @JvmStatic
    fun isVariant(element: CaosDefCommandWord, variants: List<CaosVariant>, strict: Boolean): Boolean {
        val thisVariants = element.containingCaosDefFile.variants
        if (thisVariants.isEmpty())
            return !strict
        return variants.intersect(thisVariants).isNotEmpty()
    }

    @JvmStatic
    fun isVariant(element: CaosDefDocCommentHashtag, variants: List<CaosVariant>, strict: Boolean): Boolean {
        val thisVariants = getVariants(element)
        if (thisVariants.isEmpty())
            return !strict
        return variants.intersect(thisVariants).isNotEmpty()
    }

    @JvmStatic
    fun getVariants(element: CaosDefDocCommentHashtag): List<CaosVariant> {
        return element.stub?.variants ?: element.containingCaosDefFile.variants
    }

    @JvmStatic
    fun getPresentation(element: CaosDefCommandWord): ItemPresentation {
        val text = element.getParentOfType(CaosDefCommandDefElement::class.java)?.let { command ->
            val returnType = command.returnTypeStruct?.type?.let { " " + formatType(it) }.orElse("(???)")
            val parameters = formatParameters(command.parameterStructs)
            command.commandName + " " + returnType + " " + parameters
        }?.trim() ?: (element.parent as? CaosDefCommand)?.text ?: element.text
        //val icon = if (declaration.isCategory) ObjJIcons.CATEGORY_ICON else ObjJIcons.CLASS_ICON
        val fileName = "@variants(" + element.containingCaosDefFile.variants.joinToString(",") + ")"
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return text
            }

            override fun getLocationString(): String {
                return fileName
            }

            override fun getIcon(b: Boolean): Icon? {
                return null
            }
        }
    }

    @JvmStatic
    fun getPresentation(element: CaosDefValuesListValue): ItemPresentation {
        val struct = element.toStruct()
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return getPresentationText(element)
            }

            override fun getLocationString(): String {
                val location = (struct.description
                        ?: "") + element.getParentOfType(CaosDefValuesListElement::class.java)?.typeName?.let {
                    " @ $it"
                }
                return location.trim()
            }

            override fun getIcon(b: Boolean): Icon? {
                return null
            }
        }
    }

    @JvmStatic
    fun isEquivalentTo(element: CaosDefCommandWord, another: PsiElement): Boolean {
        return element.text.equalsIgnoreCase(another.text) ||
                (another as? CaosScriptVarToken)
                        ?.varGroup
                        ?.value
                        ?.let { element.text.toUpperCase().equalsIgnoreCase(it) }
                        .orFalse()
    }

    private fun formatParameters(parameters: List<CaosDefParameterStruct>): String {
        return parameters.joinToString(" ") { formatParameter(it) }
    }

    private fun formatParameter(parameter: CaosDefParameterStruct): String {
        val type = formatType(parameter.type)
        return parameter.name + " " + type
    }

    private fun formatType(type: CaosDefVariableTypeStruct): String {
        val typeText = type.type
        return if (typeText.startsWith("[") || typeText.startsWith("("))
            typeText
        else
            "($typeText)"
    }

    @JvmStatic
    fun isValidHost(block: CaosDefCodeBlock): Boolean {
        return true
    }

    @JvmStatic
    fun toSimpleName(element:CaosDefVariableType) : CaosExpressionValueType {
        val simpleName = (element.typeLiteral ?: element.bracketString)?.text
            ?: return CaosExpressionValueType.UNKNOWN
        return CaosExpressionValueType.fromSimpleName(simpleName)
    }

    @JvmStatic
    fun updateText(block: CaosDefCodeBlock, text: String): CaosDefCodeBlock {
        val expression = CaosScriptPsiElementFactory.createCodeBlock(block.project, text)
        return block.replace(expression) as CaosDefCodeBlock
    }

    @JvmStatic
    fun createLiteralTextEscaper(block: CaosDefCodeBlock): LiteralTextEscaper<out PsiLanguageInjectionHost?>? {
        return CaosDefCodeBlockStringEscaper(block)
    }

}
