package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.scope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.sharesScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.variable
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil


internal fun CaosScriptRvalue.resolveAgentClass(): AgentClass? {
    val scope = scope
    val offset = startOffset
    val parent = getParentOfType(CaosScriptScriptElement::class.java)
            ?: return null
    variable?.let {
        CaosScriptPsiImplUtil.getTargClass(it)
    }
    return when (rvaluePrime?.commandStringUpper) {
        "NORN" -> AgentClass(4, 0, 0)
        "OWNR" -> (parent as? CaosScriptEventScript)?.let {
            AgentClass(it.family, it.genus, it.species)
        }
        "CREA" -> if (variant == CaosVariant.C2) AgentClass(4, 0, 0) else null
        "_IT_" -> AgentClass.ZERO
        "AGNT" -> AgentClass.ZERO
        "CARR" -> AgentClass.ZERO
        "EDIT" -> AgentClass.ZERO
        "FROM" -> AgentClass.ZERO
        "HELD" -> AgentClass.ZERO
        "HHLD" -> AgentClass.CREATURE
        "HOTS" -> AgentClass.ZERO
        "IITT" -> AgentClass.ZERO
        "MTOA" -> AgentClass.ZERO
        "MTOC" -> AgentClass.CREATURE
        "NCLS", "PCLS" -> arguments.let {
            toAgentClass(
                    it.getOrNull(1),
                    it.getOrNull(2),
                    it.getOrNull(3)
            )
        }
        "NULL" -> null
        "OBJP" -> AgentClass.ZERO
        "PNTR" -> AgentClass.POINTER
        "PRT: FRMA" -> AgentClass.ZERO
        "SEEN" -> AgentClass.ZERO
        "TACK" -> AgentClass.ZERO
        "TCAR" -> AgentClass.ZERO
        "TRCK" -> AgentClass.ZERO
        "TWIN" -> (arguments.firstOrNull() as? CaosScriptRvalue)?.resolveAgentClass()
        "UNID" -> AgentClass.ZERO
        else -> PsiTreeUtil.collectElementsOfType(parent, CaosScriptAssignsTarg::class.java)
                .filter { it.startOffset < offset && scope.sharesScope(it.scope) }
                .sortedByDescending { it.startOffset }
                .mapNotNull {
                    it.targClass
                }
                .firstOrNull()
    }
}

fun CaosScriptAssignsTarg.isTargClass(classifier: AgentClass): Boolean {
    return isTargClass(classifier.family, classifier.genus, classifier.species)
}

fun CaosScriptAssignsTarg.isTargClass(family: Int, genus: Int, species: Int): Boolean {
    return classComponentMatches(this.family, family)
            && classComponentMatches(this.genus, genus)
            && classComponentMatches(this.species, species)
}
internal fun toAgentClass(family: PsiElement?, genus: PsiElement?, species: PsiElement?): AgentClass {
    return AgentClass(
            toAgentClassValue(family),
            toAgentClassValue(genus),
            toAgentClassValue(species)
    )
}

private fun toAgentClassValue(element: PsiElement?): Int {
    if (element == null)
        return 0
    element.text?.toIntSafe()?.let {
        return it
    }
    val rvalue = element.getSelfOrParentOfType(CaosScriptRvalue::class.java)
            ?: return 0
    return when (val value = rvalue.toCaosVar()) {
        is CaosVar.CaosLiteral.CaosInt -> value.value.toInt()
        else -> 0
    }
}

private fun classComponentMatches(value1: Int, value2: Int): Boolean {
    return value1 == 0 || value2 == 0 || value1 == value2
}