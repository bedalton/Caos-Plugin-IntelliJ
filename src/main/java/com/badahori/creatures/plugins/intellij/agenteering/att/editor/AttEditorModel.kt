@file:Suppress("MemberVisibilityCanBePrivate")

package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttAutoFill.paddedData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileLine
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileParser.parse
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ExplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ImplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey.Companion.fromFileName
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

internal class AttEditorModel(
    project: Project,
    val attFile: VirtualFile,
    val spriteFile: VirtualFile,
    variant: CaosVariant,
    val showNotification: (message: String, messageType: MessageType) -> Unit,
    private var attChangeListener: AttChangeListener?,
) : OnChangePoint, HasSelectedCell, Disposable, VirtualFileListener, DocumentListener {

    private var mImages: List<BufferedImage>? = null
    private var mVariant: CaosVariant = variant
    internal val variant: CaosVariant get() = mVariant
    private var changedSelf: Boolean = false
    private var disposed: AtomicBoolean = AtomicBoolean(false)

    private var project: Project? = project

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

    private var md5: String? = null
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


    val actionId = AtomicInteger(0)

    val readonly = attFile is CaosVirtualFile

    init {
        val psiFile = attFile.getPsiFile(project)
        initDocumentListeners(project, psiFile)
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
        var images: List<BufferedImage> = SpriteParser.parse(spriteFile).images
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
                    } catch (_: Exception) {
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
                .getDocument(psiFile)
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
        } catch (_: Exception) {
        } catch (_: Error) {
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
        val newMD5 = MD5.fromString(text)
        if (md5 != null && md5 == newMD5) {
            return
        }
        md5 = newMD5

        // Check if new data, equals old data
        // Return if it does
        @Suppress("ControlFlowWithEmptyBody")
        if (attData.toFileText(variant) == text) {
//            return
        }
        // Parse the file data and set it to the ATT instance data
        mAttData = parse(text, mLinesCount, mPointsCount, fileName = fileName)

        ApplicationManager.getApplication().invokeLater {
            attChangeListener?.onAttUpdate()
        }
    }

    fun setCurrentPoint(newPoint: Int) {
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
        val newPoints: MutableList<Pair<Int, Int>> = ArrayList()
        var changedPoint = newPoint
        for (i in oldPoints.indices) {
            if (currentPoint == i) {
                if (lockY) {
                    changedPoint = Pair(newPoint.first, oldPoints[i].second)
                }
                if (lockX) {
                    changedPoint = Pair(oldPoints[i].first, newPoint.second)
                }
            }
            newPoints.add(if (i == currentPoint) changedPoint else oldPoints[i])
        }
        val newLine = AttFileLine(newPoints.subList(0, pointsCount))
        val oldLines: List<AttFileLine> = data.lines
        val newLines: MutableList<AttFileLine> = ArrayList()
        for (i in oldLines.indices) {
            newLines.add(if (i == lineNumber) newLine else oldLines[i])
        }
        setAttData(AttFileData(newLines, attFile.name))
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
//            attChangeListener.onAttUpdate()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    internal fun reloadFiles() {
        mImages = null
        getImages()
        showNotification("Reloaded ATT data", MessageType.INFO)
    }


    fun commit() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                val project = project
                    ?: return@runWriteAction
                if (project.isDisposed) {
                    return@runWriteAction
                }
                if (!attFile.isValid) {
                    return@runWriteAction
                }
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document!!)
                PsiDocumentManager.getInstance(project).commitDocument(document!!)
            }
        }
    }

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

    companion object {

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

                else -> Arrays.asList(
                    "Start",
                    "End"
                )
            }
        }


    }

    override fun setSelected(index: Int) {
        if (index < 0) {
            return
        }
        if (index >= attData.lines.size) {
            return
        }
        mSelectedCell = index
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
        removeDocumentListeners(project)
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