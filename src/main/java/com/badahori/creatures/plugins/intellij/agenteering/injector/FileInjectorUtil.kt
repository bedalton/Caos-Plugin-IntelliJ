package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.bedalton.common.util.className
import com.bedalton.common.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.JectScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.ScriptBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.CaosScriptsQuickCollapseToLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getScripts
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.navigation.NavigationItem
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.PrivilegedActionException

internal object FileInjectorUtil {

    private var collapse: Boolean? = null

    internal fun inject(
        project: Project,
        connection: CaosConnection,
        caosFile: CaosScriptFile,
        totalFiles: Int,
        flags: Int,
        useJect: Boolean,
    ): InjectionStatus {
        if (useJect && connection.supportsJect) {
            return connection.injectWithJect(project, caosFile, flags)
        }

        val variant = caosFile.variant

        // Get scripts as structs to combine read actions
        val fileScripts = runReadAction {
            ScriptBundle(variant, caosFile.getScripts())
        }

        val scriptBlocks = mutableMapOf<JectScriptType, List<CaosScriptStruct>>()
        if (flags hasFlag Injector.REMOVAL_SCRIPT_FLAG) {
            scriptBlocks[JectScriptType.REMOVAL] = fileScripts.removalScripts
        }
        if (flags hasFlag Injector.EVENT_SCRIPT_FLAG) {
            scriptBlocks[JectScriptType.EVENT] = fileScripts.eventScripts
        }

        if (flags hasFlag Injector.INSTALL_SCRIPT_FLAG) {
            scriptBlocks[JectScriptType.INSTALL] = fileScripts.installScripts
        }

        return injectScripts(project, variant, connection, caosFile.name, totalFiles, scriptBlocks)
    }

    fun injectScripts(
        project: Project,
        variant: CaosVariant?,
        connection: CaosConnection,
        fileName: String?,
        totalFiles: Int,
        scriptBlocks: Map<JectScriptType, List<CaosScriptStruct>>,
        collapse: Boolean = this.collapse == true
    ): InjectionStatus {
        val title = if (fileName != null) {
            "Injecting $fileName scripts"
        } else {
            "Injecting scripts"
        }
        val allScripts = scriptBlocks.values.flatten()
        if (allScripts.any { !it.collapsedLengthIsValid }) {
            val tooLong = allScripts.filter { !it.collapsedLengthIsValid }
            CaosInjectorNotifications.showError(
                project,
                "Injection Error",
                "Some CAOS scripts are too long to be injected through injector"
            )
            return InjectionStatus.MultiResponse(
                tooLong.map {
                    LOGGER.info("Script is too long: ${it.fileName}")
                    InjectionStatus.BadConnection(
                        it.fileName ?: "",
                        it.descriptor,
                        "CAOS script is too long to be injected through injector",
                        variant ?: CaosVariant.UNKNOWN
                    )
                }
            )
        }

        val serial = randomString(16)
        val pending = InjectionStatus.Pending(fileName, null, serial)
        if (!collapse && allScripts.any { it.collapsed }) {
            CaosInjectorNotifications
                .createErrorNotification(
                    project,
                    "Script Too Long",
                    "Some scripts are too long for regular injection",
                ).addAction(object : AnAction(
                    "Collapse Whitespaces and Inject"
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        this@FileInjectorUtil.collapse = true
                        val result = injectScripts(
                            project = project,
                            variant = variant,
                            connection = connection,
                            fileName = fileName,
                            scriptBlocks = scriptBlocks,
                            totalFiles = totalFiles,
                            collapse = true
                        )
                        pending.setResult(result)
                    }
                })
                .show()
            return pending
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            title,
            true
        ) {

            override fun run(indicator: ProgressIndicator) {
                val result = try {
                    runBlocking {
                        injectScriptBlocks(
                            project = project,
                            variant = variant,
                            connection = connection,
                            fileName = fileName ?: "Editor",
                            scriptBlocks = scriptBlocks,
                            totalFiles = totalFiles,
                            collapse = collapse,
                            progressIndicator = indicator
                        )
                    }
                } catch (e: ProcessCanceledException) {
                    InjectionStatus.BadConnection(
                        fileName ?: "Editor",
                        null,
                        message("caos.injector.errors.injection-cancelled"),
                        connection.variant,
                    )
                } catch (wrapped: Exception) {
                    val e = if (wrapped is PrivilegedActionException) {
                        wrapped.cause
                    } else {
                        wrapped
                    }
                    if (e is CaosInjectorExceptionWithStatus) {
                        LOGGER.severe("Background inject failed with ${e.className}): ${e.message}")
                        e.printStackTrace()
                        e.injectionStatus ?: InjectionStatus.Bad(
                            fileName ?: "<file>",
                            null,
                            e.message ?: message("caos.injector.errors.unhandled-exception")
                        )
                    } else {
                        throw wrapped
                    }
                }
                pending.setResult(result)
            }
        })
        return pending
    }

    private fun injectScriptBlocks(
        project: Project,
        @Suppress("UNUSED_PARAMETER") variant: CaosVariant?,
        connection: CaosConnection,
        fileName: String,
        scriptBlocks: Map<JectScriptType, List<CaosScriptStruct>>,
        totalFiles: Int,
        @Suppress("UNUSED_PARAMETER") collapse: Boolean,
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
            val scripts = scriptBlocks[scriptType]
                ?.nullIfEmpty()
                ?: return@async

            val totalScriptsInList = scripts.size
            injectScriptList(project, connection, scripts) run@{ _, injectionStatus, scriptPointer, indexInList ->
                progressIndicator.checkCanceled()
                val descriptor = scriptPointer.descriptor
                    ?: scriptType.singular
                val description = "$descriptor $indexInList/$totalScriptsInList from $fileName"
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
                            psiErrorElementOffsets = findPossiblePsiElementOffsets(project, scriptPointer, message)
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
        scripts: Collection<CaosScriptStruct>,
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptStruct, index: Int) -> Unit,
    ): Boolean {
        for ((index, script) in scripts.withIndex()) {
            val ok = injectScript(
                project, connection, script, index, callback
            )
            if (!ok) {
                return false
            }
        }
        return true
    }

    private inline fun injectScript(
        project: Project,
        connection: CaosConnection,
        script: CaosScriptStruct,
        index: Int,
        crossinline callback: (Project, InjectionStatus?, script: CaosScriptStruct, index: Int) -> Unit,
    ): Boolean {
        val fileName = script.fileName
        val descriptor = script.descriptor
        val content = script.text
        val result = if (script is CaosScriptStruct.EventScript) {
            connection.injectEventScript(
                project,
                fileName ?: descriptor ?: "Event Script",
                family = script.family,
                genus = script.genus,
                species = script.species,
                eventNumber = script.eventNumber,
                caos = content
            )
        } else {
            connection.inject(project, fileName ?: descriptor ?: "Script", descriptor, content)
        }
        callback(project, result, script, index)
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
    val results: List<InjectionStatus> = resultsIn.flatMap { rawResult ->
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
//                val word = when (result) {
//                    is InjectionStatus.Ok -> "OK"
//                    is InjectionStatus.Bad -> "BAD"
//                    is InjectionStatus.BadConnection -> "BAD CONNECTION"
//                    is InjectionStatus.ActionNotSupported -> "UNSUPPORTED INJECTOR"
//                    else -> continue
//                }
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
        CaosInjectorNotifications.show(
            project,
            message("caos.injector.notification.title.success"),
            "\n" + message,
            NotificationType.INFORMATION
        )
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

        var notification =
            CaosInjectorNotifications.createErrorNotification(project, title, message.escapeHTML() + tail)

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


internal val c1eElementsRegex =
    "tele|vrsn|xvec|yvec|say#|say\$|aim:|setv\\s+(?:clas|cls2|actv|attr|norn)|\\[[a-zA-Z0-9]*[a-zA-Z_\$\\-+#@!][^]]*]|var[0-9]|obv[0-9]|objp|doif\\s+(targ|norn|objp)\\s+(eq|ne)\\s+0|\\s+(?:bt|bf)|bbd:|dde:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )
internal val c2eElementsRegex =
    "(\"(?:[^\"]|\\.)*\"|'(?:[^\'\\\\]|\\\\.)'|\\d+\\.\\d+|\\s\\.\\d+|mv[0-9]{2}|(?:do|el)if.+?(?:<>|<|>|<=|>=|=)|(?:eq|ne|<>|=)\\s+null|\\s+(?:seta|sets|adds))|net:|prt:".toRegex(
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

/**
 * Format CAOS for injection
 */
internal fun formatCaos(variant: CaosVariant?, codeBlock: String?, collapse: Boolean): String {
    if (codeBlock.isNullOrBlank()) {
        return ""
    }
    // Collapse if needed
    return if (collapse) {
        // if C1e, flatten and remove comments
        CaosScriptsQuickCollapseToLine.collapse(variant?.isOld == true, codeBlock.trim())
    } else {
        // C2e, return just how it is
        codeBlock.trim()
    }
}

/**
 * Determine if C1e using variant if not null, and by checking code contents as fallback
 */
@Suppress("unused")
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
@Suppress("unused")
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
    project: Project,
    scriptPointer: CaosScriptStruct,
    error: String,
): List<SmartPsiElementPointer<out PsiElement>> = runReadAction run@{


    // Get VirtualFile for script stub
    val file = scriptPointer.path?.let { path ->
        val file = File(path)
        VfsUtil.findFileByIoFile(file, true)
    } ?: return@run emptyList()

    val psiFile = file.getPsiFile(project)
        ?: return@run emptyList()

    val withoutSpaces = scriptPointer.originalText.replace(" ", "")
    val allScripts = (if (scriptPointer is CaosScriptStruct.EventScript) {
        val eventScripts = psiFile.collectElementsOfType(CaosScriptEventScript::class.java)
            .filter {
                it.family == scriptPointer.family &&
                        it.genus == scriptPointer.genus &&
                        it.species == scriptPointer.species
            }
        if (eventScripts.size > 1) {
            eventScripts.filter { it.text.replace(" ", "") == withoutSpaces }
        } else {
            eventScripts
        }
    } else {
        psiFile.collectElementsOfType(CaosScriptScriptElement::class.java)
            .filter { it !is CaosScriptEventScript }
            .filter { it.text.replace(" ", "") == withoutSpaces }
    }).filter {
        val range = it.textRange
        range == scriptPointer.range ||
                range.contains(scriptPointer.range.startOffset) ||
                range.contains(scriptPointer.range.endOffset) ||
                scriptPointer.range.contains(range.startOffset) ||
                scriptPointer.range.contains(range.endOffset)
    }

    @Suppress("RegExpRedundantEscape")
    val pattern = ".*(\\.\\.\\.|\\[)(.*)\\{@\\}(.*)(\\.\\.\\.|\\]).*"
        .toRegex(RegexOption.MULTILINE)

    val matchParts = pattern
        .matchEntire(error.replace(WHITESPACE, " "))
        ?.groupValues
        ?.drop(1)
        ?.map { it.trim().replace(WHITESPACE, "\\\\s+") }
        ?: return@run allScripts.map { SmartPointerManager.createPointer(it) }

    val prefix = if (matchParts[0].first() == '[') "^" else ".*?"
    val suffix = if (matchParts[3].last() == ']') "$" else ".*?"
    var regex = ("$prefix(${Regex.escape(matchParts[1])})\\s+(${Regex.escape(matchParts[2])})$suffix")
        .toRegex()
    var out = allScripts.flatMap { script ->
        findPossiblePsiElementOffsets(script, script.text, regex)
    }

    if (out.isEmpty()) {
        regex = ("$prefix(${Regex.escape(matchParts[1])})\\s*(${Regex.escape(matchParts[2])})$suffix")
            .toRegex()
        out = allScripts.flatMap { script ->
            findPossiblePsiElementOffsets(script, script.text, regex)
        }
    }
    return@run out
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