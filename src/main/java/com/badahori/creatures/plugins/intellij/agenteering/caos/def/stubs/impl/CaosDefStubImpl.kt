package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType


class CaosDefCommandDefinitionStubImpl(
        parent: StubElement<*>,
        override val namespace: String?,
        override val command: String,
        override val parameters: List<CaosDefParameterStruct>,
        override val comment: String?,
        override val returnType: CaosDefReturnTypeStruct,
        override val rvalue: Boolean,
        override val lvalue: Boolean,
        override val isCommand: Boolean,
        override val variants:List<CaosVariant>,
        override val simpleReturnType: CaosExpressionValueType,
        override val requiresOwner:Boolean
) : StubBase<CaosDefCommandDefElementImpl>(parent, CaosDefStubTypes.COMMAND_ELEMENT), CaosDefCommandDefinitionStub {
    override val commandWords:List<String> by lazy {
        command.split(" ")
    }
}

class CaosDefDocCommentStubImpl(
        parent: StubElement<*>,
        override val parameters: List<CaosDefParameterStruct>,
        override val comment: String?,
        override val returnType: CaosDefReturnTypeStruct,
        override val rvalue: Boolean,
        override val requiresOwner:Boolean,
        override val lvalue: Boolean
) : StubBase<CaosDefDocCommentImpl>(parent, CaosDefStubTypes.DOC_COMMENT), CaosDefDocCommentStub

class CaosDefParameterStubImpl(
        parent:StubElement<*>,
        override val parameterName: String,
        override val type:CaosDefVariableTypeStruct,
        override val comment: String?,
        override val simpleType: CaosExpressionValueType
) : StubBase<CaosDefParameterImpl>(parent, CaosDefStubTypes.PARAMETER), CaosDefParameterStub

class CaosDefValuesListStubImpl(
        parent: StubElement<*>,
        override val typeName:String,
        override val keys: List<CaosDefValuesListValueStruct>,
        override val typeNote: String?,
        override val isBitflags: Boolean
) : StubBase<CaosDefValuesListElementImpl>(parent, CaosDefStubTypes.VALUES_LIST_ELEMENT), CaosDefValuesListStub


class CaosDefValuesListValueStubImpl(
        parent: StubElement<*>,
        override val key:String,
        override val value:String,
        override val description: String?,
        override val equality: ValuesListEq
) : StubBase<CaosDefValuesListValueImpl>(parent, CaosDefStubTypes.VALUES_LIST_VALUE), CaosDefValuesListValueStub


data class CaosDefParameterStruct(
        val parameterNumber:Int,
        val name: String,
        val type: CaosDefVariableTypeStruct,
        val simpleType:CaosExpressionValueType,
        val comment: String? = null
)

data class CaosDefReturnTypeStruct(val type: CaosDefVariableTypeStruct, val comment: String? = null)

data class CaosDefVariableTypeStruct(
        val type:String,
        val valuesList: String? = null,
        val noteText: String? = null,
        val intRange:Pair<Int, Int>? = null,
        val fileTypes:List<String>? = null,
        val length:Int? = null
)

data class CaosDefValuesListValueStruct (
        val key:String,
        val value:String,
        val equality: ValuesListEq,
        val description:String? = null
)

class CaosDefDocCommentHashtagStubImpl(
        parent:StubElement<*>?,
        override val hashtag:String,
        override val variants: List<CaosVariant>
) : StubBase<CaosDefDocCommentHashtagImpl>(parent, CaosDefStubTypes.HASHTAG), CaosDefDocCommentHashtagStub