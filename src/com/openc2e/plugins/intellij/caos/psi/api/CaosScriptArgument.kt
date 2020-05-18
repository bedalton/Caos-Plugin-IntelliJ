package com.openc2e.plugins.intellij.caos.psi.api

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.deducer.CaosScope
import com.openc2e.plugins.intellij.caos.deducer.CaosVar

interface CaosScriptArgument : CaosScriptCompositeElement {
    val index:Int
    val expectedType:CaosScriptExpectedType
    fun toCaosVar(): CaosVar
}

enum class CaosScriptExpectedType(val value:Int) {
    INT(1),
    FLOAT(2),
    BYTE_STRING(3),
    TOKEN(4),
    AGENT(5),
    DECIMAL(7),
    C1_STRING(8),
    STRING(9),
    ANY(10),
    VARIABLE(11)
}