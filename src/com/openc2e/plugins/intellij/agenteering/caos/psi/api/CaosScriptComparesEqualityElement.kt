package com.openc2e.plugins.intellij.agenteering.caos.psi.api

import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqOp
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression

interface CaosScriptComparesEqualityElement : CaosScriptCompositeElement{
    val first:CaosScriptExpression
    val second:CaosScriptExpression
    val eqOp:CaosScriptEqOp
}