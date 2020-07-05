package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil

interface CaosScriptIsVariable : CaosScriptCompositeElement

fun CaosScriptIsVariable.getInferredType() : CaosExpressionValueType
        = CaosScriptInferenceUtil.getInferredType(this)