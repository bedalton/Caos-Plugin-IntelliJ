@file:Suppress("MemberVisibilityCanBePrivate")

package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorImpl.BreedSelectionChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttAutoFill.paddedData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileLine
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileParser.parse
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedIfNotCached
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ExplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ImplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey.Companion.fromFileName
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.bedalton.creatures.common.structs.BreedKey
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.psi.*
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import kotlin.math.abs

/**
 * Model to handle changes to the ATT file
 */
internal class AttEditorModel(
    project: Project,
    val disposable: Disposable,
    val attFile: VirtualFile,
    val spriteFile: VirtualFile,
    variant: CaosVariant,
    val showNotification: (message: String, messageType: MessageType) -> Unit,
    private var partBreedsProvider: PartBreedsProvider?,
    private var attChangeListener: AttChangeListener?,
    private var selectedCellHolder: HasSelectedCell?,
) : OnChangePoint, HasSelectedCell, Disposable, VirtualFileListener, DocumentListener, BreedSelectionChangeListener {

    private var mImages: List<BufferedImage>? = null
    private var mReplications: Map<Int, List<Int>>? = null
    private var mVariant: CaosVariant = variant
    internal val variant: CaosVariant get() = mVariant
    private var changedSelf: Boolean = false
    private var disposed: AtomicBoolean = AtomicBoolean(false)
    private val mNotReplicatedAtts = mutableListOf<Int>()
    private var mCurrentReplication: List<Int>? = null

    private var project: Project? = project

    val notReplicatedAtts: List<Int> get() = mNotReplicatedAtts

    var mPart: Char = attFile.name[0].lowercase()
    val part: Char get() = mPart

    private var mLinesCount: Int = 0
    val linesCount: Int get() = mLinesCount

    private var mPointsCount: Int = 0
    val pointsCount: Int get() = mPointsCount
    private var mPointNames: List<String> = pointNames(part)
    val pointNames: List<String> get() = mPointNames
    private var mCurrentPoint: Int = 0
    val currentPoint: Int get() = mCurrentPoint

    var mSelectedCell: Int = 0
    override val selectedCell: Int get() = mSelectedCell

    val rootPath: VirtualFile get() = attFile.parent

    private var mAttData: AttFileData? = null

    val fileName: String get() = attFile.name

    var mShiftAttachmentPointInRelatedAtt: Boolean = false

    val shiftAttachmentPointInRelatedAtt: Boolean get() = mShiftAttachmentPointInRelatedAtt

    private var relatedAtt: RelativeAtt? = null

    var relativeFileFailed = false

    @Suppress("unused")
    var relativePoint: Int? = null

    var mRelativePart: Char? = null

    val relativePart: Char? get() = mRelativePart

    private var shiftPointInRelatedAttAllowed: Boolean = false

    val attData: AttFileData
        get() {
            mAttData?.let {
                return it
            }
            val contents = attFile.contents
            val data = paddedData(contents, spriteFile, variant)
                ?.let { (trueVariant, data) ->
                    mVariant = trueVariant
                    data
                }
                ?: parse(contents, fileName = attFile.name)
            mAttData = data
            return data
        }

    private var document: Document? = null

    val attLines: List<AttFileLine> get() = attData.lines

    // Lock X axis for point
    private var mLockX: Boolean = false
    var lockX: Boolean
        get() = mLockX
        set(value) {
            mLockY = false; mLockX = value
        }

    // Lock Y axis for point
    private var mLockY: Boolean = false
    var lockY: Boolean
        get() = mLockY
        set(value) {
            mLockX = false; mLockY = value
        }

    var replicateAttsToDuplicateSprites: Boolean? = CaosApplicationSettingsService
        .getInstance()
        .replicateAttsToDuplicateSprites

    val actionId = AtomicInteger(0)

    val readonly = attFile is CaosVirtualFile

    private var mFoldedLines: List<Int>? = null

    @Suppress("unused")
    private var lockRelative = false


    init {
        if (!project.isDisposed) {
            // Register disposer to prevent memory leaks
            Disposer.register(disposable, this)
        }

        // Add document listeners
        val psiFile = attFile.getPsiFile(project)
        initDocumentListeners(project, psiFile)

        // Add application level settings listener
        CaosApplicationSettingsService.addSettingsChangedListener(this) { _, it ->
            replicateAttsToDuplicateSprites = it.replicateAttsToDuplicateSprites
            mFoldedLines = null
            invokeLater {
                attChangeListener?.onAttUpdate()
            }
        }

        // Run first update to get everything going
        update(variant, part)
    }

    /**
     * Update the editor image
     *
     * @param variant variant of att file
     * @param part    part to refresh
     */
    fun update(variant: CaosVariant, part: Char) {
        val (first, second) = assumedLinesAndPoints(variant, part)
        this.mLinesCount = first
        this.mPointsCount = second
        mPointNames = pointNames(part)
        this.setPart(part)
    }

    internal fun setVariant(variant: CaosVariant) {
        mVariant = variant
        mAttData?.let { data ->
            mAttData = parse(data.toFileText(variant), fileName = attFile.name)
        }
        cacheVariant(attFile, variant)
    }

    internal fun setPose(pose: Pose?) {
        if (!attFile.isValid) {
            return
        }
        attFile.putUserData(AttEditorPanel.ATT_FILE_POSE_KEY, pose)
    }

    internal fun getPose(): Pose? {
        if (!attFile.isValid) {
            return null
        }
        return attFile.getUserData(AttEditorPanel.ATT_FILE_POSE_KEY)
    }

    internal fun getRequestedPose(): Pose? {
        if (!attFile.isValid) {
            return null
        }
        return attFile.getUserData(AttEditorPanel.REQUESTED_POSE_KEY)
    }


    internal fun getRequestedVisibility(): Map<Char, PoseRenderer.PartVisibility>? {
        if (!attFile.isValid) {
            return null
        }
        return attFile.getUserData(AttEditorPanel.REQUESTED_VISIBILITY_KEY)
    }

    fun setPart(part: Char) {
        if (this.mPart == part) {
            return
        }
        this.mPart = part
        update(variant, part)
    }

    fun getBreedPartKey(): BreedPartKey? {
        return fromFileName(attFile.nameWithoutExtension, variant)
    }

    fun getImages(): List<BufferedImage?> {
        mImages?.let {
            return it
        }

        if (!spriteFile.isValid) {
            return emptyList()
        }
        var images: List<BufferedImage> = runBlocking {
            SpriteParser
                .parse(spriteFile)
                .imagesAsync()
        }
        images = images.mapIndexed { i, image ->
            var out: BufferedImage = image
            if (i % 16 in 4..7) {
                val repeated = image.width == 32 && image.height == 32 && image.isCompletelyTransparent
                if (repeated) {
                    try {
                        val sourceImage = images[i - 4]
                        out = images[i - 4].flipHorizontal()
                        if (out.width != sourceImage.width || out.height != sourceImage.height) {
                            LOGGER.severe("Failed to properly maintain scale in image.")
                        }
                    } catch (e: Exception) {
                        e.rethrowAnyCancellationException()
                    }
                }
            }
            out
        }
        mImages = images
        return images
    }

    /**
     * Inits change listeners for this att file
     *
     * @param psiFile the psi file for this att editor
     */
    private fun initDocumentListeners(project: Project, psiFile: PsiFile?) {

        // If file is not null. add commit file listeners
        if (psiFile != null && psiFile.isValid) {
            // Get document for psi file
            PsiDocumentManager.getInstance(project)
                .getDocument(psiFile)?.apply {
                    if (this@AttEditorModel.document == null) {
                        this@AttEditorModel.document = this
                    }
                }
                ?.addDocumentListener(this@AttEditorModel)
        }

        // If this document is null
        // Add a local file system watcher, which watches for changes to any file
        if (this.document == null) {
            // Add listener for all files
            LocalFileSystem.getInstance().addVirtualFileListener(this)
        }
    }

    private fun removeDocumentListeners(project: Project) {
        if (disposed.get()) {
            return
        }
        try {
            if (attFile.isValid) {
                attFile.getPsiFile(project)?.document?.removeDocumentListener(this)
            }
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
        }
        LocalFileSystem.getInstance().removeVirtualFileListener(this)
        attChangeListener = null
    }

    override fun contentsChanged(
        event: VirtualFileEvent,
    ) {
        if (!attFile.isValid) {
            return
        }
        // Check that changed file is THIS att file
        if (attFile.path == event.file.path) {
            val text = event.file.contents
            onEdit(text)
        }
    }

    override fun documentChanged(
        event: DocumentEvent,
    ) {

        if (!attFile.isValid) {
            return
        }
        // If this change was made internally by this class
        // update display, but do not update att file data
        // As it was just set by the application
        if (changedSelf) {
            changedSelf = false
            attChangeListener?.onAttUpdate()
            return
        }

        // Get the document text from the changed document
        // Changed due to external file changes, or an undo/redo operation
        val text = event.document.text
        onEdit(text)
    }

    /**
     * Called when the document is edited
     */
    private fun onEdit(text: String) {
        if (!attFile.isValid) {
            return
        }

        // Check if new data, equals old data
        // Return if it does
        @Suppress("ControlFlowWithEmptyBody")
        if (attData.toFileText(variant) == text) {
//            return
        }
        val oldLines = attData.lines
        val newData = parse(text, mLinesCount, mPointsCount, fileName = fileName)
        val newLines = newData.lines.toMutableList()
        val maxCheck = minOf(newLines.size, oldLines.size)
        val changed = (0 until maxCheck).filter { i ->
            oldLines[i] != newLines[i]
        }
        val changedLineNumber = if (changed.size == 1) {
            changed[0]
        } else {
            null
        }
//        if (changedLineNumber != null && replicateAttsToDuplicateSprites != false) {
//            val changedLine = newLines[changedLineNumber]
//            val replications = getReplications(changedLineNumber)
//                .filter { it !in independent }
//            for (replicant in replications) {
//                newLines[replicant] = changedLine
//            }
//        }

        // Parse the file data and set it to the ATT instance data
        mAttData = AttFileData(newLines, fileName = fileName)

        if (changedLineNumber != null && mSelectedCell != changedLineNumber) {
            val changedLine = changed[0]
            setSelected(changedLine, true)
        }

        invokeLater {
            attChangeListener?.onAttUpdate()
        }
    }

    fun setCurrentPoint(newPoint: Int) {
        disposeRelativeAtt()
        if (shiftAttachmentPointInRelatedAtt) {
            setupAttachmentPointRelatedAttFile()
        }
        // Ensure point is within valid range

        // Ensure point is within valid range
        if (newPoint < 0) {
            throw IndexOutOfBoundsException("New point cannot be less than zero. Found: $newPoint")
        }
        if (newPoint > 5) {
            throw IndexOutOfBoundsException("New point '$newPoint' is out of bound. Should be (0..5)")
        }

        // Sets the current point to edit when point change events are fires

        // Sets the current point to edit when point change events are fires
        this.mCurrentPoint = newPoint
    }


    override fun onShiftPoint(lineNumber: Int, offset: Pair<Int, Int>) {
        if (!attFile.isValid) {
            return
        }
        if (readonly) {
            showNotification("File is readonly", MessageType.INFO)

            return
        }
        val data = getEnsuringLines(lineNumber)
        val oldPoints = data.lines[lineNumber]
        val oldPoint = oldPoints[currentPoint]
        val point = Pair(oldPoint.first + offset.first, oldPoint.second + offset.second)
        shiftRelative(offset.first, offset.second)
        onChangePoint(lineNumber, point)
    }

    override fun onChangePoint(
        lineNumber: Int,
        newPoint: Pair<Int, Int>,
    ) {
        if (!attFile.isValid) {
            return
        }
        if (readonly) {
            showNotification("File is readonly", MessageType.INFO)
            return
        }
        val data = getEnsuringLines(lineNumber)
        val oldPoints = data.lines[lineNumber].points
        var delta: Pair<Int, Int> = Pair(0, 0)
        val changedPoint = oldPoints.getOrNull(currentPoint)?.let { oldPoint ->
            if (lockX && lockY) {
                oldPoint
            } else if (lockX) {
                Pair(oldPoint.first, newPoint.second)
            } else if (lockY) {
                Pair(newPoint.first, oldPoint.second)
            } else {
                newPoint
            }
        } ?: newPoint

        val currentReplication = currentReplication
        val currentPointIndex = currentPoint
        val newPoints = oldPoints.mapIndexed { i, oldPoint ->
            if (i == currentPointIndex) {
                delta = Pair(changedPoint.first - oldPoint.first, changedPoint.second - oldPoint.second)
                changedPoint
            } else {
                oldPoint
            }
        }
        val newLine = AttFileLine(newPoints.subList(0, pointsCount))
        val oldLines: List<AttFileLine> = data.lines
        val newLines: MutableList<AttFileLine> = ArrayList()
        for (i in oldLines.indices) {
            newLines.add(if (i == lineNumber || i in currentReplication) newLine else oldLines[i])
        }
        setAttData(AttFileData(newLines, attFile.name))
        shiftRelative(delta.first, delta.second)
    }

    private fun getEnsuringLines(lineNumber: Int): AttFileData {
        val data = mAttData
        val blank = (0..(pointsCount / 2)).map { Pair(0, 0) }
        return if (data == null) {
            val lines = (0..linesCount).map {
                AttFileLine(blank)
            }
            AttFileData(lines, attFile.name).apply {
                mAttData = this
            }
        } else if (data.lines.size <= lineNumber) {
            val needed = lineNumber - data.lines.size
            val lines = data.lines + (0..needed).map { AttFileLine(blank) }
            AttFileData(lines, attFile.name).apply {
                mAttData = this
            }
        } else {
            data
        }
    }

    private fun setAttData(attData: AttFileData) {
        mAttData = attData
        try {
            if (!writeFile(attData)) {
                LOGGER.severe("Failed to write Att file data")
            }
            attChangeListener?.onAttUpdate()
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
            e.printStackTrace()
        }
    }

    internal fun reloadFiles() {
        mImages = null
        getImages()
        mReplications = null
        getReplications()
        showNotification("Reloaded ATT data", MessageType.INFO)
    }

    fun getFoldedLines(): List<Int> {
        if (replicateAttsToDuplicateSprites == false) {
            return emptyList()
        }
        mFoldedLines?.let {
            return it
        }
        val replications = getReplications()
        val folded = (0 until linesCount)
            .filter { i ->
                i != 0 && replications[i - 1]?.contains(i) == true
            }
        mFoldedLines = folded
        return folded
    }

    internal fun getReplications(): Map<Int, List<Int>> {
        if (replicateAttsToDuplicateSprites == false) {
            return emptyMap()
        }
        mReplications?.let {
            return it
        }
        val images = getImages()
        val map = mutableMapOf<Int, List<Int>>()
        for (i in images.indices) {
            val image = images[i]
                ?: continue
            val matches = mutableListOf<Int>()
            for (j in images.indices) {
                if (i in mNotReplicatedAtts) {
                    map[i] = emptyList()
                    continue
                }
                val anImage = images[j]
                    ?: continue
                if (j !in mNotReplicatedAtts && i != j && image.contentsEqual(anImage)) {
                    matches.add(j)
                }
            }
            map[i] = matches
        }
        mReplications = map
        return map
    }

    private val currentReplication: List<Int>
        get() {
            mCurrentReplication?.let {
                return it
            }

            val replications = runBlocking { getReplications(mSelectedCell) }
            mCurrentReplication = replications
            val project = project
            if (project != null && replicateAttsToDuplicateSprites == null && replications.isNotEmpty()) {
                CaosApplicationSettingsService.getInstance().replicateAttsToDuplicateSprites = true
                showReplicationNotice(project)
            }
            return replications
        }

    private fun getReplications(line: Int): List<Int> {
        return if (replicateAttsToDuplicateSprites == false || mNotReplicatedAtts.contains(line)) {
            emptyList()
        } else {
            getReplications()[line] ?: emptyList()
        }
    }


    private fun showReplicationNotice(project: Project) {
        CaosNotifications.createInfoNotification(
            project,
            "ATT Point Replication",
            "<b>Sprite contains duplicate frames</b>. Att changes in the visual editor will be replicated<br />" +
                    "NOTE: Manual text changes will not be replicated<br/>" +
                    "You can change this setting in the CAOS settings panel"
        ).addAction(object : AnAction("Disable Replication?") {

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }

            override fun update(e: AnActionEvent) {
                super.update(e)
                e.presentation.description = "Disabled ATT point replication in this and future ATT files.\n" +
                        "Setting can be changed later in the CAOS and Agenteering settings panel"
            }

            override fun actionPerformed(e: AnActionEvent) {
                CaosApplicationSettingsService.getInstance().replicateAttsToDuplicateSprites = false
            }
        })
            .show()
    }

//    fun commit() {
//        ApplicationManager.getApplication().invokeLater {
//            ApplicationManager.getApplication().runWriteAction {
//                val project = project
//                    ?: return@runWriteAction
//                if (project.isDisposed) {
//                    return@runWriteAction
//                }
//                if (!attFile.isValid) {
//                    return@runWriteAction
//                }
//                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document!!)
//                PsiDocumentManager.getInstance(project).commitDocument(document!!)
//            }
//        }
//    }

    private fun writeFile(fileData: AttFileData): Boolean {
        val project = project
            ?: return false
        if (project.isDisposed)
            return false
        val psiFile = PsiManager.getInstance(project).findFile(attFile)
        if (psiFile == null) {
            LOGGER.severe("Cannot update ATT file without PSI file")
            return false
        }
        if (!psiFile.isValid) {
            return false
        }
        val file = SmartPointerManager.createPointer(psiFile)
        WriteCommandAction.runWriteCommandAction(
            project,
            "Move Points",
            COMMAND_GROUP_ID + '_' + selectedCell + '_' + actionId.incrementAndGet(),
            {
                if (project.isDisposed)
                    return@runWriteCommandAction
                val theFile = file.element
                    ?: return@runWriteCommandAction
                if (!theFile.isValid) {
                    return@runWriteCommandAction
                }
//                psiFile.navigate(true)
                changedSelf = true
                val document = PsiDocumentManager.getInstance(project).getDocument(theFile)
                if (document == null) {
                    LOGGER.severe("Cannot write ATT file without document")
                    return@runWriteCommandAction
                }
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                document.setText(fileData.toFileText(variant).replace("\r?\n".toRegex(), "\n"))
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            }, psiFile
        )
        return true
    }


    override fun setSelected(index: Int, sender: Any?): Int {
        var targetIndex: Int
        val lastIndex = (if (variant.isOld) 10 else 16) - 1 // Last possible line in file
        targetIndex = if (index < 0) {
            0
        } else if (index > lastIndex) {
            lastIndex
        } else {
            index
        }

        targetIndex = if (replicateAttsToDuplicateSprites == false) {
            index
        } else {
            getNotFoldedLine(targetIndex)
        }
        mCurrentReplication = null
        mSelectedCell = targetIndex
        if (sender != selectedCellHolder) {
            selectedCellHolder?.setSelected(targetIndex, false)
        }
        return targetIndex
    }

    /**
     * Get the cell that is not currently folded, or itself if it cannot find a not folded cell
     */
    private fun getNotFoldedLine(newIndex: Int): Int {
        if (newIndex == mSelectedCell) {
            return newIndex
        }
        val lastIndex = (if (variant.isOld) 10 else 16) - 1

        if (newIndex < 0) {
            return 0
        }
        if (newIndex > lastIndex) {
            return lastIndex
        }

        val replications = runBlocking { getReplications() }
        val mod = newIndex - mSelectedCell

        // If previous image and this are not consecutive, just jump
        if (abs(mod) > 1) {
            return newIndex
        }

        if (replications[newIndex - mod]?.contains(newIndex) != true) {
            if (mod > 0) {
                return newIndex
            }
        }

        val isNew = replications[newIndex - mod]?.contains(newIndex) != true
        // Check if this image is completely different from the one before
        if (mod > 0 && isNew) {
            return newIndex
        }

        val indices = if (newIndex > mSelectedCell) {
            (newIndex + 1)..lastIndex
        } else {
            (newIndex - 1) downTo 0
        }
        var outIndex = newIndex
        val shouldChange = isNew || indices.any { !replications.containsKey(it) || newIndex !in replications[it]!! }
        if (!shouldChange) {
            return mSelectedCell
        }
        for (cell in indices) {
            if (replications[cell]?.contains(newIndex) == true) {
                outIndex = cell + mod
            } else {
                break
            }
        }
        // If having shifted up, the outIndex would point to the first non-duplicate,
        // but it should be the last duplicate, so add one to index
        if (mod < 0 && outIndex != newIndex) {
            outIndex += 1
        }
        return outIndex
    }

    override fun dispose() {
        if (disposed.getAndSet(true)) {
            return
        }
        val project = project
            ?: return
        if (project.isDisposed) {
            return
        }
        selectedCellHolder = null
        partBreedsProvider = null
        removeDocumentListeners(project)
    }


    private fun setupAttachmentPointRelatedAttFile() {
        val project = project
        if (!shiftAttachmentPointInRelatedAtt || project == null || project.isDisposed) {
            disposeRelativeAtt()
            return
        }
        val selected = currentPoint
        if (currentPoint > 5) {
            return
        }
        val relativePartData = RelativeAttUtil.getRelative(part, selected)
        if (relativePartData == null) {
            disposeRelativeAtt()
            showNotification("No relative ATT for part", MessageType.INFO)
            return
        }
        val (relativePart, relativePoint) = relativePartData

        this.mRelativePart = relativePart

        // Dispose current relative att object
        if (relatedAtt?.part != relativePart) {
            disposeRelativeAtt()
        }

        // Construct relative att file name
        val breedKey = getBreedPartKey()
            ?.copyWithPart(relativePart)
            ?.code
        val fileName = "$breedKey.att"

        // Get relative att in parent folder
        val file = attFile.parent.findChild(fileName)
            ?: throw Exception("Failed to find relative file; Relative ATT file must be in the same folder as this base ATT")

        val relAtt = RelativeAtt(
            variant,
            project,
            file,
            relativePart,
            relativePoint,
            this,
        )

        relatedAtt = relAtt
        // Setup whether this is actually possible
        val otherPartBreed = partBreedsProvider?.getPartBreed(relativePart)
        onBreedSelected(otherPartBreed, relativePart)
    }


    internal fun setShiftAttachmentPointInRelatedAtt(shift: Boolean) {
        if (!shiftAttachmentPointInRelatedAtt) {
            disposeRelativeAtt()
            relatedAtt = null
        }

        this.mShiftAttachmentPointInRelatedAtt = shift

        if (shift) {
            setupAttachmentPointRelatedAttFile()
        }
    }

    fun shiftRelative(thisDeltaX: Int, thisDeltaY: Int) {
        if (!enableShiftRelativeAtt) {
            return
        }
        if (!shiftAttachmentPointInRelatedAtt || !shiftPointInRelatedAttAllowed) {
            return
        }
        val relativeAtt = this.relatedAtt
            ?: return
        if (!relativeAtt.moveRelative(thisDeltaX, thisDeltaY) && !relativeFileFailed) {
            this.relativeFileFailed = true
            showNotification("Failed to move relative att", MessageType.ERROR)
        }
    }

    private fun disposeRelativeAtt() {
        mRelativePart = null
        relatedAtt?.dispose()
        relatedAtt = null
    }

    override fun onBreedSelected(key: BreedKey?, vararg parts: Char) {
        if (!shiftAttachmentPointInRelatedAtt) {
            return
        }
        val relativePart = relativePart
            ?: return

        // If neither part of interest has changed, do nothing
        if (part !in parts && relativePart !in parts) {
            return
        }

        // Get this ATTs breed
        val thisBreed = this.getBreedPartKey()?.breedKey
            ?: return

        // Get other ATT
        this.shiftPointInRelatedAttAllowed = !thisBreed.isGenericMatchFor(key)
    }


    companion object {
        internal const val enableShiftRelativeAtt = false
        private const val COMMAND_GROUP_ID = "ATTEditor"
        private val LOGGER: Logger = Logger.getLogger("#AttEditorModel")

        @JvmStatic
        fun assumedLinesAndPoints(variant: CaosVariant, part: Char): Pair<Int, Int> {
            val lines = if (variant.isOld) 10 else 16
            val columns = when (part.lowercase()) {
                'a' -> when {
                    variant.isOld -> 2
                    else -> 5
                }

                'b' -> 6
                'q' -> 1
                'z' -> 1
                else -> 2
            }
            return Pair(lines, columns)
        }

        @JvmStatic
        fun cacheVariant(virtualFile: VirtualFile, variant: CaosVariant) {
            if (!virtualFile.isValid) {
                return
            }
            ExplicitVariantFilePropertyPusher.writeToStorage(virtualFile, variant)
            ImplicitVariantFilePropertyPusher.writeToStorage(virtualFile, variant)
            virtualFile.putUserData(CaosScriptFile.ExplicitVariantUserDataKey, variant)
            virtualFile.putUserData(CaosScriptFile.ImplicitVariantUserDataKey, variant)
            for (sibling in virtualFile.parent?.children.orEmpty()) {
                if (!sibling.isDirectory) {
                    sibling.setCachedIfNotCached(variant, false)
                }
            }
        }


        internal fun pointNames(part: Char): List<String> {
            return when (part.lowercaseChar()) {
                'a' -> listOf(
                    "Neck",
                    "Mouth",
                    "(L)Ear",
                    "(R)Ear",
                    "Hair"
                )

                'b' -> listOf(
                    "Neck",
                    "(L)Thigh",
                    "(R)Thigh",
                    "(L)Arm",
                    "(R)Arm",
                    "Tail"
                )

                'c', 'f' -> listOf(
                    "Hip",
                    "Knee"
                )

                'd', 'g' -> listOf(
                    "Knee",
                    "Ankle"
                )

                'e', 'h' -> listOf(
                    "Ankle",
                    "Toe"
                )

                'i', 'k' -> listOf(
                    "Shoulder",
                    "Elbow"
                )

                'j', 'l' -> listOf(
                    "Elbow",
                    "Hand"
                )

                'q' -> listOf("Head")
                'z' -> listOf("Center")

                else -> listOf(
                    "Start",
                    "End"
                )
            }
        }


    }
}


private val BufferedImage.isCompletelyTransparent: Boolean
    get() {
        for (y in 0 until this.height) {
            for (x in 0 until this.width) {
                if (!this.isTransparent(x, y)) {
                    return false
                }
            }
        }
        return true
    }

fun BufferedImage.isTransparent(x: Int, y: Int): Boolean {
    val pixel = this.getRGB(x, y)
    return pixel shr 24 == 0x00
}


private data class RelativeAtt(
    val variant: CaosVariant,
    val project: Project,
    val file: VirtualFile,
    val part: Char,
    val point: Int,
    var disposable: Disposable,
) : Disposable {

    private var watcher: PsiTreeChangeListener? = null
    private var mAttModel: AttFileData? = null
    private val attModel: AttFileData?
        get() = mAttModel ?: try {
            parse(project, file).also {
                mAttModel = it
            }
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
            null
        }

    init {
        if (!project.isDisposed) {
            initWatcher()
            // Register disposer to prevent memory leaks
            Disposer.register(disposable, this)
        }
    }

    fun moveRelative(otherDeltaX: Int, otherDeltaY: Int): Boolean {
        val oldModel = attModel
            ?: return false
        val lines = oldModel.lines
        val updatedLines = lines.map {
            val points = it.points.mapIndexed { i, point ->
                if (i == this.point) {
                    Pair(point.first + otherDeltaX, point.second + otherDeltaY)
                } else {
                    point
                }
            }
            AttFileLine(points)
        }

        val updated = oldModel.copy(
            lines = updatedLines
        )
        mAttModel = updated
        val psi = file.getPsiFile(project)
            ?: return false
        val document = psi.document
            ?: return false
        try {
            EditorUtil.replaceText(document, psi.textRange, updated.toFileText(variant))
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
            return false
        }
        return try {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            true
        } catch (e: Throwable) {
            e.rethrowAnyCancellationException()
            false
        }
    }

    private fun initWatcher() {
        val onChange = change@{ event: PsiTreeChangeEvent ->
            if (event.file?.virtualFile?.path?.lowercase() != file.path.lowercase()) {
                return@change
            }
            mAttModel = parse(project, file)
        }

        val theWatcher = object : PsiTreeChangeAdapter() {
            override fun childrenChanged(event: PsiTreeChangeEvent) {
                onChange(event)
            }

            override fun childAdded(event: PsiTreeChangeEvent) {
                onChange(event)
            }

            override fun childRemoved(event: PsiTreeChangeEvent) {
                onChange(event)
            }

            override fun childMoved(event: PsiTreeChangeEvent) {
                onChange(event)
            }

            override fun childReplaced(event: PsiTreeChangeEvent) {
                onChange(event)
            }
        }

        watcher = theWatcher
        PsiManager.getInstance(project).addPsiTreeChangeListener(theWatcher, this)
    }

    override fun dispose() {
        watcher?.let {
            PsiManager.getInstance(project).removePsiTreeChangeListener(it)
        }
        watcher = null
    }
}