package com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.*
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant

interface CaosDefCommandDefinitionStub : StubElement<CaosDefCommandDefElementImpl> {
    val namespace:String?
    val command:String
    val commandWords:List<String>
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val rvalue:Boolean
    val lvalue:Boolean
    val isCommand:Boolean
    val comment:String?
    val variants:List<CaosVariant>
}

interface CaosDefDocCommentStub : StubElement<CaosDefDocCommentImpl> {
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val lvalue:Boolean
    val rvalue:Boolean
    val comment:String?
}

interface CaosDefDocCommentHashtagStub: StubElement<CaosDefDocCommentHashtagImpl> {
    val hashtag:String
    val variants:List<CaosVariant>
}

interface CaosDefTypeDefinitionStub : StubElement<CaosDefTypeDefinitionElementImpl> {
    val typeName:String
    val keys: List<CaosDefTypeDefValueStruct>
}

interface CaosDefTypeDefValueStub : StubElement<CaosDefTypeDefinitionImpl> {
    val key:String
    val equality:TypeDefEq
    val value:String
    val description:String?
}

enum class TypeDefEq {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN
}

interface CaosDefParameterStub : StubElement<CaosDefParameterImpl> {
    val parameterName:String
    val type:CaosDefVariableTypeStruct
    val comment:String?
}