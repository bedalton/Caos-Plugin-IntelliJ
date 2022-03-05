package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobVirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager


open class AgentScript(val code: String, val scriptName: String, val type: AgentScriptType) {
    class InstallScript(code: String, scriptName: String = "Install Script") : AgentScript(code, scriptName, AgentScriptType.INSTALL)
    class RemovalScript(code: String, scriptName: String = "Removal Script") : AgentScript(code, scriptName, AgentScriptType.REMOVAL)
    fun toCaosFile(project: Project, cobPath: CaosVirtualFile, caosVariant: CaosVariant): CaosScriptFile {
        val fileName = getEventScriptName(code) ?: scriptName
        val file = CobVirtualFileUtil.createChildCaosScript(project, cobPath, caosVariant, "$fileName.cos", code).apply {
            isWritable = false
        }
        val psiFile = (PsiManager.getInstance(project).findFile(file) as? CaosScriptFile)
                ?: PsiFileFactory.getInstance(project)
                        .createFileFromText("$scriptName.cos", CaosScriptLanguage, code) as CaosScriptFile
        psiFile.setVariant(caosVariant, true)
        return psiFile
    }

    companion object {
        private fun getEventScriptName(code:String) : String? {
            val codeToLower = code.lowercase().trim(' ','\t', ',', '\n')
            val splits = "([,\\s]+scrp|^scrp)".toRegex()
                    .splitWithDelimiter(codeToLower)
                    .map { it.trim(' ','\t', ',', '\n')}
            val valid = mutableListOf<String>()
            var i = if (codeToLower.startsWith("scrp")) 0 else 1
            while(++i < splits.size) {
                val prev = splits[i++].trim()
                if (prev == "scrp") {
                    // Only 4 parts needed as SCRP was removed earlier, and have
                    valid.add(splits[i++].split("[\\s,]+".toRegex(), 5).drop(0).dropLast(1).joinToString(" "))
                }
                if (splits.getOrNull(i)?.endsWith("dde:").orFalse())
                    i++
            }
            return if (valid.size == 1)
                valid[0]
            else
                null
        }
    }
}
private const val withDelimiter = "((?<=%1\$s)|(?=%1\$s))"

fun Regex.splitWithDelimiter(input: CharSequence) =
        Regex(withDelimiter.format(this.pattern)).split(input)

enum class AgentScriptType {
    INSTALL,
    REMOVAL,
    EVENT,
    OBJECT
}

fun <T:AgentScript> T.nullIfEmpty(): T? {
    return if (this.code.isBlank())
        null
    else
        this
}