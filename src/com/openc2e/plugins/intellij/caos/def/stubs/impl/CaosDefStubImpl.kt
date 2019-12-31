package com.openc2e.plugins.intellij.caos.def.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefDocCommentImpl
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefParameterImpl
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefCommandDefinitionStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefDocCommentStub
import com.openc2e.plugins.intellij.caos.def.stubs.api.CaosDefParameterStub
import com.openc2e.plugins.intellij.caos.def.stubs.types.CaosDefStubTypes


class CaosDefCommandDefinitionStubImpl(
        parent: StubElement<*>,
        override val namespace: String?,
        override val command: String,
        override val parameters: List<CaosDefParameterStruct>,
        override val comment: String?,
        override val returnType: CaosDefReturnTypeStruct,
        override val rvalue: Boolean,
        override val lvalue: Boolean,
        override val isCommand: Boolean
) : StubBase<CaosDefCommandDefElementImpl>(parent, CaosDefStubTypes.COMMAND_ELEMENT), CaosDefCommandDefinitionStub


class CaosDefDocCommentStubImpl(
        parent: StubElement<*>,
        override val command: String,
        override val parameters: List<CaosDefParameterStruct>,
        override val comment: String?,
        override val returnType: CaosDefReturnTypeStruct,
        override val rvalue: Boolean,
        override val lvalue: Boolean
) : StubBase<CaosDefDocCommentImpl>(parent, CaosDefStubTypes.DOC_COMMENT), CaosDefDocCommentStub

class CaosParameterStubImpl(
        parent:StubElement<*>,
        override val parameterName: String,
        override val parameterType: String,
        override val typeDefType: String?,
        override val typeNote: String?,
        override val parameterStruct: CaosDefParameterStruct
) : StubBase<CaosDefParameterImpl>(parent, CaosDefStubTypes.PARAMETER), CaosDefParameterStub

data class CaosDefParameterStruct(val name: String, val type: CaosDefVariableTypeStruct, val comment: String?)

data class CaosDefReturnTypeStruct(val type: CaosDefVariableTypeStruct, val comment: String?)

data class CaosDefVariableTypeStruct(
        val type:String,
        val typedef: String? = null,
        val noteText: String? = null,
        val comment: String? = null,
        val min:Int? = null,
        val max:Int? = null
)