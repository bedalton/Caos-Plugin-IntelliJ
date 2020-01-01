package com.openc2e.plugins.intellij.caos.def.psi.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.psi.api.*
import com.openc2e.plugins.intellij.caos.def.references.CaosDefCommandWordReference
import com.openc2e.plugins.intellij.caos.def.references.CaosDefTypeNameReference
import com.openc2e.plugins.intellij.caos.def.references.CaosDefVariableLinkReference
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.caos.utils.substringFromEnd

object CaosDefPsiImplUtil {

    @Suppress("MemberVisibilityCanBePrivate")
    const val AnyType:String = "value"
    const val UnknownReturn:String = "???"
    val UnknownReturnType = CaosDefReturnTypeStruct(type = CaosDefVariableTypeStruct(type = UnknownReturn))
    val AnyTypeType:CaosDefVariableTypeStruct = CaosDefVariableTypeStruct(type = AnyType)

    @JvmStatic
    fun isVariant(header:CaosDefHeader, variant:String) : Boolean {
        return variant in getVariants(header)
    }

    @JvmStatic
    fun getVariants(header:CaosDefHeader) : List<String> {
        return header.variantList.map { it.variantCode.text }
    }

    @JvmStatic
    fun isVariant(command:CaosDefCommandDefElement, variant:String) : Boolean {
        return variant in getVariants(command)
    }

    @JvmStatic
    fun getVariants(command:CaosDefCommandDefElement) : List<String> {
        return command.stub?.variants ?: (command.containingFile as CaosDefFile).variants
    }

    @JvmStatic
    fun isLvalue(command:CaosDefCommandDefElement) : Boolean {
        val stub = command.stub
        if (stub != null)
            return stub.lvalue
        val comment = command.docComment
                ?: return false
        return comment.lvalueList.isNotEmpty()
    }

    @JvmStatic
    fun isRvalue(command:CaosDefCommandDefElement) : Boolean {
        val stub = command.stub
        if (stub != null)
            return stub.lvalue
        val returnType = getReturnTypeString(command)
        return returnType != "command" && returnType != UnknownReturn
    }

    @JvmStatic
    fun getReturnTypeString(command:CaosDefCommandDefElement) : String {
        val stub = command.stub
        if (stub != null)
            return stub.returnType.type.type
        return command.returnType?.variableType?.typeLiteral?.text
                ?: command.returnType?.variableType?.bracketString?.text
                ?: UnknownReturn
    }

    @JvmStatic
    fun getNamespace(@Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate") command:CaosDefCommandDefElement) : String? {
        return null
    }

    @JvmStatic
    fun getCommandName(command:CaosDefCommandDefElement) : String {
        return command.command.commandWordList.joinToString(" ") { it.text }
    }

    @JvmStatic
    fun getParameterStructs(command:CaosDefCommandDefElement) : List<CaosDefParameterStruct> {
        val comment = command.docComment
                ?: return command.parameterList.map { it.toStruct() }
        return command.parameterList.map {
            comment.getParameterStruct(it.parameterName) ?: it.toStruct()
        }
    }

    @JvmStatic
    fun getParameterStruct(command:CaosDefCommandDefElement, name:String) : CaosDefParameterStruct? {
        return getParameterStructs(command).firstOrNull { it.name == name }
    }

    @JvmStatic
    fun getIntRange(fromElement:CaosDefFromTo) : Pair<Int, Int>? {
        val from = fromElement.fromInt.intValue
        val to = fromElement.toInt?.intValue
                ?: return null
        return Pair(from, to)
    }

    @JvmStatic
    fun getIntRange(fromElement:CaosDefFromUntil) : Pair<Int, Int>? {
        val from = fromElement.fromInt.intValue
        val to = fromElement.untilInt?.intValue
                ?: return null
        return Pair(from, to)
    }

    @JvmStatic
    fun getComment(command:CaosDefCommandDefElement) : String? {
        val comment = command.docComment
                ?: return null
        return comment.docCommentFrontComment?.docCommentLineList
                ?.joinToString {
                    val text = it.text
                    text.trim().removePrefix("*")+ "\n"
                }?.trim()
    }

    @JvmStatic
    fun getParameterStruct(docComment:CaosDefDocComment, variableName:String) : CaosDefParameterStruct? {
        return getParameterStructs(docComment).firstOrNull {
            it.name == variableName
        }
    }

    @JvmStatic
    fun getEnclosingDocComment(element:CaosDefCompositeElement) : CaosDefDocComment? {
        val parentCommandDefinition
                = element.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null
        return parentCommandDefinition.docComment
    }

    @JvmStatic
    fun toStruct(parameter: CaosDefParameter) : CaosDefParameterStruct {
        val stub = parameter.stub
        val parameterName = stub?.parameterName ?: getParameterName(parameter)
        val type = stub?.type ?: CaosDefVariableTypeStruct(type = getParameterType(parameter))
        return CaosDefParameterStruct(
                name = parameterName,
                type = type,
                comment = null
        )
    }

    @JvmStatic
    fun getParameterName(parameter: CaosDefParameter) : String {
        return parameter.variableName.text
    }

    @JvmStatic
    fun getParameterType(parameter: CaosDefParameter) : String {
        return parameter.variableType.text
    }

    @JvmStatic
    fun getParameterStructs(docComment:CaosDefDocComment?) : List<CaosDefParameterStruct> {
        return docComment?.docCommentParamList?.mapNotNull{
            getParameterStruct(it)
        } ?: emptyList()
    }

    @JvmStatic
    fun getParameterStruct(paramComment:CaosDefDocCommentParam) : CaosDefParameterStruct? {
        val parameterNameLink = paramComment.variableLink
                ?: return null
        val parameterName = getVariableName(parameterNameLink)
        val parameterTypeName = paramComment.docCommentVariableType?.toStruct()
        val comment = paramComment.docCommentParamText?.text
        return CaosDefParameterStruct(
                name = parameterName,
                type = parameterTypeName ?: AnyTypeType,
                comment = comment
        )
    }

    @JvmStatic
    fun getVariableNameTextRangeInLink(variableLink:CaosDefVariableLink) : TextRange {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        return if (linkTextLength > 2)
            TextRange.create(1, variableLink.text.length - 1)
        else
            TextRange.EMPTY_RANGE
    }

    @JvmStatic
    fun getVariableName(variableLink:CaosDefVariableLink) : String {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        if (linkTextLength > 2)
            return linkText.substringFromEnd(1, 1)
        return ""
    }

    @JvmStatic
    fun getReturnTypeStruct(element: CaosDefDocComment) : CaosDefReturnTypeStruct? {
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
    fun toStruct(returnElement: CaosDefDocCommentReturn) : CaosDefReturnTypeStruct? {
        val type = returnElement.docCommentVariableType?.toStruct() ?:
                CaosDefVariableTypeStruct(
                        type = AnyType
                )
        val comment = returnElement.docCommentParamText?.text
        return CaosDefReturnTypeStruct(
                type = type,
                comment = comment
        )
    }

    @JvmStatic
    fun getReturnTypeStruct(commandDefElement: CaosDefCommandDefElement) : CaosDefReturnTypeStruct? {
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
    fun toStruct(element:CaosDefDocCommentVariableType) : CaosDefVariableTypeStruct? {
        val type = element.typeLiteral?.text
                ?: return null
        val typeDef = element.typeDefName?.text?.substring(1)
        val typeNote = element.typeNote?.text?.substring(1)
        var intRange:Pair<Int,Int>? = null
        var length:Int? = null
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
        return CaosDefVariableTypeStruct(
                type = type,
                noteText = typeNote,
                intRange = intRange,
                length = length,
                typedef = typeDef
        )
    }

    @JvmStatic
    fun isCommand(element:CaosDefCommandDefElement) : Boolean {
        val stub = element.stub
        if (stub != null) {
            return stub.isCommand
        }
        return element.returnType?.text?.toLowerCase() == "command"

    }

    @JvmStatic
    fun getIntValue(element:CaosDefVariableLength) : Int? {
        return element.int.intValue
    }

    @JvmStatic
    fun getIntValue(element:CaosDefInt) : Int {
        return element.text.toInt()
    }


    @JvmStatic
    fun getTypeName(element:CaosDefTypeDefinitionElement) : String {
        return element.stub?.typeName ?: element.typeDefName.text.substring(1)
    }

    @JvmStatic
    fun getKeys(element:CaosDefTypeDefinitionElement) : List<CaosDefTypeDefValueStruct> {
        return element.stub?.keys ?: element.typeDefinitionList.mapNotNull {
            it.toStruct()
        }
    }


    @JvmStatic
    fun getValueForKey(element:CaosDefTypeDefinitionElement, key:String) : CaosDefTypeDefValueStruct? {
        return element.keys.firstOrNull{ it.key == key }
    }

    @JvmStatic
    fun getAllKeys(element:CaosDefTypeDefinitionElement, key:String) : List<String> {
        return element.keys.map{ it.key }
    }

    @JvmStatic
    fun toStruct(element:CaosDefTypeDefinition) : CaosDefTypeDefValueStruct {
        return CaosDefTypeDefValueStruct(
                key = element.stub?.key ?: element.key,
                value = element.stub?.value ?: element.value,
                description = element.stub?.description ?: element.description
        )
    }

    @JvmStatic
    fun getKey(element:CaosDefTypeDefinition) : String {
        return element.stub?.key ?: element.typeDefinitionKey.text
    }

    @JvmStatic
    fun getValue(element:CaosDefTypeDefinition) : String {
        return element.stub?.value ?: element.typeDefinitionValue?.text ?: UnknownReturn
    }

    @JvmStatic
    fun getDescription(element:CaosDefTypeDefinition) : String? {
        return element.stub?.description ?: element.typeDefinitionDescription?.text
    }

    @JvmStatic
    fun getReference(command:CaosDefCommandWord) : CaosDefCommandWordReference {
        return CaosDefCommandWordReference(command)
    }

    @JvmStatic
    fun getName(variableName:CaosDefVariableName) : String {
        return variableName.text
    }

    @JvmStatic
    fun setName(variableName:CaosDefVariableName, newNameString:String) : PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getVariableNameElement(variableName.project, newNameString)
        return variableName.replace(newNameElement)
    }

    @JvmStatic
    fun getName(variableLink: CaosDefVariableLink) : String {
        return variableLink.variableName
    }

    @JvmStatic
    fun setName(variableLink:CaosDefVariableLink, newNameString:String) : PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getVariableLinkElement(variableLink.project, newNameString)
        return variableLink.replace(newNameElement)
    }

    @JvmStatic
    fun getTextOffset(variableLink:CaosDefVariableLink) : Int {
        return 1
    }


    @JvmStatic
    fun getName(element: CaosDefCommandWord) : String {
        return element.text
    }

    @JvmStatic
    fun setName(element:CaosDefCommandWord, newNameString:String) : PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getCommandWordElement(element.project, newNameString)
        return element.replace(newNameElement)
    }

    @JvmStatic
    fun getIndex(element:CaosDefCommandWord) : Int {
        val parent = element.parent as? CaosDefCommand
                ?: return 0
        val index = parent.commandWordList.indexOf(element)
        return if (index >= 0)
            index
        else
            0
    }

    @JvmStatic
    fun getOffsetRange(element:CaosDefVariableLink) : TextRange {
        val length = element.variableName.length
        if (length < 3)
            return TextRange.EMPTY_RANGE
        else
            return TextRange(1, length + 1)
    }

    @JvmStatic
    fun getReference(element:CaosDefVariableLink) : CaosDefVariableLinkReference {
        return CaosDefVariableLinkReference(element)
    }


    @JvmStatic
    fun getName(element: CaosDefTypeDefName) : String {
        return element.text.substring(1)
    }

    @JvmStatic
    fun setName(element:CaosDefTypeDefName, newNameString:String) : PsiElement {
        val newNameElement = CaosDefPsiElementFactory
                .getTypeDefName(element.project, newNameString)
        return element.replace(newNameElement)
    }

    @JvmStatic
    fun getReference(element:CaosDefTypeDefName) : CaosDefTypeNameReference {
        return CaosDefTypeNameReference(element)
    }



}