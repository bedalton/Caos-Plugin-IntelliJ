package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.InsertScriptsFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptEventScriptIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasFlag
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.bedalton.common.util.filterNotNull
import com.bedalton.log.Log
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptHasBehaviorScriptsInspection : LocalInspectionTool() {

    override fun getDisplayName(): String {
        return CaosBundle.message("caos.inspection.has-behavior-scripts.name")
    }

    override fun getGroupDisplayName(): String = CAOSScript

    override fun getShortName(): String {
        return "AgentHasRequiredBehaviorScripts"
    }

    private val requiredScripts = mapOf(
        1 to Pair(1, "Activate 1"),
        2 to Pair(2, "Activate 2"),
        4 to Pair(0, "Deactivate"),
        8 to Pair(3, "Hit"),
        16 to Pair(12, "Eat")
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCommandCall(o: CaosScriptCommandCall) {
                onCommandCall(o, holder)
            }
        }
    }


    private fun onCommandCall(call: CaosScriptCommandCall, problemsHolder: ProblemsHolder) {

        if (call.variant?.isNotOld == false) {
            return
        }

        val commandString = call.commandStringUpper
        if (commandString != "BHVR") {
            return
        }

        val argument = call
            .arguments
            .getOrNull(0)
            ?: return

        val value = argument.text
            ?.toIntOrNull()
            ?: return

        val flags = requiredScripts
            .mapNotNull { (key, data) ->
                if (value.hasFlag(key)) {
                    data
                } else {
                    null
                }
            }
            .nullIfEmpty()
            ?: return

        val parentScript = call.getParentOfType(CaosScriptScriptElement::class.java)
        val callStartOffset = call.startOffset

        val rawElements = PsiTreeUtil
            .collectElementsOfType(parentScript, CaosScriptAssignsTarg::class.java)
            .filter { it.endOffset <= callStartOffset}

        val ownr = call.getParentOfType(CaosScriptEventScript::class.java)
            ?.targClassifier

        val classifier = rawElements
            .map {
                // After enum, targ switches back to ownr
                if (it is CaosScriptEnumNextStatement || it is CaosScriptEnumSceneryStatement) {
                    if (it.textRange.contains(call.startOffset)) {
                        it.targClassifier
                    } else {
                        ownr
                    }
                } else {
                    it.targClassifier
                }
            }
            .filterNotNull()
            .reversed()
            .firstOrNull()
            ?: ownr
            ?: return

        if (classifier == CaosScriptAssignsTarg.UNPARSABLE_TARG) {
            return
        }

        if (classifier.family <= 0 || classifier.genus <= 0 || classifier.species <= 0) {
            return
        }

        val eventScriptsInFile = PsiTreeUtil.collectElementsOfType(call.containingFile, CaosScriptEventScript::class.java)
            .filter {
                (it.family == classifier.family || it.family == 0)
                        && (it.genus == classifier.genus || it.genus == 0)
                        && (it.species == classifier.species || it.species == 0)
            }
            .map {
                it.eventNumber
            }
            .toSet()

        val project = call.project
        val variant = call.variant
        val missing = mutableListOf<Int>()
        var missingText = ""
        for ((event, name) in flags) {
            val exists = event in eventScriptsInFile || CaosScriptEventScriptIndex
                .instance[variant, classifier.family, classifier.genus, classifier.species, event, project]
                .isNotEmpty()

            if (exists) {
                continue
            }

            missingText += ", $event ($name)"
            missing.add(event)
        }

        if (missingText.isEmpty() || missing.isEmpty()) {
            return
        }

        missingText = missingText.substring(1)
        val fixText = CaosBundle.message(
            "caos.inspection.has-behavior-scripts.quickfix.insert-scripts",
            classifier.family,
            classifier.genus,
            classifier.species,
            missingText
        )
        val fix = InsertScriptsFix.getQuickFix(
            call.containingFile,
            fixText,
            classifier.family,
            classifier.genus,
            classifier.species,
            missing
        )
        val errorMessage = CaosBundle.message(
            "caos.inspection.has-behavior-scripts.missing-scripts",
            classifier.family,
            classifier.genus,
            classifier.species,
            missingText
        )
        Log.i(errorMessage)
        problemsHolder.registerProblem(argument, errorMessage, fix)
    }

}