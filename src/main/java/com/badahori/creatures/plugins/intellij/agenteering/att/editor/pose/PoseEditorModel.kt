@file:Suppress("UNUSED_PARAMETER")

package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose.Companion.fromString
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseCalculator.calculateHeadPose
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport.allParts
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.CreatureSpriteSet
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.render
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartsIndex
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem.Companion.instance
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class PoseEditorModel(
    val project: Project,
    var variant: CaosVariant,
    val poseEditor: BreedPoseHolder,
) {


    private val allValidPoseChars = "012345xX?!".toCharArray()
    private val validPartPoseChars = "0123xX?!".toCharArray()

    var progressIndicator: ProgressIndicator? = null
    private var spriteSet: CreatureSpriteSet? = null
    private var mLocked: Map<Char, BodyPartFiles> = emptyMap()
    private var updating = AtomicInteger(0)
    private var mRendering: Long? = null
    private var renderId = AtomicInteger(0)
    val rendering: Boolean get() = mRendering?.let { it < now } ?: false

    val locked: Map<Char, BodyPartFiles> get() = mLocked


    private val manualAtts: MutableMap<Char, BodyPartFiles> by lazy {
        mutableMapOf()
    }

    private val virtualAttFolder: CaosVirtualFile? by lazy {
        val folder = randomString(12)
        try {
            instance.getOrCreateRootChildDirectory(folder)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    fun setImmediate(directory: VirtualFile, key: BreedPartKey) {
        if (project.isDisposed) {
            updating.set(0)
            return
        }
        if (updating.getAndIncrement() > 0) {
            updating.decrementNotNegative()
            return
        }
        GlobalScope.launch(Dispatchers.IO) launch@{
            if (project.isDisposed) {
                updating.decrementNotNegative()
                Exception().printStackTrace()
                return@launch
            }

            val mappedParts = BodyPartsIndex.getImmediate(project, directory, key)

            if (mappedParts == null) {
                updating.decrementNotNegative()
                updating.set(0)
                executeOnPooledThread(this@PoseEditorModel::updateFiles)
                return@launch
            }

            val sprites = CreatureSpriteSet(
                head = mappedParts['a']!!.data(project),
                body = mappedParts['b']!!.data(project),
                leftThigh = mappedParts['c']!!.data(project),
                leftShin = mappedParts['d']!!.data(project),
                leftFoot = mappedParts['e']!!.data(project),
                rightThigh = mappedParts['f']!!.data(project),
                rightShin = mappedParts['g']!!.data(project),
                rightFoot = mappedParts['h']!!.data(project),
                leftUpperArm = mappedParts['i']!!.data(project),
                leftForearm = mappedParts['j']!!.data(project),
                rightUpperArm = mappedParts['k']!!.data(project),
                rightForearm = mappedParts['l']!!.data(project),
                tailBase = mappedParts['m']?.data(project),
                tailTip = mappedParts['n']?.data(project),
                leftEar = mappedParts['o']?.data(project),
                rightEar = mappedParts['p']?.data(project),
                hair = mappedParts['q']?.data(project)
            )

            spriteSet = sprites
            poseEditor.setFiles(mappedParts.values.filterNotNull())
            @Suppress("SpellCheckingInspection")
            requestRender(*("abcdefghijklmnopq").toCharArray())
            updating.set(0)
            executeOnPooledThread(this@PoseEditorModel::updateFiles)
        }
    }

    private fun updateFiles() {
        if (project.isDisposed) {
            return
        }
        if (updating.getAndIncrement() > 0) {
            updating.decrementNotNegative()
            Exception().printStackTrace()
            return
        }

        GlobalScope.launch(Dispatchers.IO) launch@{
            if (project.isDisposed) {
                updating.set(0)
                return@launch
            }
            if (DumbService.isDumb(project)) {
                updating.set(0)
                DumbService.getInstance(project).runWhenSmart {
                    executeOnPooledThread(this@PoseEditorModel::updateFiles)
                }
                return@launch
            }
            try {
                val files = BodyPartsIndex.variantParts(project, variant, null)
                poseEditor.setFiles(files)
                updating.set(0)
            } catch (e: ProcessCanceledException) {
                updating.set(0)
                delay(20)
                updateFiles()
            }
        }
    }

    fun requestRender(vararg parts: Char) {
        GlobalScope.launch(Dispatchers.IO) {
            progressIndicator?.cancel()
            val indicator = EmptyProgressIndicator()
            progressIndicator = indicator
            try {
                requestRenderAsync(indicator, renderId.incrementAndGet(), *parts)
            } catch (_: ProcessCanceledException) {
            }
        }
    }

    private fun requestRenderAsync(progressIndicator: ProgressIndicator, id: Int, vararg parts: Char): Boolean {

        if (project.isDisposed) {
            return false
        }
        if (updating.get() > 0) {
//            return false
        }
        if (DumbService.isDumb(project)) {
            progressIndicator.checkCanceled()
            DumbService.getInstance(project).runWhenSmart {
                progressIndicator.checkCanceled()
                if (id < renderId.get()) {
                    return@runWhenSmart
                }
                requestRender(*parts)
            }
            return false
        }
        if (id < renderId.get()) {
            return true
        }

        progressIndicator.checkCanceled()
        mRendering = now + 900

        val updatedSprites = try {
            if (id < renderId.get()) {
                null
            } else {
                getUpdatedSpriteSet(progressIndicator, *parts)
                    ?: getUpdatedSpriteSet(progressIndicator, *allParts)
            }
        } catch (e: java.lang.Exception) {
            progressIndicator.checkCanceled()
            mRendering = null
            LOGGER.severe("Failed to located required sprites Error:(" + e.javaClass.simpleName + ") " + e.localizedMessage)
            e.printStackTrace()
            poseEditor.setRendered(null)
            return false
        }
        if (updatedSprites == null) {
            if (updating.get() == 0 && id == renderId.get()) {
                LOGGER.severe("Failed to update sprite sets without reason")
            }
            progressIndicator.checkCanceled()
            mRendering = null
            return false
        }
        progressIndicator.checkCanceled()
        val updatedPose: Pose = invokeAndWaitIfNeeded {
            try {
                if (id < renderId.get()) {
                    null
                } else {
                    poseEditor.updatePoseAndGet(progressIndicator, *parts)
                }
            } catch (e: ProcessCanceledException) {
                null
            }
        } ?: return false

        progressIndicator.checkCanceled()

        val visibilityMask = poseEditor.getVisibilityMask() ?: mapOf()

        progressIndicator.checkCanceled()

        return try {
            if (id < renderId.get()) {
                false
            } else {
                val image = render(variant, updatedSprites, updatedPose, visibilityMask, poseEditor.zoom)
                progressIndicator.checkCanceled()
                invokeLater {
                    progressIndicator.checkCanceled()
                    poseEditor.setRendered(image)
                }
                true
            }
        } catch (e: ProcessCanceledException) {
            return false
        } catch (e: java.lang.Exception) {
            LOGGER.severe("Failed to render pose. Error:(" + e.javaClass.simpleName + ") " + e.localizedMessage)
            e.printStackTrace()
            progressIndicator.checkCanceled()
            if (id == renderId.get()) {
                poseEditor.setRendered(null)
            }
            false
        } finally {
            progressIndicator.checkCanceled()
            if (id == renderId.get()) {
                mRendering = null
            }
        }
    }


    fun validateNewPose(
        currentPose: Pose?,
        newPoseRaw: String,
        lastPoseString: String,
        facing: Int,
    ): Triple<Pose?, Boolean, IntArray>? {
        val newPoseStringBuilder = StringBuilder(newPoseRaw)
        while (newPoseStringBuilder.length < 15) {
            newPoseStringBuilder.append(" ")
        }
        val poseStringPadded = newPoseStringBuilder.toString()
        var hasError = false
        if (newPoseRaw.length.let { it != 15 && it != 16 } || newPoseRaw.any { it !in allValidPoseChars }) {
            hasError = true
        } else if (lastPoseString.trim() == newPoseRaw.trim()) {
            return null
        }

        val (_, newPose) = fromString(variant, facing, currentPose, poseStringPadded)

        val errorChars = (0..14).filter { i ->
            val invalidValue = poseStringPadded[i] !in (if (i < 3) { allValidPoseChars } else { validPartPoseChars })
            if (i > 0) {
                val part = ('a'.code + i).toChar()
                val thisPose = newPose[part]
                thisPose == null || invalidValue
            } else {
                invalidValue
            }
        }
        return Triple(newPose, hasError || errorChars.isNotEmpty(), errorChars.toIntArray())
    }

    /**
     * Gets Sprite offset for this facing direction
     *
     * @param facing direction
     * @return sprite offset to first pose of direction
     */
    fun getFacingOffset(variant: CaosVariant, facing: Int): Int {
        return if (facing == 0) {
            0
        } else if (facing == 1) {
            4
        } else if (facing == 2) {
            if (variant.isOld) {
                0
            } else {
                8
            }
        } else {
            if (variant.isOld) {
                0
            } else {
                12
            }
        }
    }

    fun clearSpriteSet() {
        spriteSet = null
    }

    /**
     * Sets the att file data manually for a given part
     *
     * @param part part to update att data for
     * @param att  new att file data
     */
    fun setManualAtt(
        part: Char,
        breedString: String,
        spriteFile: VirtualFile?,
        att: AttFileData?,
    ) {
        val spriteSet = spriteSet
            ?: return
        if (att == null) {
            return
        }
        val virtualAttFolder = virtualAttFolder
        var moved = false
        var file: VirtualFile? = null
        if (virtualAttFolder != null) {
            try {
                file = CaosVirtualFile("$breedString.att", att.toFileText(variant))
                instance.moveFile(file, file, virtualAttFolder)
                moved = true
            } catch (ignored: Exception) {
            }
        }
        if (!moved) {
            file = CaosVirtualFile("${breedString}_${randomString(8)}.att", att.toFileText(variant))
            instance.addFile(file, true)
        }
        manualAtts[part] = BodyPartFiles(spriteFile!!, file!!)
        this.spriteSet = spriteSet.replacing(part, att)
        requestRender(part)
    }

    /**
     * Removes a manually set att from the list
     */
    fun removeManualAtt(part: Char) {
        if (!locked.containsKey(part) && manualAtts.containsKey(part)) {
            manualAtts.remove(part)
        }
    }

    /**
     * Removes a manually set att from the list
     */
    fun removeManualAttIfSpriteNotMatching(part: Char, spriteFile: VirtualFile) {
        if (!locked.containsKey(part) && manualAtts.containsKey(part)) {
            val manual = manualAtts[part]
            if (manual?.spriteFile?.path != spriteFile.path) {
                manualAtts.remove(part)
            }
        }
    }

    /**
     * Get the actual direction that is faced based on other dropdowns
     * Direction is determined by a 3 direction combo box, but there are 4 directions
     *
     * @param bodyPose pose of the body
     * @return true facing direction 0..4 from the pose 0..3
     */
    fun getTrueFacing(bodyPose: Int): Int? {
        return if (bodyPose < 4) {
            0
        } else if (bodyPose < 8) {
            1
        } else if (variant.isOld) {
            when (bodyPose) {
                8 -> 2
                9 -> 3
                else -> {
                    LOGGER.severe("Invalid body pose '$bodyPose' for facing test")
                    null
                }
            }
        } else {
            when {
                (bodyPose < 12) -> 2
                (bodyPose < 16) -> 3
                else -> {
                    LOGGER.severe("Invalid body pose '$bodyPose' for facing test")
                    null
                }
            }
        }
    }


    fun setLocked(locked: Map<Char, BodyPartFiles>) {
        this.mLocked = locked
        val chars = locked.keys
            .stream()
            .map { obj: Char -> obj.toString() }
            .collect(Collectors.joining())
            .toCharArray()
        requestRender(*chars)
    }

    private fun getUpdatedSpriteSet(progressIndicator: ProgressIndicator, vararg parts: Char): CreatureSpriteSet? {
        if (project.isDisposed) {
            return null
        }
        val updatedSprites = SpriteSetUtil.getUpdatedSpriteSet(
            project,
            progressIndicator,
            spriteSet,
            poseEditor.getBreedFiles(PartGroups.HEAD),
            poseEditor.getBreedFiles(PartGroups.BODY),
            poseEditor.getBreedFiles(PartGroups.LEGS),
            poseEditor.getBreedFiles(PartGroups.ARMS),
            poseEditor.getBreedFiles(PartGroups.TAIL),
            poseEditor.getBreedFiles(PartGroups.EARS),
            poseEditor.getBreedFiles(PartGroups.HAIR),
            manualAtts,
            locked,
            *parts
        )
        spriteSet = updatedSprites
        return updatedSprites
    }

    fun hardReload() {
        val progressIndicator = EmptyProgressIndicator()
        try {
            getUpdatedSpriteSet(progressIndicator, *allParts)
            requestRender(*allParts)
        } catch (_: Exception) {}
    }

    fun getBodyPoseActual(bodyDirection: Int, tilt: Int): Int {
        return if (variant.isOld) {
            when (bodyDirection) {
                0 -> 3 - tilt
                1 -> 4 + (3 - tilt)
                2 -> 8
                else -> 9
            }
        } else {
            bodyDirection * 4 + (3 - tilt)
        }
    }


    fun getHeadPoseActual(
        facing: Int,
        headPoseString: String?,
        headPose: Int,
        headDirection2: Int,
        mood: Int,
        eyesClosed: Boolean,
    ): Int {
        val faceIsBack = headPoseString != null && headPoseString.lowercase().startsWith("back")
        val pose = calculateHeadPose(variant,
            if (headPose == 2 && faceIsBack) 3 else facing,
            headPose,
            headDirection2,
            mood,
            eyesClosed
        )
        if (variant.isOld && pose == 8) {
            if (faceIsBack) {
                return pose + 1
            }
        }
        return pose
    }

    fun hasTail(files: List<BodyPartFiles>): Boolean {
        return files.any {
            val part = it.spriteFile.name.lowercase()[0]
            part == 'm' || part == 'n'
        }
    }
}

enum class PartGroups {
    HEAD,
    BODY,
    LEGS,
    ARMS,
    TAIL,
    EARS,
    HAIR
}

@Suppress("unused")
enum class Part(partChar: Char) {
    HEAD('a'),
    Body('b'),
    LEFT_THIGH('c'),
    LEFT_SHIN('d'),
    LEFT_FOOT('e'),
    RIGHT_THIGH('f'),
    RIGHT_SHIN('g'),
    RIGHT_FOOT('h'),
    LEFT_UPPER_ARM('i'),
    LEFT_LOWER_ARM('j'),
    RIGHT_UPPER_ARM('k'),
    RIGHT_LOWER_ARM('l'),
    TAIL_BASE('m'),
    TAIL_TIP('n'),
    LEFT_EAR('o'),
    RIGHT_EAR('p'),
    HAIR('q')
}

private fun AtomicInteger.decrementNotNegative() {
    updateAndGet {
        maxOf(0, it - 1)
    }
}