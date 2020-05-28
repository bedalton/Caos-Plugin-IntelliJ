package com.openc2e.plugins.intellij.caos.psi.api

interface CaosScriptIsAssignment : CaosScriptCompositeElement {
    val lvalue:CaosScriptLvalue?
}