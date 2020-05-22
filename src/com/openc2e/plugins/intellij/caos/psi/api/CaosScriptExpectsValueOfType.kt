package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

interface CaosScriptExpectsValueOfType : CaosScriptCompositeElement {
    val expectedType:CaosScriptExpectedType
}