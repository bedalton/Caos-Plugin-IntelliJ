package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.types.CaosDefStubTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant


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
        override val variants:List<CaosVariant>
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
        override val lvalue: Boolean
) : StubBase<CaosDefDocCommentImpl>(parent, CaosDefStubTypes.DOC_COMMENT), CaosDefDocCommentStub

class CaosDefParameterStubImpl(
        parent:StubElement<*>,
        override val parameterName: String,
        override val type:CaosDefVariableTypeStruct,
        override val comment: String?
) : StubBase<CaosDefParameterImpl>(parent, CaosDefStubTypes.PARAMETER), CaosDefParameterStub

class CaosDefTypeDefinitionStubImpl(
        parent: StubElement<*>,
        override val typeName:String,
        override val keys: List<CaosDefTypeDefValueStruct>
) : StubBase<CaosDefTypeDefinitionElementImpl>(parent, CaosDefStubTypes.TYPE_DEFINITION_ELEMENT), CaosDefTypeDefinitionStub


class CaosDefTypeDefValueStubImpl(
        parent: StubElement<*>,
        override val key:String,
        override val value:String,
        override val description: String?,
        override val equality: TypeDefEq
) : StubBase<CaosDefTypeDefinitionImpl>(parent, CaosDefStubTypes.TYPE_DEFINITION_VALUE), CaosDefTypeDefValueStub


data class CaosDefParameterStruct(val parameterNumber:Int, val name: String, val type: CaosDefVariableTypeStruct, val comment: String? = null)

data class CaosDefReturnTypeStruct(val type: CaosDefVariableTypeStruct, val comment: String? = null)

data class CaosDefVariableTypeStruct(
        val type:String,
        val typedef: String? = null,
        val noteText: String? = null,
        val intRange:Pair<Int, Int>? = null,
        val length:Int? = null
)

data class CaosDefTypeDefValueStruct (
        val key:String,
        val value:String,
        val equality: TypeDefEq,
        val description:String? = null
)

class CaosDefDocCommentHashtagStubImpl(
        parent:StubElement<*>?,
        override val hashtag:String,
        override val variants: List<CaosVariant>
) : StubBase<CaosDefDocCommentHashtagImpl>(parent, CaosDefStubTypes.HASHTAG), CaosDefDocCommentHashtagStub