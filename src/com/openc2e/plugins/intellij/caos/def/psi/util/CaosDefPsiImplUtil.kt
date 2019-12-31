package com.openc2e.plugins.intellij.caos.def.psi.util

import com.intellij.openapi.util.TextRange
import com.openc2e.plugins.intellij.caos.def.psi.api.*
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.caos.utils.substringFromEnd

object CaosDefPsiImplUtil {

    const val AnyType:String = "value"

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
                ?: return null;
        return parentCommandDefinition.docComment
    }

    @JvmStatic
    fun getParameterTypeStruct(parameter: CaosDefParameter) : CaosDefParameterStruct {
        val struct = parameter.stub?.parameterStruct
        if (struct != null)
            return struct
        val parameterName = getParameterName(parameter)
        val type = getParameterType(parameter)
        // todo think about allowing all notations in
        val typeDefType:String? = null
        val typeNote:String? = null
        return CaosDefParameterStruct(
                name = parameterName,
                typedef =  typeDefType,
                type = type,
                noteText = typeNote,
                comment = null
        );
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
        return docComment?.docCommentBody?.docCommentParamList?.mapNotNull{
            getParameterStruct(it)
        } ?: emptyList()
    }

    @JvmStatic
    fun getParameterStruct(paramComment:CaosDefDocCommentParam) : CaosDefParameterStruct? {
        val parameterNameLink = paramComment.variableLink
                ?: return null
        val parameterName = getVariableName(parameterNameLink)
        val parameterTypeName = paramComment.docCommentVariableType?.typeLiteral?.text
        val type = paramComment.docCommentVariableType?.typeDefName?.text
        val note = paramComment.docCommentVariableType?.typeNote?.text
        val comment = paramComment.docCommentParamText?.text
        return CaosDefParameterStruct(
                name = parameterName,
                type = parameterTypeName ?: AnyType,
                typedef = type,
                noteText = note,
                comment = comment
        )
    }

    @JvmStatic
    fun getVariableNameTextRangeInLink(variableLink:CaosDefVariableLink) : TextRange {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        if (linkTextLength > 2)
            return TextRange.create(1, variableLink.text.length - 1);
        else
            return TextRange.EMPTY_RANGE
    }

    @JvmStatic
    fun getVariableName(variableLink:CaosDefVariableLink) : String {
        val linkText = variableLink.text
        val linkTextLength = linkText.length
        if (linkTextLength > 2)
            return linkText.substringFromEnd(1, 2)
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
        val parameterTypeName = returnElement.docCommentVariableType?.typeLiteral?.text.nullIfEmpty()
                ?: return null
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
    fun toStruct(element:CaosDefDocCommentVariableType) : CaosDefVariableTypeStruct {

    }

    @JvmStatic
    fun getVariableTypeStruct(element:CaosDefDocCommentVariableType) : CaosDefParameterStruct {

    }

    @JvmStatic
    fun isCommand(element:CaosDefCommandDefElement) : Boolean {
        val stub = element.stub
        if (stub != null) {
            return stub.isCommand
        }
        return element.returnType?.text?.toLowerCase() == "command"

    }


}