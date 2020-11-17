package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiTarget

interface CaosScriptIsVariable : CaosScriptCompositeElement, PsiNamedElement, PsiTarget

fun CaosScriptIsVariable.getInferredType() : CaosExpressionValueType
        = CaosScriptInferenceUtil.getInferredType(this) ?: CaosExpressionValueType.VARIABLE