package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.JectScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.CaosScriptsQuickCollapseToLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getScripts
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

internal object FileInjectorUtil {

    internal suspend fun inject(
        project: Project,
        connection: CaosConnection,
        caosFile: CaosScriptFile,
        flags: Int,
        useJect: Boolean,
    ): InjectionStatus {
        if (useJect && connection.supportsJect) {
            return connection.injectWithJect(caosFile, flags)
        }
        val fileScripts = runReadAction {
            caosFile.getScripts()
        }

        val scriptBlocks = mutableListOf<List<CaosScriptScriptElement>>()
        if (flags hasFlag Injector.REMOVAL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptRemovalScript>()
            if (flags == Injector.REMOVAL_SCRIPT_FLAG && scripts.size == 1) {
                val caos = formatCaos(scripts[0].codeBlock) ?: ""
                return connection.inject(caos)
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
                    formatCaos(eventScript.codeBlock) ?: ""
                )
            }
            scriptBlocks.add(scripts)
        }

        if (flags hasFlag Injector.INSTALL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptMacroLike>()
            if (flags == Injector.INSTALL_SCRIPT_FLAG && scripts.size == 1) {
                return connection.inject(formatCaos(scripts[0].codeBlock) ?: "")
            }
            scriptBlocks.add(scripts)
        }
        return injectScriptBlocks(project, connection, scriptBlocks)
    }

    internal suspend fun inject(
        project: Project,
        connection: CaosConnection,
        scripts: Map<JectScriptType, List<CaosScriptScriptElement>>,
    ): InjectionStatus {
        val scriptBlocks = mutableListOf<List<CaosScriptScriptElement>>()
        scripts[JectScriptType.REMOVAL]?.apply {
            scriptBlocks.add(this)
        }
        scripts[JectScriptType.EVENT]?.apply {
            scriptBlocks.add(this)
        }
        scripts[JectScriptType.INSTALL]?.apply {
            scriptBlocks.add(this)
        }
        return injectScriptBlocks(project, connection, scriptBlocks)
    }

    private suspend fun injectScriptBlocks(
        project: Project,
        connection: CaosConnection,
        scriptBlocks: List<List<CaosScriptScriptElement>>,
    ): InjectionStatus {
        val responses = mutableListOf<String>()
        var oks = 0
        var error: InjectionStatus? = null
        scriptBlocks.mapAsync async@{ scripts ->
            if (scripts.isEmpty())
                return@async
            injectScriptList(
                project,
                connection,
                scripts
            ) { _, injectionStatus, script ->
                when (injectionStatus) {
                    is InjectionStatus.Ok -> {
                        oks++
                        if (injectionStatus.response.isNotBlank()) {
                            responses.add(injectionStatus.response)
                        }
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
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptScriptElement) -> Unit,
    ): Boolean {
        return runReadAction run@{
            for (script in scripts) {
                val ok = injectScript(
                    project, connection, script, callback
                )
                if (!ok) {
                    return@run false
                }
            }
            return@run true
        }
    }

    private inline fun injectScript(
        project: Project,
        connection: CaosConnection,
        script: CaosScriptScriptElement,
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptScriptElement) -> Unit,
    ): Boolean {
        val content = formatCaos(script.codeBlock)
        val result = if (script is CaosScriptEventScript) {
            connection.injectEventScript(
                family = script.family,
                genus = script.genus,
                species = script.species,
                eventNumber = script.eventNumber,
                caos = content ?: ""
            )
        } else if (content != null) {
            connection.inject(content)
        } else {
            return true
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
        is CaosScriptRemovalScript -> "Removal script"
        is CaosScriptEventScript -> "Event Script: $family $genus $species $eventNumber"
        is CaosScriptInstallScript -> "Install script"
        is CaosScriptMacro -> "Body script"
        else -> "Code block"
    }
}


internal val c1eElementsRegex =
    "tele|vrsn|xvec|yvec|say#|say\$|aim:|setv\\s+(?:clas|cls2|actv|attr|norn)|\\[[a-zA-Z0-9]*[a-zA-Z_\$\\-+#@!][^]]*]|var[0-9]|obv[0-9]|objp|doif\\s+(targ|norn|objp)\\s+(eq|ne)\\s+0|\\s+(?:bt|bf)|bbd:|dde:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
internal val c2eElementsRegex =
    "(\"(?:[^\"]|\\.)*\"|'(?:[^\'\\\\]|\\\\.)'|\\d+\\.\\d+|\\s\\.\\d+|mv[0-9]{2}|(?:do|el)if.+?(?:<>|<|>|<=|>=|=)|(?:eq|ne|<>|=)\\s+null|\\s+(?:seta|sets|adds))|net:|prt:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))

/**
 * Format the contents of a code block element
 */
private fun formatCaos(codeBlock: CaosScriptCodeBlock?): String? {
    if (codeBlock == null)
        return null
    val blockText = runReadAction {
        codeBlock.text
    } ?: return null
    return formatCaos(codeBlock.variant, blockText)
}

/**
 * Format CAOS for injection
 */
private fun formatCaos(variant: CaosVariant?, codeBlock: String?): String? {
    if (codeBlock.isNullOrBlank()) {
        return if (variant?.isNotOld != true)
            codeBlock?.trim()
        else
            codeBlock
    }
    // Check if C1e
    val isC1e = isC1e(variant, codeBlock).orTrue()
    return if (isC1e) {
        // if C1e, flatten and remove comments
        CaosScriptsQuickCollapseToLine.collapse(true, codeBlock)
    } else {
        // C2e, return just how it is
        codeBlock
    }
}

/**
 * Determine if C1e using variant if not null, and by checking code contents as fallback
 */
private fun isC1e(variant: CaosVariant?, codeBlock: String?): Boolean? {
    variant?.isOld?.let {
        return it
    }
    // If the code block is null or blank,
    // Then c1e/c2e status cannot be determined
    // return null so that caller can decide what to do
    if (codeBlock.isNullOrBlank()) {
        return null
    }

    // Check if C1e
    val isC1e = c1eElementsRegex.find(codeBlock) != null
    // Check if C2e
    val isC2e = c2eElementsRegex.find(codeBlock) != null

    return if (isC1e && !isC2e) {
        true
    } else if (!isC1e && isC2e) {
        false
    } else if (isC1e) {
        LOGGER.warning("Inconsistent results from check variant regexes; \"isC1e\" and \"isC2e\" both returned true;\n\tC1eRegex: ${c1eElementsRegex.pattern}\n\tC2eRegex: ${c2eElementsRegex.pattern}")
        null
    } else {
        // Neither check returned a positive result, return null so caller can deal with it
        null
    }
}