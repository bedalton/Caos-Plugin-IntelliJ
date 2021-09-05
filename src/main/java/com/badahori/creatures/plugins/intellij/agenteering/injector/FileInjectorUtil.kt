package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getScripts
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.utils.escapeHTML
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasFlag
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

internal object FileInjectorUtil {

    internal fun inject(
        project: Project,
        connection: CaosConnection,
        caosFile: CaosScriptFile,
        flags: Int,
        useJect: Boolean
    ): InjectionStatus {
        if (useJect && connection.supportsJect) {
            return connection.injectWithJect(caosFile, flags)
        }
        val responses = mutableListOf<String>()
        var oks = 0
        var error: InjectionStatus? = null
        val fileScripts = caosFile.getScripts()

        val scriptBlocks = mutableListOf<List<CaosScriptScriptElement>>()
        if (flags hasFlag Injector.REMOVAL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptRemovalScript>()
            if (flags == Injector.REMOVAL_SCRIPT_FLAG && scripts.size == 1) {
                return connection.inject(scripts[0].codeBlock?.text ?: "")
            }
            scriptBlocks.add(scripts)
        }
        if (flags hasFlag Injector.EVENT_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptEventScript>()
            if (flags == Injector.EVENT_SCRIPT_FLAG && scripts.size == 1) {
                val eventScript = scripts.first()
                return connection.injectEventScript(
                    eventScript.family,
                    eventScript.genus,
                    eventScript.species,
                    eventScript.eventNumber,
                    eventScript.codeBlock?.text ?: ""
                )
            }
            scriptBlocks.add(scripts)
        }

        if (flags hasFlag Injector.INSTALL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptMacroLike>()
            if (flags == Injector.INSTALL_SCRIPT_FLAG && scripts.size == 1) {
                connection.inject(scripts[0].codeBlock?.text ?: "")
            }
            scriptBlocks.add(scripts)
        }

        for (scripts in scriptBlocks) {
            if (scripts.isEmpty())
                continue
            injectScriptList(
                project,
                connection,
                scripts
            ) { _, injectionStatus, script ->
                when (injectionStatus) {
                    is InjectionStatus.Ok -> {
                        oks++
                        responses.add(injectionStatus.response)
                    }
                    is InjectionStatus.Bad -> {
                        error = injectionStatus.copy(
                            error = "Inject ${script.getDescriptor()} failed with error:\n${injectionStatus.error}"
                        )
                    }
                    is InjectionStatus.BadConnection -> {
                        error = injectionStatus.copy(
                            error = "Inject ${script.getDescriptor()} failed with bad connection error:\n${injectionStatus.error}"
                        )
                    }
                }
                if (error != null)
                    throw CaosInjectorExceptionWithStatus(error)
            }
        }
        return InjectionStatus.Ok(
            response = responses.joinToString("\n")
        )

    }

    private inline fun injectScriptList(
        project: Project,
        connection: CaosConnection,
        scripts: Collection<CaosScriptScriptElement>,
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptScriptElement) -> Unit
    ): Boolean {
        return runReadAction run@{
            for (script in scripts) {
                val ok = injectScript(
                    project, connection, script, callback
                )
                if (!ok)
                    return@run false
            }
            return@run true
        }
    }

    private inline fun injectScript(
        project: Project,
        connection: CaosConnection,
        script: CaosScriptScriptElement,
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptScriptElement) -> Unit
    ): Boolean {
        val content = script
            .codeBlock
            ?.text
            ?.nullIfEmpty()
            ?: return true
        val result = if (script is CaosScriptEventScript) {
            connection.injectEventScript(
                family = script.family,
                genus = script.genus,
                species = script.species,
                eventNumber = script.eventNumber,
                caos = content
            )
        } else {
            connection.inject(content)
        }
        callback(project, result, script)
        return result is InjectionStatus.Ok
    }

}


/**
 * Processes CAOS injection response, and notifies the user
 */
internal fun onCaosResponse(project: Project, response: InjectionStatus?) {
    when (response) {
        is InjectionStatus.Ok -> postOk(project, response)
        is InjectionStatus.BadConnection -> postError(project, "Connection Failed", response.error)
        is InjectionStatus.Bad -> postError(project, "Injection Failed", response.error)
        else -> postError(project, "Invalid response", "Caos injection failed to respond")
    }
}


/**
 * Responsible for displaying an OK status response to the user
 */
internal fun postOk(project: Project, response: InjectionStatus.Ok) {
    val prefix = "&gt;"
    val responseString = response.response
    val message = if (responseString.isBlank())
        ""
    else
        "<pre>\n$prefix" + responseString.split("\n").joinToString("\n$prefix").escapeHTML() + "</pre>"
    invokeLater {
        CaosInjectorNotifications.show(project, "Injection Success", message, NotificationType.INFORMATION)
    }
}

/**
 * Logs an INFO based message to the user such as empty CAOS string
 */
internal fun postInfo(project: Project, title: String, message: String) {
    invokeLater {
        CaosInjectorNotifications.show(project, title, message, NotificationType.INFORMATION)
    }
}

/**
 * Posts an ERROR message to the user.
 */
internal fun postError(project: Project, title: String, message: String) {
    invokeLater {
        CaosInjectorNotifications.show(project, title, message, NotificationType.ERROR)
    }
}

/**
 * Logs warning message to users CAOS notification panel
 */
@Suppress("unused")
internal fun postWarning(project: Project, title: String, message: String) {
    invokeLater {
        CaosInjectorNotifications.show(project, title, message, NotificationType.WARNING)
    }
}


private fun CaosScriptScriptElement.getDescriptor(): String {
    return when (this) {
        is CaosScriptRemovalScript -> "Removal script, line: $lineNumber"
        is CaosScriptEventScript -> "Event Script: $family $genus $species $eventNumber, line: $lineNumber"
        is CaosScriptInstallScript -> "Install script, line: $lineNumber"
        is CaosScriptMacro -> "Body script, line: $lineNumber"
        else -> "Code block, line: $lineNumber"
    }
}