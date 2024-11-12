package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.bedalton.common.structs.Pointer
import com.bedalton.common.util.ensureEndsWith
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptAtDirectiveComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2BlockComment
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.collectChildren
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import java.io.File
import java.util.*
import java.util.Timer
import javax.swing.*

internal class LinkFilesInsertHandler(
    private val action: String,
    private val isCaos2: Boolean = true,
) : LocalQuickFix, InsertHandler<LookupElement>, IntentionAction {

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        return "$action files..."
    }


    override fun getFamilyName(): String {
        return "CAOS2Pray"
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return isAvailable(file)
    }

    fun isAvailable(file: PsiFile?): Boolean {
        if (file == null || file !is CaosScriptFile) {
            return false
        }
        val virtualFile = (file.virtualFile ?: file.originalFile.virtualFile)
            ?: return false

        val parent = getParent(file.project, virtualFile)
            ?: return false

        val caos2Preflight = PsiTreeUtil.collectElementsOfType(file, CaosScriptCaos2Block::class.java).isNotEmpty() &&
                file.variant?.isNotOld.orTrue()

        if (!caos2Preflight) {
            return false
        }
        return getLinksInDirectory(parent, "", virtualFile, action.lowercase() == "link").isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        applyFix(project, file)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile
        applyFix(project, file)
    }

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val file = context.file
        val containingComment = file.findElementAt(context.selectionEndOffset)
            ?.getSelfOrParentOfType(CaosScriptCaos2BlockComment::class.java)
            ?: file.findElementAt(context.startOffset)
                ?.getSelfOrParentOfType(CaosScriptCaos2BlockComment::class.java)
        val pointer = if (containingComment != null) SmartPointerManager.createPointer(containingComment) else null
        applyFix(context.project, file)
        runWriteAction {
            pointer?.element?.delete()
        }
    }

    private fun applyFix(project: Project, file: PsiFile) {

        val virtualFile = (file.virtualFile ?: file.originalFile.virtualFile)
            ?: return

        val parent = getParent(file.project, virtualFile)
            ?: return

        val linksRaw = getLinksInDirectory(parent, "", file.virtualFile, action.lowercase() == "link")
        val allExtensions = linksRaw.map { it.category + " in " + it.parentPath }.distinct().sorted().toSet()
        val selected = Pointer<Pair<List<LinkedFile>, Boolean>?>(null)
        invokeLater {
            showPanel(file.fileType == PrayFileType, project, allExtensions, linksRaw, selected)

            val selectedFiles = selected.value
                ?: return@invokeLater

            if (isCaos2) {
                linkCaos2(project, file, selectedFiles.first)
                file.document?.let { document ->
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                }
            } else {
                //TODO: Add PRAY linker
//                linkPray(project, file, selectedFiles)
            }
        }
    }

    private fun showPanel(
        isPray: Boolean,
        project: Project,
        allExtensions: Set<String>,
        files: List<LinkedFile>,
        out: Pointer<Pair<List<LinkedFile>, Boolean>?>,
    ) {

        // Initialize JPanel
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Select file types to add"))

        // Create Component lists
        val checkboxes = mutableListOf<JCheckBox>()
        val endOfListComponents = mutableListOf<JComponent>()

        // Add a checkbox to add all
        val all = JCheckBox("Add All")
        all.addActionListener {
            val enableOthers = !all.isSelected
            checkboxes.forEach {
                it.isEnabled = enableOthers
            }
        }
        panel.add(all)


        var lastTag = ""

        // Create Checkboxes
        for (expression in allExtensions) {
            lastTag = createCheckbox(panel, checkboxes, endOfListComponents, lastTag, expression)
        }

        // Add components meant for the end of the list
        if (endOfListComponents.isNotEmpty()) {
            panel.add(JLabel("Other Files"))
            endOfListComponents.addAll(endOfListComponents)
        }

        // Add regex filter
        panel.add(JLabel("Filter file names with regex"))
        val regexField = JTextField().apply {
            isEditable = true
        }


        // Report the number of downloads that will be added vs those possible
        val filtered = JLabel()
        panel.add(filtered)
        val reportFiltered = filter@{
            val selected = filterWithCheckboxData(all.isSelected, files, getCheckedCheckboxFilterData(checkboxes))
            val afterRegexCount = filterWithRegex(selected, regexField.text, filtered)?.size
                ?: return@filter
            filtered.text = "Result: $afterRegexCount / ${selected.size}"
            if (afterRegexCount == 0) {
                filtered.foreground = JBColor.red
            } else {
                filtered.foreground = JBColor.foreground()
            }
        }

        // Add change listener on checkboxes to update file count
        checkboxes.forEach {
            it.addActionListener {
                reportFiltered()
            }
        }

        // Update filtered as regex changes
        var timer: Timer? = null
        regexField.document.addDocumentListener(DocumentChangeListener { _, _ ->
            timer?.cancel()
            timer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        reportFiltered()
                    }
                }, 200)
            }
        })
        panel.add(regexField)


        val inlineFiles = JBCheckBox("Inline files")
        if (isPray) {
            panel.add(inlineFiles)
        }

        // Show modal and apply on okay
        DialogBuilder(project)
            .centerPanel(panel).apply {
                okActionEnabled(true)
                setOkOperation {
                    val selected =
                        filterWithCheckboxData(all.isSelected, files, getCheckedCheckboxFilterData(checkboxes))
                    val afterRegexCount = filterWithRegex(selected, regexField.text, filtered)
                        ?: return@setOkOperation
                    out.value = Pair(afterRegexCount, isPray && inlineFiles.isSelected)
                    this.dialogWrapper.close(0)
                }
            }.showModal(true)
    }

    private fun linkCaos2(project: Project, file: PsiFile, files: List<LinkedFile>) {
        if (files.isEmpty()) {
            CaosNotifications.showWarning(project, "$action Failed", "No file types selected for add")
            return
        }
        val runnable = Runnable run@{
            runWriteAction {
                if (project.isDisposed) {
                    return@runWriteAction
                }
                val document = file.document.apply {
                    if (this == null) {
                        invokeLater {
                            CaosNotifications.showError(
                                project,
                                "$action Failed",
                                "Failed to $action files. Could not access document in tree"
                            )
                        }
                        return@runWriteAction
                    }
                }
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document!!)

                val editor = file.editor
                val offset = editor?.caretModel?.currentCaret?.offset
                val elementAtCursor = if (offset != null) {
                    file.findElementAt(offset)
                        ?.getSelfOrParentOfType(CaosScriptCaos2BlockComment::class.java)
                } else {
                    null
                }
                val insertPoint = elementAtCursor
                    ?.let {
                        val start = it.startOffset
                        val nextPointer = (it.previous as? PsiWhiteSpace)
                            ?.let { ws ->
                                SmartPointerManager.createPointer(ws)
                            }
                        it.delete()
                        nextPointer?.element?.delete()
                        start
                    }
                    ?: file.getChildOfType(CaosScriptCaos2Block::class.java)?.endOffset
                    ?: file.getChildOfType(CaosScriptAtDirectiveComment::class.java)?.endOffset
                    ?: 0
                val text = buildCaos2Text(file, action, files).trimEnd().nullIfEmpty()
                    ?: return@runWriteAction

                if (editor != null) {
                    EditorUtil.insertText(editor, text + "\n", insertPoint, true)
                } else {
                    EditorUtil.insertText(document, text + "\n", insertPoint)
                }
            }
        }
        CommandProcessor
            .getInstance()
            .executeCommand(
                project,
                runnable,
                "${action.matchCase(Case.CAPITAL_FIRST)} ${files.size} files",
                "CAOS:$action:$now"
            )
    }


    private fun buildCaos2Text(file: PsiFile, action: String, linksUnfiltered: List<LinkedFile>): String {
        val sorted = mutableMapOf<String, MutableList<String>>()
        val linksFiltered = getPathsToAdd(file, action, linksUnfiltered)
        for (link in linksFiltered) {
            @Suppress("SpellCheckingInspection")
            val key = if (link.category == "CAOS" || link.category == "COS") {
                "CAOS"
            } else if (BreedPartKey.isPartName(link.virtualFile.nameWithoutExtension)) {
                "BREED"
            } else if (link.category.startsWith(":")) {
                "zzzzz"
            } else {
                link.category
            }
            if (!sorted.containsKey(key)) {
                sorted[key] = mutableListOf()
            }
            val linkPath = if (link.path[0] == '/') link.path.substring(1) else link.path
            sorted[key]!!.add(linkPath)
        }
        val out = StringBuilder()
        for (key in sorted.keys.sorted()) {
            val linksRaw = sorted[key]!!
            val prefix = "\n*# $action \""
            for (link in linksRaw.sortedBy { it.lowercase() }) {
                out.append(prefix).append(link).append('"')
            }
        }
        return out.toString()
    }

    /**
     * Filters list of linked files by regex
     */
    private fun filterWithRegex(selected: List<LinkedFile>, regexRaw: String, filtered: JLabel): List<LinkedFile>? {
        return if (regexRaw.isBlank()) {
            selected
        } else {

            val pathRegex = try {
                "(.*?/)?$regexRaw".toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                null
            }

            val regex = try {
                regexRaw.toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                filtered.text = "Regex error"
                filtered.foreground = JBColor.red
                null
            }

            selected.filter {
                try {
                    regex?.matches(it.virtualFile.name) == true ||
                            pathRegex?.matches(it.virtualFile.path) == true
                } catch (e: Exception) {
                    e.rethrowAnyCancellationException()
                    filtered.text = "Regex error"
                    filtered.foreground = JBColor.red
                    false
                }
            }
        }
    }

    /**
     * Returns the filter data for the selected checkboxes
     */
    private fun getCheckedCheckboxFilterData(checkboxes: List<JCheckBox>): List<Pair<String, String>> {
        return checkboxes.filter {
            it.isSelected
        }.map { checkbox ->
            val expression = checkbox.text
            getCheckboxFilterData(expression)
        }
    }

    /**
     * Transforms a checkboxes text expression into a data set to use for filtering
     * Pair::first = extension
     * Pair::Second = directory of files to add
     */
    private fun getCheckboxFilterData(expression: String): Pair<String, String> {
        val components = expression.split("*.")
        val extension = components.last().trim().lowercase()
        val parentDirectory = components.first().trim().let { directory ->
            if (directory[0] == '.') {
                directory.substring(1)
            } else {
                directory
            }
        }
        return Pair(extension, parentDirectory.ensureEndsWith('/'))
    }

    /**
     * Creates a checkbox adding it to a panel and to a list of checkboxes
     * Returns the tag of this item
     */
    private fun createCheckbox(
        panel: JPanel,
        checkboxes: MutableList<JCheckBox>,
        delayedCheckboxes: MutableList<JComponent>,
        lastTag: String,
        expression: String,
    ): String {
        val components = expression.split("in")
        val extensionRaw = components
            .first()
            .trim()
            .nullIfEmpty()
            ?: return lastTag
        val isOther = extensionRaw[0] == ':'
        val extension = if (isOther) extensionRaw.substring(1) else extensionRaw
        val tag = if (isOther) "Other" else extension.uppercase()
        val parentDirectory = components[1].trim()
        if (tag != lastTag) {
            if (!isOther) {
                panel.add(JLabel("$tag Files"))
            }
        }
        val checkbox = JCheckBox("\t.$parentDirectory/*.${extension.lowercase()}").apply {
            this.toolTipText = "$action all $extension files"
        }
        if (isOther) {
            delayedCheckboxes.add(checkbox)
        } else {
            panel.add(checkbox)
        }
        checkboxes.add(checkbox)
        return tag
    }

    /**
     * Filters the list of linked files by those matching the checked checkboxes criteria
     */
    private fun filterWithCheckboxData(
        allSelected: Boolean,
        files: List<LinkedFile>,
        paths: List<Pair<String, String>>,
    ): List<LinkedFile> {
        return if (allSelected) {
            files
        } else {
            files.filter { file ->
                paths.any { path ->
                    file.category.let { if (it[0] == ':') it.substring(1) else it }
                        .lowercase() == path.first && file.parentPath.ensureEndsWith('/') like path.second
                }
            }
        }
    }

    /**
     * Filter list of linked files to get those files not already linked
     */
    private fun getPathsToAdd(file: PsiFile, action: String, files: List<LinkedFile>): List<LinkedFile> {

        // Get this PSI files virtual file
        val virtualFile = (file.virtualFile ?: file.containingFile?.virtualFile)
            ?.parent
            ?: return emptyList()

        // Pair linked file with its relative path to root
        val relativePaths = files.map { link ->
            Pair(link, VfsUtil.findRelativePath(virtualFile, link.virtualFile, '/')!!.lowercase())
        }

        // Get already included or linked items with action
        val block = file.getChildOfType(CaosScriptCaos2Block::class.java)
            ?: return files
        val existing = getExisting(block, action)

        // Get Linked files not already included
        return relativePaths
            .distinctBy { it.second }
            .filter {
                it.second !in existing
            }
            .map(Pair<LinkedFile, String>::first)
    }


    /**
     * Get paths that already exist for this action in this file
     */
    private fun getExisting(block: CaosScriptCaos2Block, action: String): Set<String> {
        val actionLower = action.lowercase()
        val get: (data: Pair<String, List<String>>) -> List<String> = when (actionLower) {
            "inline" -> { data: Pair<String, List<String>> -> listOfNotNull(data.second.lastOrNull()?.lowercase()) }
            else -> { data: Pair<String, List<String>> -> data.second.map(String::lowercase) }
        }
        val commands = block
            .commands
            .filter { (command, _) ->
                command.lowercase() == actionLower
            }
        return commands.flatMap { get(it) }.toSet()
    }

    fun getParent(project: Project, virtualFile: VirtualFile): VirtualFile? {

        val projectPath = project.basePath?.let {
            val projectFile = File(it)
            if (projectFile.exists() && virtualFile.path.startsWith(it)) {
                VfsUtil.findFileByIoFile(projectFile, false)
            } else {
                null
            }
        }

        return projectPath
            ?: virtualFile.parent
    }

}

internal data class LinkedFile(
    val virtualFile: VirtualFile,
    val path: String,
    val parentPath: String,
    val category: String,
)

@Suppress("SameParameterValue")
private fun getLinksInDirectory(
    directory: VirtualFile,
    path: String, // TODO figure out what this variable was for if value is always <"">
    thisFile: VirtualFile,
    caos: Boolean,
): List<LinkedFile> {
    val out = mutableListOf<LinkedFile>()
    for (child in directory.collectChildren(::skip)) {
        if (child.path == thisFile.path) {
            continue
        }

        val extension = child.extension
            ?.lowercase()
            ?: continue

        if (caos && extension != "cos" && extension != "caos") {
            continue
        } else if (!caos && (extension == "cos" || extension == "caos")) {
            continue
        }

        val category = if (extension in creaturesFileExtensions) {
            extension
        } else {
            ":${extension}"
        }

        val link = LinkedFile(
            virtualFile = child,
            path = VfsUtil.findRelativePath(thisFile, child, '/') ?: child.name,
            parentPath = path,
            category = category
        )
        out.add(link)
    }
    return out
}

internal val attachableFileExtensions = listOf(
    "att",
    "s16",
    "c16",
    "blk",
    "catalogue",
    "gen",
    "gno",
    "wav",
    "ming",
    "mng",
    "pray",
    "creature",
    "journal"
)
internal val creaturesFileExtensions = listOf(
    "cos",
    "caos",
    *attachableFileExtensions.toTypedArray()
)

@Suppress("unused")
internal val caos2PrayFileExtensions = listOf(
    "att",
    "s16",
    "c16",
    "blk",
    "catalogue",
    "gen",
    "gno",
    "wav",
    "ming",
    "mng",
    "journal"
)

private val skipExtensions = listOf(
    "iml",
    "xml"
)

private fun skip(file: VirtualFile): Boolean {
    if (file.name[0] == '.') {
        return false
    }
    if (file.path.contains("/.")) {
        return false
    }
    return file.extension?.lowercase() !in skipExtensions
}