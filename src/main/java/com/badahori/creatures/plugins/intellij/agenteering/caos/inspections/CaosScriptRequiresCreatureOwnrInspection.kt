package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor

class CaosScriptRequiresCreatureOwnrInspection : LocalInspectionTool(), DumbAware {
    override fun getDisplayName(): String = "Requires creature ownr object"
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = "RequiresCreatureOwnr"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitIsCommandToken(o: CaosScriptIsCommandToken) {
                super.visitIsCommandToken(o)
                annotateCommand(o, holder)
            }
        }
    }

    private fun annotateCommand(element: CaosScriptIsCommandToken, holder: ProblemsHolder) {
        val requiresCreatureOwnr = (element.parent as? CaosScriptCommandElement)?.commandDefinition?.requiresCreatureOwnr
            ?: return
        if (!requiresCreatureOwnr)
            return
        element.getParentOfType(CaosScriptEventScript::class.java)?.let { script: CaosScriptEventScript ->
            if (script.hasCreatureOwnr) {
                return
            }
        }
        holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-creature-ownr", element.commandString.uppercase()))
    }
}

/**
 * Helper function to determine whether an event script has a creature OWNR
 */
val CaosScriptEventScript.hasCreatureOwnr:Boolean get() {
    if (family.let { it == 0 || it == 4})
        return true
    val creatureOwnrScripts = variant?.creatureOwnrScripts
        ?: return true
    val eventNumber = eventNumber
    return eventNumber in creatureOwnrScripts
}


/**
 * Returns a list of Script numbers that have a creature OWNR object
 */
val CaosVariant.creatureOwnrScripts:List<Int> get() {
    return when (this) {
        CaosVariant.C1 -> c1CreatureOwnrEventNumbers
        CaosVariant.C2 -> c2CreatureOwnrEventNumbers
        else -> c2eCreatureOwnrEventNumbers
    }
}

private val c2eCreatureOwnrEventNumbers = (16..47) + (64..72) + 200
private val c1CreatureOwnrEventNumbers = (16..47) + (64..72)
private val c2CreatureOwnrEventNumbers = (16..29) + (32..45)+ (64..72) + 200