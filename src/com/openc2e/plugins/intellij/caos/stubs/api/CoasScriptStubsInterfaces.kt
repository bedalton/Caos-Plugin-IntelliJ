package com.openc2e.plugins.intellij.caos.stubs.api

import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandCallImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandTokenImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptExpressionImpl
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType

interface CaosScriptCommandStub : StubElement<CaosScriptCommandImpl> {
    val command:String
    val commandTokens:List<String>
}

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandTokens:List<String>
    val numParameters:Int
}


interface CaosScriptCommandTokenStub : StubElement<CaosScriptCommandTokenImpl> {
    val text:String
    val index:Int
}

interface CaosScriptExpressionStub : StubElement<CaosScriptExpressionImpl> {
    val type:CaosScriptExpressionType
    val text:String
}