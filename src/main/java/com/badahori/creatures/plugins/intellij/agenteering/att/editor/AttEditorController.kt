package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.bedalton.creatures.common.structs.BreedKey
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import javax.swing.JComponent

internal class AttEditorController(
    val project: Project,
    parent: Disposable,
    attFile: VirtualFile,
    spriteFile: VirtualFile,
    variant: CaosVariant,
    override val showFooterNotification: (message: String, messageType: MessageType) -> Unit
) : AttEditorHandler, AttChangeListener, Disposable, PartBreedsProvider {

    private val model: AttEditorModel by lazy {
        AttEditorModel(
            project,
            parent,
            attFile,
            spriteFile,
            variant,
            showFooterNotification,
            this,
            this,
            this
        )
    }

    private lateinit var mView: View

    override val shiftRelativeAtt: Boolean
        get() = model.shiftAttachmentPointInRelatedAtt

    internal val view: View
        get() {
            if (this::mView.isInitialized) {
                return mView
            }
            val view = createView()
            mView = view
            return view
        }

    override var lockX: Boolean
        get() = model.lockX
        set(value) {
            model.lockX = value
        }

    override var lockY: Boolean
        get() = model.lockY
        set(value) {
            model.lockY = value
        }
    override val replications: Map<Int, List<Int>>
        get() = model.getReplications()

    override val notReplicatedLines: List<Int>
        get() = model.notReplicatedAtts

    override fun setShiftRelativeAtt(shift: Boolean) {
        model.setShiftAttachmentPointInRelatedAtt(shift)
    }

    val isInitialized: Boolean
        get() = this::mView.isInitialized

    override val attData: AttFileData
        get() = model.attData

    override val part: Char
        get() = model.part

    override val spriteFile: VirtualFile
        get() = model.spriteFile

    override val rootPath: VirtualFile
        get() = model.rootPath

    override val pointNames: List<String>
        get() = model.pointNames

    override val fileName: String
        get() = model.fileName

    override val variant: CaosVariant
        get() = model.variant

    override val linesCount: Int
        get() = model.linesCount

    override val pointCount: Int
        get() = model.pointsCount

    override val attLines: List<AttFileLine>
        get() = model.attLines

    override val images: List<BufferedImage?>
        get() = model.getImages()

    override val selectedCell: Int
        get() = model.selectedCell

    override val relativePart: Char?
        get() = model.relativePart

    init {
        if (!project.isDisposed) {
            Disposer.register(parent, this)
        }
    }

    /**
     * Gets the point that will be updated when changes are made
     */
    override fun getCurrentPoint(): Int {
        return model.currentPoint
    }

    override fun getFolded(): List<Int> {
        return model.getFoldedLines()
    }

    /**
     * Sets the point to edit
     */
    override fun setCurrentPoint(point: Int) {
        model.setCurrentPoint(point)
    }

    /**
     * Sets the variant of this file
     */
    override fun setVariant(variant: CaosVariant) {
        model.setVariant(variant)
    }

    fun clearPose() {
        view.clearPose()
    }

    override fun getPartBreed(part: Char?): BreedKey? {
        return view.getPartBreed(part)
    }

    /**
     * Sets the part that is being edited
     */
    override fun setPart(part: Char) {
        model.setPart(part)
    }

    /**
     * Sets the cached pose for this file
     */
    override fun setPose(pose: Pose?) {
        return model.setPose(pose)
    }

    /**
     * Gets the cached pose for this file
     */
    override fun getPose(): Pose? {
        return model.getPose()
    }

    /**
     * Gets the cached pose for this file
     */
    override fun getRequestedPose(): Pose? {
        return model.getRequestedPose()
    }

    override fun getRequestedVisibility(): Map<Char, PoseRenderer.PartVisibility>? {
        return model.getRequestedVisibility()
    }

    /**
     * Gets the breed part key for this file
     */
    override fun getBreedPartKey(): BreedPartKey? {
        return model.getBreedPartKey()
    }

    /**
     * Called when a change to the ATT is made
     */
    override fun onAttUpdate(vararg lines: Int) {
        view.onAttUpdate(*lines)
    }

    /**
     * Moves an ATT point to a new location
     */
    override fun onChangePoint(lineNumber: Int, newPoint: Pair<Int, Int>) {
        model.onChangePoint(lineNumber, newPoint)
    }

    /**
     * Shifts the point by a given amount
     */
    override fun onShiftPoint(lineNumber: Int, offset: Pair<Int, Int>) {
        model.onShiftPoint(lineNumber, offset)
    }

    /**
     * Sets the selected Cell/Line for editing
     */
    override fun setSelected(index: Int, sender: Any?): Int {
        if (this == sender) {
            return index
        }
        if (this::mView.isInitialized) {
            if (view.selectedCell != index && view != sender) {
                view.setSelected(index, sender = sender)
            }
        }
        if (model.selectedCell != index && model != sender) {
            return model.setSelected(index, sender = sender)
        }
        return index
    }

    /**
     * Gets the current component
     */
    internal fun getComponent(): Any {
        return view.component
    }

    internal fun getPreferredFocusComponent(): JComponent {
        return view.preferredFocusComponent()
    }

    internal fun getPopupMessageTarget(): JComponent {
        return view.toolbar as JComponent
    }

    /**
     * Creates the ATT view panel
     */
    private fun createView(): AttEditorPanel {
        val mView = AttEditorPanel(project, this, this)
        if (project.isDisposed) {
            return mView
        }

        return mView
    }

    override fun dispose() {
//        model.dispose()
//        view.dispose()
    }

    override fun reloadFiles() {
        model.reloadFiles()
        view.refresh()
    }

    /**
     * Interface for the view
     */
    internal interface View : AttChangeListener, Disposable, PartBreedsProvider, HasSelectedCell {
        val component: Any
        val toolbar: Any
        fun init()
        fun refresh()
        fun clearPose()
        fun scrollCellIntoView()
        fun preferredFocusComponent(): JComponent
        fun togglePartVisibility(part: Char, visibility: PartVisibility)
        fun incrementPartPose(part: Char)
    }

}


internal interface AttChangeListener {
    fun onAttUpdate(vararg lines: Int)
}

internal interface AttEditorHandler : OnChangePoint, HasSelectedCell {
    val variant: CaosVariant
    val spriteFile: VirtualFile
    val part: Char
    val attData: AttFileData
    val linesCount: Int
    val pointCount: Int
    val attLines: List<AttFileLine>
    val images: List<BufferedImage?>
    val rootPath: VirtualFile
    val pointNames: List<String>
    val fileName: String
    val showFooterNotification: (message: String, messageType: MessageType) -> Unit
    var lockX: Boolean
    var lockY: Boolean
    val replications: Map<Int, List<Int>>
    val notReplicatedLines: List<Int>
    val shiftRelativeAtt: Boolean
    val relativePart: Char?
    fun setShiftRelativeAtt(shift: Boolean)
    fun getCurrentPoint(): Int
    fun getFolded(): List<Int>
    fun setCurrentPoint(point: Int)
    fun setVariant(variant: CaosVariant)
    fun setPart(part: Char)
    fun setPose(pose: Pose?)
    fun getPose(): Pose?
    fun getRequestedPose(): Pose?
    fun getRequestedVisibility(): Map<Char, PoseRenderer.PartVisibility>?
    fun getBreedPartKey(): BreedPartKey?
    fun reloadFiles()
}