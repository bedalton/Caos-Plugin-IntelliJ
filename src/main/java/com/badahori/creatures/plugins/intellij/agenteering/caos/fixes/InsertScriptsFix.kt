package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.log.Log
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object InsertScriptsFix {

    fun getQuickFix(file: PsiFile, family: Int, genus: Int, species: Int, events: List<Int>): LocalQuickFix {
        val fixText = CaosBundle.message(
            "caos.inspection.has-behavior-scripts.quickfix.insert-scripts",
            family,
            genus,
            species,
            events.joinToString()
        )
        return getQuickFix(
            file,
            fixText,
            family,
            genus,
            species,
            events
        )
    }

    fun getQuickFix(file: PsiFile, fixText: String, family: Int, genus: Int, species: Int, events: List<Int>): LocalQuickFix {

        val classifier = AgentClass(family, genus, species)

        val scriptDescriptors = events.map { event -> Triple(classifier, event, "    \n") }

        val scripts = PsiTreeUtil
            .collectElementsOfType(file, CaosScriptScriptElement::class.java)

        if (scripts.isEmpty()) {
            return CaosScriptInsertEventScriptsFix(
                file,
                fixText,
                true,
                null,
                scriptDescriptors,
            )
        }

        val eventScripts = scripts
            .filterIsInstance<CaosScriptEventScript>()

        val lastEventScriptForClassifier = eventScripts
            .filter { it.family == family && it.genus == genus && it.species == species }
            .maxByOrNull { it.startOffset }

        if (lastEventScriptForClassifier != null) {
            return CaosScriptInsertEventScriptsFix(
                file,
                fixText,
                true,
                lastEventScriptForClassifier,
                scriptDescriptors,
            )
        }


        val lastEventScript = eventScripts
            .maxByOrNull { it.startOffset }
        if (lastEventScript != null) {
            return CaosScriptInsertEventScriptsFix(
                file,
                fixText,
                true,
                lastEventScript,
                scriptDescriptors,
            )
        }

        val rscr = scripts
            .filterIsInstance<CaosScriptRemovalScript>()
            .minByOrNull { it.startOffset }

        if (rscr != null) {
            return CaosScriptInsertEventScriptsFix(
                file,
                fixText,
                false,
                rscr,
                scriptDescriptors,
            )
        }

        val lastScript = scripts.maxByOrNull { it.startOffset }

        if (lastScript != null) {
            return CaosScriptInsertEventScriptsFix(
                file,
                fixText,
                true,
                lastScript,
                scriptDescriptors,
            )
        }

        return CaosScriptInsertEventScriptsFix(
            file,
            fixText,
            true,
            null,
            scriptDescriptors,
        )
    }

}