package com.openc2e.plugins.intellij.caos.def.stubs.api

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.*
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefVariableTypeStruct

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
    val variants:List<String>
}

interface CaosDefDocCommentStub : StubElement<CaosDefDocCommentImpl> {
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val lvalue:Boolean
    val rvalue:Boolean
    val comment:String?
}

interface CaosDefTypeDefinitionStub : StubElement<CaosDefTypeDefinitionElementImpl> {
    val typeName:String
    val keys: List<CaosDefTypeDefValueStruct>
}

interface CaosDefTypeDefValueStub : StubElement<CaosDefTypeDefinitionImpl> {
    val key:String
    val value:String
    val description:String?
}

interface CaosDefParameterStub : StubElement<CaosDefParameterImpl> {
    val parameterName:String
    val type:CaosDefVariableTypeStruct
    val comment:String?
}