package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeNone
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptAgentToIntCoercionInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Agent to Int Coercion"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "AgentToIntCoercion"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitRvaluePrime(o: CaosScriptRvaluePrime) {
                annotateArgument(o, holder)
                super.visitRvaluePrime(o)
            }

            override fun visitLvalue(o: CaosScriptLvalue) {
                annotateArgument(o, holder)
                super.visitLvalue(o)
            }
        }
    }

    companion object {

        private val returnsAgent: MutableMap<CaosVariant, Set<String>> = mutableMapOf()

        private fun commandReturnsAgent(variant: CaosVariant, commandString: String): Boolean {
            val commandReturnsAgent = returnsAgent[variant]?.let { commands ->
                commandString likeAny commands
            }
            // Value can be nullable if not cached,
            // so have explicit check to true to ensure it is not null and true
            if (commandReturnsAgent == true)
                return true

            val commands = CaosLibs[variant].allCommands.filter { it.returnType == CaosExpressionValueType.AGENT }
                    .map { it.command }
                    .toSet()
                    .let { commands ->
                        returnsAgent[variant] = commands
                        commands
                    }
            return commandString likeAny commands
        }

        private fun annotateArgument(childCommand: CaosScriptCommandElement, problemsHolder: ProblemsHolder) {
            val token = childCommand.commandString
                    ?: return

            // Assert has variant, as this is needed for command and parameter check
            val variant = childCommand.variant
                    ?: return

            // Check that this command does indeed return an agent
            if (!commandReturnsAgent(variant, token))
                return

            // Get command or parent as argument rvaluePrime is not an argument, but lvalue is
            val argument = (childCommand as? CaosScriptArgument ?: childCommand.parent as? CaosScriptArgument)
                    ?: return

            // Get parent command for this argument
            val parentCommand = argument.parent as? CaosScriptCommandElement
                    ?: return

            // Get this arguments parameter information
            val parameter = parentCommand.commandDefinition?.parameters?.getOrNull(argument.index)
                    ?: return
            // Check first for if needs agent to prevent having to search for all number types
            val expectedType = parameter.type

            // If expected parameter is not a number, it will be handled by another inspection
            if (!expectedType.isNumberType)
                return
            // Create a message stating agent to reference coercion
            val message = CaosBundle.message("caos.annotator.command-annotator.agent-to-integer-coercion")
            // Register message as information and not error.
            // TODO should this be a weak warning. People using it this way might know what they are doing
            // Perhaps should make this warning a second inspection
            problemsHolder.registerProblem(childCommand.commandToken ?: childCommand, message)
        }
    }
}