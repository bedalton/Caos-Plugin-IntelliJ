package com.openc2e.plugins.intellij.caos.psi.api

interface CaosScriptCommandElement : CaosScriptCompositeElement {
    val commandString:String
    val commandToken:CaosScriptIsCommandToken
    //val arguments:List<CaosScriptArgument>
}