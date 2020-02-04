package com.openc2e.plugins.intellij.caos.psi.api

interface CaosScriptCommandElement {
    val command:String;
    val arguments:List<CaosScriptArgument>
}