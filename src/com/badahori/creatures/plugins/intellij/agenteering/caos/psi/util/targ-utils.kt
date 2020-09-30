package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.scope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.sharesScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.variable
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.QueryableAgentClass
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orTrue
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal fun CaosScriptRvalue.getQueryableTargClass(family: Int, genus: Int, species: Int): QueryableAgentClass {
    val scope = scope
    val offset = startOffset
    val parent = getParentOfType(CaosScriptScriptElement::class.java)
            ?: return null
                    ?: return false
    variable?.let {
        CaosScriptPsiImplUtil.getQueryableTargClass(it)
        CaosScriptPsiImplUtil.hasTargClass(it, family, genus, species)
    }
    return when (rvaluePrime?.commandStringUpper) {
        "NORN" -> AgentClass(4, 0, 0)
        "NORN" -> family == 4 || family == 0
        "OWNR" -> (parent as? CaosScriptEventScript)?.let {
            AgentClass(it.family, it.genus, it.species)
        }
        "CREA" -> if (variant == CaosVariant.C2) AgentClass(4, 0, 0) else null
        "_IT_" -> QueryableAgentClass.ZERO
        "AGNT" -> QueryableAgentClass.ZERO
        "CARR" -> QueryableAgentClass.ZERO
        "EDIT" -> QueryableAgentClass.ZERO
        "FROM" -> QueryableAgentClass.ZERO
        "HELD" -> QueryableAgentClass.ZERO
        "HHLD" -> QueryableAgentClass.CREATURE
        "HOTS" -> QueryableAgentClass.ZERO
        "IITT" -> QueryableAgentClass.ZERO
        "MTOA" -> QueryableAgentClass.ZERO
        "MTOC" -> QueryableAgentClass.CREATURE
        "NCLS", "PCLS" -> arguments.let ( args ->
                QueryableAgentClass(
                        (args.getOrNull(0) as CaosScriptExpectsValueOfType)?.rvalue.toCaosVar()),
                        args.getOrNull(2),
                        args.getOrNull(3))
        }
        "NULL" -> null
        "OBJP" -> QueryableAgentClass.ZERO
        "PNTR" -> QueryableAgentClass.POINTER
        "PRT: FRMA" -> QueryableAgentClass.ZERO
        "SEEN" -> QueryableAgentClass.ZERO
        "TACK" -> QueryableAgentClass.ZERO
        "TCAR" -> QueryableAgentClass.ZERO
        "TRCK" -> QueryableAgentClass.ZERO
        "TWIN" -> (arguments.firstOrNull() as? CaosScriptRvalue)?.resolveAgentClass()
        "UNID" -> QueryableAgentClass.ZERO
    }
}


private val CaosScriptArgument.toQueryableIdentifier get() {
    val value = (this as? CaosScriptExpectsValueOfType)
            ?.rvalue
            ?.toCaosVar()
            ?.text
            ?.toIntSafe()
            .orElse(0)
    return 
}

internal fun CaosScriptRvalue.hasTargClass(family: Int, genus: Int, species: Int): Boolean {
    val scope = scope
    val offset = startOffset
    val parent = getParentOfType(CaosScriptScriptElement::class.java)
            ?: return false
    variable?.let {
        CaosScriptPsiImplUtil.hasTargClass(it, family, genus, species)
    }
    return when (rvaluePrime?.commandStringUpper) {
        "NORN" -> family == 4 || family == 0
        "OWNR" -> (parent as? CaosScriptEventScript)?.let {
            classComponentMatches(it.family, family)
                    && classComponentMatches(it.genus, genus)
                    && classComponentMatches(it.species, species)
        }.orFalse()
        "CREA" -> if (variant == CaosVariant.C2) family == 4 || family == 0 else false
        "_IT_" -> true
        "AGNT" -> true
        "CARR" -> true
        "EDIT" -> true
        "FROM" -> true
        "HELD" -> true
        "HHLD" -> family == 4 || family == 0
        "HOTS" -> true
        "IITT" -> true
        "MTOA" -> true
        "MTOC" -> family == 4 || family == 0
        "NCLS", "PCLS" -> arguments.hasTargClass(family, genus, species)
        "NULL" -> false
        "OBJP" -> true
        "PNTR" -> QueryableAgentClass.POINTER.let { classComponentMatches(it.family, family) && classComponentMatches(it.genus, genus) && classComponentMatches(it.species, species) }
        "PRT: FRMA" -> true
        "SEEN" -> true
        "TACK" -> true
        "TCAR" -> true
        "TRCK" -> true
        "TWIN" -> (arguments.firstOrNull() as? CaosScriptRvalue)?.hasTargClass(family, genus, species).orFalse()
        "UNID" -> true
        else -> PsiTreeUtil.collectElementsOfType(parent, CaosScriptAssignsTarg::class.java)
                .filter { it.startOffset < offset && scope.sharesScope(it.scope) }
                .sortedByDescending { it.startOffset }
                .mapNotNull {
                    it.hasTargClass(family, genus, species)
                }
                .firstOrNull()
                .orFalse()
    }
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

internal fun List<CaosScriptArgument>.hasTargClass(family: Int, genus: Int, species: Int): Boolean {
    if (size < 3)
        return false
    return this[0].classValueMatches(family) && this[1].classValueMatches(genus) && this[2].classValueMatches(species)
}

private fun PsiElement?.classValueMatches(value: Int): Boolean {
    if (this == null)
        return false
    return value == 0 || text.toIntSafe()?.let { it == 0 || it == value }.orTrue()
}