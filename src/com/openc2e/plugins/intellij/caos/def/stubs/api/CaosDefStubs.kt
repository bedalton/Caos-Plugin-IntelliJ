package com.openc2e.plugins.intellij.caos.def.stubs.api

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefCommandDefElementImpl
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefDocCommentImpl
import com.openc2e.plugins.intellij.caos.def.psi.impl.CaosDefParameterImpl
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefReturnTypeStruct

interface CaosDefCommandDefinitionStub : StubElement<CaosDefCommandDefElementImpl> {
    val namespace:String?
    val command:String
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val rvalue:Boolean
    val lvalue:Boolean
    val isCommand:Boolean
    val comment:String?
}

interface CaosDefDocCommentStub : StubElement<CaosDefDocCommentImpl> {
    val command:String
    val parameters:List<CaosDefParameterStruct>
    val returnType:CaosDefReturnTypeStruct
    val lvalue:Boolean
    val rvalue:Boolean
    val comment:String?
}

interface CaosDefParameterStub : StubElement<CaosDefParameterImpl> {
    val parameterName:String
    val parameterType:String
    val typeDefType:String?
    val typeNote:String?
    val parameterStruct:CaosDefParameterStruct
}