package com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api

import com.intellij.psi.stubs.StubElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefValuesListValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType

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
    val requiresOwner:Boolean
    val simpleReturnType: CaosExpressionValueType
}

interface CaosDefDocCommentStub : StubElement<CaosDefDocCommentImpl> {
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val lvalue:Boolean
    val rvalue:Boolean
    val requiresOwner:Boolean
    val comment:String?
}

interface CaosDefDocCommentHashtagStub: StubElement<CaosDefDocCommentHashtagImpl> {
    val hashtag:String
    val variants:List<CaosVariant>
}

interface CaosDefValuesListStub : StubElement<CaosDefValuesListElementImpl> {
    val listName:String
    val keys: List<CaosDefValuesListValueStruct>
    val typeNote:String?
    val isBitflags:Boolean
}

interface CaosDefValuesListValueStub : StubElement<CaosDefValuesListValueImpl> {
    val key:String
    val equality:ValuesListEq
    val value:String
    val description:String?
}

enum class ValuesListEq {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN
}

interface CaosDefParameterStub : StubElement<CaosDefParameterImpl> {
    val parameterName:String
    val type:CaosDefVariableTypeStruct
    val comment:String?
    val simpleType: CaosExpressionValueType
}