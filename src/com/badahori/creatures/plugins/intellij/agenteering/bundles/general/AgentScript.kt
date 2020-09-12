package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.parser.CaosScriptParserDefinition
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import java.util.*


open class AgentScript(val code: String, val scriptName: String, val type:AgentScriptType) {
    class EventScript(val family:Int, val genus:Int, val species:Int, val eventNumber:Int, code: String) : AgentScript(code, "$family $genus $species $eventNumber", AgentScriptType.EVENT) {
        companion object {
            fun parse(project:Project, variant:CaosVariant, code:String) : AgentScript {
                val uuid = UUID.randomUUID().toString()
                val psiFile = PsiFileFactory.getInstance(project)
                        .createFileFromText("$uuid.cos", CaosScriptLanguage, code) as CaosScriptFile
                psiFile.variant = variant
                val eventScript = PsiTreeUtil.collectElementsOfType(psiFile, CaosScriptEventScript::class.java)
                        .firstOrNull()
                        ?: return AgentScript(code, "Script", AgentScriptType.OBJECT)
                return EventScript(eventScript.family, eventScript.genus, eventScript.species, eventScript.eventNumber, code)
            }
        }
    }
    class InstallScript(code:String, scriptName:String = "InstallScript") : AgentScript(code, scriptName, AgentScriptType.INSTALL)
    class RemovalScript(code:String, scriptName:String = "InstallScript") : AgentScript(code, scriptName, AgentScriptType.REMOVAL)
    fun toCaosFile(project: Project, caosVariant: CaosVariant): CaosScriptFile {
        val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("$scriptName.cos", CaosScriptLanguage, code) as CaosScriptFile
        psiFile.variant = caosVariant
        /*val provider = object : SingleRootFileViewProvider(PsiManager.getInstance(project),
                psiFile.virtualFile,
                false) {
            override fun supportsIncrementalReparse(rootLanguage: Language): Boolean {
                return false
            }
        }*/
        return psiFile
    }
}

enum class AgentScriptType {
    INSTALL,
    REMOVAL,
    EVENT,
    OBJECT
}