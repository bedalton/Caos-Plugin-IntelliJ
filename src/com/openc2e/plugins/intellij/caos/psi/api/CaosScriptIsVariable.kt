package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosScriptVarDeducer

interface CaosScriptIsVariable : CaosScriptCompositeElement

fun CaosScriptIsVariable.getInferredType() : CaosExpressionValueType
        = CaosScriptVarDeducer.getInferredType(this)