package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.bedalton.common.util.className
import com.bedalton.common.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.JectScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.CaosScriptsQuickCollapseToLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getScripts
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.navigation.NavigationItem
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import java.io.File

internal object FileInjectorUtil {

    internal fun inject(
        project: Project,
        connection: CaosConnection,
        caosFile: CaosScriptFile,
        totalFiles: Int,
        flags: Int,
        useJect: Boolean,
    ): InjectionStatus {
        if (useJect && connection.supportsJect) {
            return connection.injectWithJect(caosFile, flags)
        }

        val fileScripts = runReadAction {
            caosFile.getScripts()
        }

        val canInjectSingleScript = runReadAction {
            caosFile.collectElementsOfType(CaosScriptCaos2Command::class.java)
                .none { it.commandName.equalsIgnoreCase("Link") }
        }



        val scriptBlocks = mutableMapOf<JectScriptType, List<CaosScriptScriptElement>>()
        if (flags hasFlag Injector.REMOVAL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptRemovalScript>()
            if (canInjectSingleScript && flags == Injector.REMOVAL_SCRIPT_FLAG && scripts.size == 1) {
                return injectSingle(connection, caosFile.name, scripts[0])
            }
            scriptBlocks[JectScriptType.REMOVAL] = scripts
        }
        if (flags hasFlag Injector.EVENT_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptEventScript>()
            if (canInjectSingleScript && canInjectSingleScript && flags == Injector.EVENT_SCRIPT_FLAG && scripts.size == 1) {
                return injectSingleEventScript(connection, caosFile.name, scripts.first())

            }
            scriptBlocks[JectScriptType.EVENT] = scripts
        }

        if (flags hasFlag Injector.INSTALL_SCRIPT_FLAG) {
            val scripts = fileScripts.filterIsInstance<CaosScriptMacroLike>()
            if (canInjectSingleScript && flags == Injector.INSTALL_SCRIPT_FLAG && scripts.size == 1) {
                return injectSingle(connection, caosFile.name, scripts[0])
            }
            scriptBlocks[JectScriptType.INSTALL] = scripts
        }

        return injectScriptBlocks(project, connection, caosFile.name, totalFiles, scriptBlocks)
    }

    private fun injectSingle(
        connection: CaosConnection,
        fileName: String,
        script: CaosScriptScriptElement,
    ): InjectionStatus {
        val pointer = runReadAction {
            SmartPointerManager.createPointer(script)
        }
        val response = connection.inject(fileName, script.getDescriptor(), formatCaos(script.codeBlock) ?: "")
        return if (response is InjectionStatus.Bad && response.error.contains("{@}")) {
            val positions = runReadAction {
                findPossiblePsiElementOffsets(pointer, response.error)
            }
            response.copy(positions = positions)
        } else {
            response
        }
    }

    private fun injectSingleEventScript(
        connection: CaosConnection,
        fileName: String,
        eventScript: CaosScriptEventScript,
    ): InjectionStatus {
        val pointer = SmartPointerManager.createPointer(eventScript as PsiElement)

        val response = connection.injectEventScript(
            fileName,
            eventScript.family,
            eventScript.genus,
            eventScript.species,
            eventScript.eventNumber,
            formatCaos(eventScript.codeBlock) ?: ""
        )
        return if (response is InjectionStatus.Bad && response.error.contains("{@}")) {
            response.copy(positions = findPossiblePsiElementOffsets(pointer, response.error))
        } else {
            response
        }
    }

    internal fun inject(
        project: Project,
        connection: CaosConnection,
        fileName: String,
        totalFiles: Int,
        scripts: Map<JectScriptType, List<CaosScriptScriptElement>>,
    ): InjectionStatus {
        val scriptBlocks = mutableMapOf<JectScriptType, List<CaosScriptScriptElement>>()
        scripts[JectScriptType.REMOVAL]?.apply {
            scriptBlocks[JectScriptType.REMOVAL] = this
        }
        scripts[JectScriptType.EVENT]?.apply {
            scriptBlocks[JectScriptType.EVENT] = this
        }
        scripts[JectScriptType.INSTALL]?.apply {
            scriptBlocks[JectScriptType.INSTALL] = this
        }
        return injectScriptBlocks(project, connection, fileName, totalFiles, scriptBlocks)
    }

    private fun injectScriptBlocks(
        project: Project,
        connection: CaosConnection,
        fileName: String?,
        totalFiles: Int,
        scriptBlocks: Map<JectScriptType, List<CaosScriptScriptElement>>,
    ): InjectionStatus {
        val title = if (fileName != null) {
            "Injecting $fileName scripts"
        } else {
            "Injecting scripts"
        }
        val serial = randomString(16)
        val pending = InjectionStatus.Pending(fileName, null, serial)
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            title,
            true
        ) {

            override fun run(indicator: ProgressIndicator) {
                val result = try {
                    injectScriptBlocks(
                        project = project,
                        connection = connection,
                        fileName = fileName ?: "Editor",
                        scriptBlocks = scriptBlocks,
                        totalFiles = totalFiles,
                        progressIndicator = indicator
                    )
                } catch (e: ProcessCanceledException) {
                    InjectionStatus.BadConnection(
                        fileName ?: "Editor",
                        null,
                        message("caos.injector.errors.injection-cancelled"),
                        connection.variant,
                    )
                } catch (e: CaosInjectorExceptionWithStatus) {
                    LOGGER.severe("Background inject failed with ${e.className}): ${e.message}")
                    e.printStackTrace()
                    e.injectionStatus ?: InjectionStatus.Bad(
                        fileName ?: "<file>",
                        null,
                        e.message ?: message("caos.injector.errors.unhandled-exception")
                    )
                }
                pending.setResult(result)
            }
        })
        return pending
    }

    private fun injectScriptBlocks(
        project: Project,
        connection: CaosConnection,
        fileName: String,
        scriptBlocks: Map<JectScriptType, List<CaosScriptScriptElement>>,
        totalFiles: Int,
        progressIndicator: ProgressIndicator,
    ): InjectionStatus {
        val responses = mutableListOf<InjectionStatus>()
        var oks = 0
        var error: InjectionStatus? = null

        val scriptTypesInOrder = listOf(
            JectScriptType.REMOVAL,
            JectScriptType.EVENT,
            JectScriptType.INSTALL
        )

        var indexInAllScripts = 0
        val totalScriptsInjecting = scriptBlocks.values.sumOf { it.size }
        scriptTypesInOrder.map async@{ scriptType ->
            progressIndicator.checkCanceled()
            val scripts = runReadAction read@{
                scriptBlocks[scriptType]
                    ?.nullIfEmpty()
                    ?.mapNotNull {
                        if (it.isValid) {
                            SmartPointerManager.createPointer(it)
                        } else {
                            null
                        }
                    }
                    ?: return@read null
            } ?: return@async

            if (scripts.isEmpty()) {
                return@async
            }
            val totalScriptsInList = scripts.size
            injectScriptList(project, connection, scripts) { _, injectionStatus, scriptPointer, indexInList ->
                progressIndicator.checkCanceled()
                val descriptor = scriptPointer.element?.getDescriptor()
                    ?: scriptType.singular
                val description = "${scriptType.singular} $indexInList/$totalScriptsInList from $fileName"
                progressIndicator.text = "Injecting $description"
                val text2Tail = if (totalFiles > 1) {
                    " across $totalFiles files"
                } else {
                    ""
                }
                progressIndicator.text2 = "..script ${indexInAllScripts++}/$totalScriptsInjecting$text2Tail"
                var psiErrorElementOffsets: List<SmartPsiElementPointer<out PsiElement>>? = null

                // Add results to result list
                if (injectionStatus != null) {
                    // Do not add the multi-response, only its children
                    if (injectionStatus is InjectionStatus.MultiResponse) {
                        responses.addAll(injectionStatus.all)
                    } else {
                        responses.add(injectionStatus)
                    }
                }

                when (injectionStatus) {
                    is InjectionStatus.Ok -> {
                        oks++
                    }

                    is InjectionStatus.Bad -> {
                        val message = injectionStatus.error
                        if (message.contains("{@}")) {
                            psiErrorElementOffsets = findPossiblePsiElementOffsets(scriptPointer, message)
                        }
                        error = injectionStatus.copy(
                            positions = psiErrorElementOffsets.nullIfEmpty()
                        )
                    }

                    is InjectionStatus.BadConnection -> error = injectionStatus

                    is InjectionStatus.ActionNotSupported -> error = injectionStatus

                    else -> {}
                }
                if (error != null) {
                    throw CaosInjectorExceptionWithStatus(
                        error,
                        fileName
                    )
                }
            }
        }
        return InjectionStatus.MultiResponse(
            results = responses
        )
    }

    private inline fun injectScriptList(
        project: Project,
        connection: CaosConnection,
        scripts: Collection<SmartPsiElementPointer<CaosScriptScriptElement>>,
        crossinline callback: (Project, InjectionStatus?, script: SmartPsiElementPointer<CaosScriptScriptElement>, index: Int) -> Unit,
    ): Boolean {
        return runReadAction run@{
            for ((index, script) in scripts.withIndex()) {
                val ok = injectScript(
                    project, connection, script, index, callback
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
        scriptPointer: SmartPsiElementPointer<CaosScriptScriptElement>,
        index: Int,
        crossinline callback: (Project, InjectionStatus?, script: SmartPsiElementPointer<CaosScriptScriptElement>, index: Int) -> Unit,
    ): Boolean {
        val script = scriptPointer.element
            ?: return false
        val fileName = script.containingFile?.name ?: script.originalElement?.containingFile?.name ?: ""
        val descriptor = script.getDescriptor()
        val content = formatCaos(script.codeBlock)
            .nullIfEmpty()
            ?: return true
        val result = if (script is CaosScriptEventScript) {
            connection.injectEventScript(
                fileName,
                family = script.family,
                genus = script.genus,
                species = script.species,
                eventNumber = script.eventNumber,
                caos = content
            )
        } else {
            connection.inject(fileName, descriptor, content)
        }
        callback(project, result, scriptPointer, index)
        return result is InjectionStatus.Ok
    }

}


/**
 * Processes CAOS injection response, and notifies the user
 */
internal fun onCaosResponse(
    project: Project,
    response: InjectionStatus?,
) {
    if (response is InjectionStatus.Pending) {
        response.setCallback { status, _ ->
            onCaosResponse(project, status)
        }
        return
    }
    when (response) {
        is InjectionStatus.Ok -> {
            postOk(project, combine(response.toListOf()))
        }
        is InjectionStatus.BadConnection -> {
            postError(
                project,
                message("caos.injector.notification.title.bad-connection"),
                response.formattedError()
            )
        }

        is InjectionStatus.Bad -> {
            postError(
                project,
                message("caos.injector.notification.title.error"), response.error
            )
        }

        is InjectionStatus.ActionNotSupported -> {
            postError(
                project,
                message("caos.injector.notification.title.action-not-supported"),
                response.error
            )
        }

        is InjectionStatus.MultiResponse -> {
            response.all.nullIfEmpty()?.let {
                postOk(project, combine(it))
            }
            response.errors.nullIfEmpty()?.forEach {
                onCaosResponse(project, it)
            }
            response.connectionErrors.nullIfEmpty()?.let {
                postError(project, message("caos.injector.notification.title.bad-connection"), combine(it))
            }
        }

        else -> {
            postError(
                project,
                message("caos.injector.response-title.unhandled"),
                "Unhandled caos result type"
            )
        }
    }
}

private fun combine(resultsIn: List<InjectionStatus>): String {
    val builder: StringBuilder = StringBuilder()
    val results = resultsIn.flatMap { rawResult ->
        when (rawResult) {
            is InjectionStatus.MultiResponse -> rawResult.all
            is InjectionStatus.Pending -> rawResult.resultOrNull()?.let { result ->
                listOf(result)
            } ?: emptyList()

            else -> rawResult.toListOf()
        }
    }

    val nonMultiResults = results.filterNot { it is InjectionStatus.MultiResponse }
    val keys = results.mapNotNull { it.fileName?.lowercase()?.trim() }.distinct()
    for (key in keys) {
        builder.append("\n- ").append(key).append(" -")
        for (result in nonMultiResults.filter { it.fileName?.lowercase()?.trim() == key }) {
            val resultRaw = when (result) {
                is InjectionStatus.Ok -> result.response
                is InjectionStatus.Bad -> result.error
                is InjectionStatus.BadConnection -> result.formattedError()
                is InjectionStatus.ActionNotSupported -> result.error
                is InjectionStatus.MultiResponse -> continue
                is InjectionStatus.Pending -> continue
            }.trim().nullIfEmpty()
            val couldBeEventScriptDescriptor = result.descriptor?.startsWith("scrp ") == true
                    && result.descriptor?.contains(".") == false
            if (resultRaw == null) {
                val word = when (result) {
                    is InjectionStatus.Ok -> "OK"
                    is InjectionStatus.Bad -> "BAD"
                    is InjectionStatus.BadConnection -> "BAD CONNECTION"
                    is InjectionStatus.ActionNotSupported -> "UNSUPPORTED INJECTOR"
                    else -> continue
                }
//                builder.append("> $word")
            } else if (!couldBeEventScriptDescriptor || !resultRaw.equalsIgnoreCase("OK")) {
//                if (resultRaw.trimEnd().contains('\n')) {
//                    builder.append(">>>\n").append(resultRaw).append("\n<<<")
//                } else {
//                    builder.append(resultRaw)
//                }
                builder.append("\n").append(resultRaw)
            }
        }
    }
    return builder.toString().trim()
}


/**
 * Responsible for displaying an OK status response to the user
 */
internal fun postOk(project: Project, responseString: String) {
    val message = if (responseString.isBlank()) {
        ""
    } else {
        "<pre>${responseString.escapeHTML()}</pre>"
    }
    invokeLater {
        CaosInjectorNotifications.show(project, message("caos.injector.notification.title.success"), "\n" + message, NotificationType.INFORMATION)
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
internal fun postError(
    project: Project,
    title: String,
    message: String,
    positions: List<SmartPsiElementPointer<out PsiElement>>? = null,
) {
    invokeLater {

        val tail = if (positions != null && positions.size > 1) {
            message("caos.injection.notification.possible-error-locations")
        } else {
            ""
        }

        var notification = CaosInjectorNotifications.createErrorNotification(project, title, message.escapeHTML() + tail)

        if (positions.isNotNullOrEmpty()) {
            for (position in positions) {
                notification = addPositionLink(notification, position)
            }
        }

        notification.show()
    }
}

private fun addPositionLink(
    notification: CaosNotification,
    position: SmartPsiElementPointer<out PsiElement>,
): CaosNotification {
    val getText: () -> String = {
        val element = position.element
        if (element == null) {
            "<<invalidated>>"
        } else {
            val lineNumber = element.lineNumber?.toString() ?: "?"
            val fileName = element.containingFile?.name ?: element.originalElement?.containingFile?.name
            val line = "Line: $lineNumber"
            if (fileName != null) {
                "$fileName @ $line"
            } else {
                line
            }
        }
    }
    val navigateToElementAction = object : AnAction(getText()) {

        override fun update(e: AnActionEvent) {
            super.update(e)
            val presentation = e.presentation
            val element = position.element
            if (element == null || element.isInvalid) {
                presentation.isEnabled = false
            } else {
                presentation.text = getText()
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            position.element?.let {
                if (it.isValid && it is NavigationItem) {
                    it.navigate(true)
                }
            }
        }
    }
    return notification.addAction(navigateToElementAction)
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
        is CaosScriptEventScript -> "SCRP $family $genus $species $eventNumber"
        is CaosScriptInstallScript -> "Install script"
        is CaosScriptMacro -> "Body script"
        else -> "Code block"
    }
}


internal val c1eElementsRegex =
    "tele|vrsn|xvec|yvec|say#|say\$|aim:|setv\\s+(?:clas|cls2|actv|attr|norn)|\\[[a-zA-Z0-9]*[a-zA-Z_\$\\-+#@!][^]]*]|var[0-9]|obv[0-9]|objp|doif\\s+(targ|norn|objp)\\s+(eq|ne)\\s+0|\\s+(?:bt|bf)|bbd:|dde:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )
internal val c2eElementsRegex =
    "(\"(?:[^\"]|\\.)*\"|'(?:[^\'\\\\]|\\\\.)'|\\d+\\.\\d+|\\s\\.\\d+|mv[0-9]{2}|(?:do|el)if.+?(?:<>|<|>|<=|>=|=)|(?:eq|ne|<>|=)\\s+null|\\s+(?:seta|sets|adds))|net:|prt:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

/**
 * Format the contents of a code block element
 */
private fun formatCaos(codeBlock: CaosScriptCodeBlock?): String? {
    if (codeBlock == null) {
        return null
    }
    return runReadAction {
        val blockText = codeBlock.text
        if (blockText != null) {
            formatCaos(codeBlock.variant, blockText)
        } else {
            null
        }
    }
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

/**
 * COPY caos to a given interfaces Bootstrap directory
 */
internal fun copyForJect(variant: CaosVariant?, gameInterfaceName: GameInterfaceName, caos: String): File? {

    if (variant?.isNotOld != true) {
        return null
    }

    if (gameInterfaceName is IsNet) {
        return null
    }

    val tempFile = try {
        getJectTempFile(gameInterfaceName)
    } catch (_: Exception) {
        null
    } ?: return null

    return try {
        tempFile.writeText(caos)
        tempFile
    } catch (_: Exception) {
        try {
            tempFile.delete()
        } catch (_: Exception) {
        }
        null
    }
}


private fun getJectTempFile(gameInterfaceName: GameInterfaceName): File? {
    val bootstrap = getBootstrapDirectory(gameInterfaceName)
        ?: return null
    val tempDirectory = File(bootstrap, "temp")
    if (!tempDirectory.exists()) {
        tempDirectory.mkdir()
    }
    var kicks = 0
    while (kicks++ < 30) {
        val temp = File(tempDirectory, randomString(4) + "cosx")
        if (!temp.exists()) {
            temp.createNewFile()
            return temp
        }
    }
    return null
}

private fun getBootstrapDirectory(gameInterfaceName: GameInterfaceName): File? {
    val gameDirectory = gameInterfaceName.path
        ?: return null
    var root = File(gameDirectory)
    if (!root.exists()) {
        if (gameInterfaceName is WineInjectorInterface) {
            root = getWineFallbackPath(root, gameInterfaceName, gameDirectory)
                ?: return null
        } else {
            return null
        }
    }

    val bootstrap = if (root.name like "Bootstrap") {
        root
    } else {
        File(root, "Bootstrap")
    }
    if (!bootstrap.exists()) {
        return null
    }
    return bootstrap
}

private fun getWineFallbackPath(root: File, gameInterfaceName: WineInjectorInterface, gameDirectory: String): File? {
    try {
        val prefix = File(gameInterfaceName.prefix)
        if (!prefix.exists()) {
            return null
        }
        val parts = "([a-zA-Z]):[\\\\/](.+)".toRegex()
            .matchEntire(gameDirectory)
            ?.groupValues
            ?.drop(1)
            ?: return null
        val drive = File(root, "drive_${parts[0].lowercase()}")
        if (!drive.exists()) {
            return null
        }
        val temp = File(drive, parts[1])
        if (!temp.exists()) {
            return null
        }
        return temp
    } catch (_: Exception) {
        return null
    }
}

private fun findPossiblePsiElementOffsets(
    scriptPointer: SmartPsiElementPointer<out PsiElement>,
    error: String,
): List<SmartPsiElementPointer<out PsiElement>> {

    val script = scriptPointer.element as? CaosScriptScriptElement
        ?: return emptyList()

    val text = script.text
        .nullIfEmpty()
        ?: return listOf(scriptPointer)

    val pattern = ".*(\\.\\.\\.|\\[)(.*)\\{@\\}(.*)(\\.\\.\\.|\\]).*"
        .toRegex(RegexOption.MULTILINE)

    val matchParts = pattern
        .matchEntire(error.replace(WHITESPACE, " "))
        ?.groupValues
        ?.drop(1)
        ?.map { it.trim().replace(WHITESPACE, "\\\\s+") }
        ?: return listOf(scriptPointer)
    val prefix = if (matchParts[0].first() == '[') "^" else ".*?"
    val suffix = if (matchParts[3].last() == ']') "$" else ".*?"
    var regex = ("$prefix(${Regex.escape(matchParts[1])})\\s+(${Regex.escape(matchParts[2])})$suffix")
        .toRegex()
    var out = findPossiblePsiElementOffsets(script, text, regex)

    if (out.isEmpty()) {
        regex = ("$prefix(${matchParts[1]})\\s*(${matchParts[2]})$suffix")
            .toRegex()
        out = findPossiblePsiElementOffsets(script, text, regex)
    }
    return out
}

private fun findPossiblePsiElementOffsets(
    script: CaosScriptCompositeElement,
    text: String,
    regex: Regex,
): List<SmartPsiElementPointer<CaosScriptCompositeElement>> {
    val out = mutableListOf<SmartPsiElementPointer<CaosScriptCompositeElement>>()
    var match = regex.find(text)
    while (match != null) {
        val absoluteStart = match.range.first
        val errorStart = absoluteStart + (match.groupValues.getOrNull(1)?.length ?: 0)
        try {
            val rawElement = script.findElementAt(errorStart)

            val element = rawElement as? CaosScriptCompositeElement
                ?: rawElement?.parent as? CaosScriptCompositeElement
                ?: rawElement?.parent?.parent as? CaosScriptCompositeElement
            if (element != null) {
                val pointer = SmartPointerManager.createPointer(element)
                out.add(pointer)
            }
        } catch (e: Exception) {
            LOGGER.severe("Failed to get element at index: $errorStart in <$text>; ${e.className}: ${e.message ?: ""}")
        }
        match = regex.find(text, match.range.last)
    }
    return out
}