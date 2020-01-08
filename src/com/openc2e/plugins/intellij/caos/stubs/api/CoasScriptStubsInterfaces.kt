package com.openc2e.plugins.intellij.caos.stubs.api

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.psi.impl.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
/*
interface CaosScriptCommandStub : StubElement<CaosScriptCommandImpl> {
    val command:String
    val commandTokens:List<String>
}

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandTokens:List<String>
    val parameterTypes:List<CaosScriptExpressionType>
    val numParameters:Int
}

*/
interface CaosScriptCommandTokenStub : StubElement<CaosScriptCommandTokenImpl> {
    val text:String
    val index:Int
}

interface CaosScriptExpressionStub : StubElement<CaosScriptExpressionImpl> {
    val type:CaosScriptExpressionType
    val text:String
}

interface CaosScriptVarTokenStub : StubElement<CaosScriptVarTokenImpl> {
    val varGroup:CaosScriptVarTokenGroup
    val varIndex:Int?
}