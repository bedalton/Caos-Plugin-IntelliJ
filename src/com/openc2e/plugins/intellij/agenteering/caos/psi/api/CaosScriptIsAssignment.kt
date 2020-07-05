package com.openc2e.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptIsAssignment : CaosScriptCompositeElement {
    val lvalue:CaosScriptLvalue?
}