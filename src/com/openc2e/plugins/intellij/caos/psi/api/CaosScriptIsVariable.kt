package com.openc2e.plugins.intellij.caos.psi.api

import com.openc2e.plugins.intellij.caos.deducer.CaosScriptInferenceUtil

interface CaosScriptIsVariable : CaosScriptCompositeElement

fun CaosScriptIsVariable.getInferredType() : CaosExpressionValueType
        = CaosScriptInferenceUtil.getInferredType(this)