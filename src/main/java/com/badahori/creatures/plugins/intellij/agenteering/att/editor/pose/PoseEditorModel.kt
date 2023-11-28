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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
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
import kotlin.collections.set

class PoseEditorModel(
    val project: Project,
    var variant: CaosVariant,
    private val poseEditor: BreedPoseHolder,
) {


    private val allValidPoseChars = "012345xX?!".toCharArray()
    private val validPartPoseChars = "0123xX?!".toCharArray()

    var progressIndicator: ProgressIndicator? = null
    private var spriteSet: CreatureSpriteSet? = null
    private var mLocked: Map<Char, BodyPartFiles> = emptyMap()
    private var settingImmediate = AtomicInteger(0)
    private var updating = AtomicInteger(0)
    private var mRendering: Long? = null
    private var renderId = AtomicInteger(0)
    val rendering: Boolean get() = mRendering?.let { it < now } ?: false

    @Suppress("MemberVisibilityCanBePrivate")
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
            settingImmediate.set(10000)
            return
        }
        if (settingImmediate.getAndIncrement() > 0) {
            settingImmediate.decrementNotNegative()
            return
        }
        GlobalScope.launch(Dispatchers.IO) launch@{
            if (project.isDisposed) {
                updating.decrementNotNegative()
                Exception().printStackTrace()
                return@launch
            }

            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart {
                    executeOnPooledThread {
                        setImmediate(directory, key)
                    }
                }
                return@launch
            }
            val mappedParts = BodyPartsIndex.getImmediate(project, directory, key)

            if (mappedParts == null) {
                settingImmediate.set(0)
                executeOnPooledThread(this@PoseEditorModel::updateFiles)
                return@launch
            }

            val sprites = try {
                CreatureSpriteSet(
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
            } catch (e: NullPointerException) {
                null
            }

            spriteSet = sprites
            poseEditor.setFiles(mappedParts.values.filterNotNull().filter{ it.spriteFile.isValid && it.bodyDataFile.isValid })
            @Suppress("SpellCheckingInspection")
            requestRender(*("abcdefghijklmnopq").toCharArray(), breedChanged = true)
            settingImmediate.set(0)
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
                if (files.isNotEmpty()) {
                    poseEditor.setFiles(files.filter { it.spriteFile.isValid && it.bodyDataFile.isValid })
                }
                updating.set(0)
            } catch (e: ProcessCanceledException) {
                updating.set(0)
                delay(20)
                updateFiles()
            }
        }
    }

    fun requestRender(vararg parts: Char, breedChanged: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            progressIndicator?.cancel()
            val indicator = EmptyProgressIndicator()
            progressIndicator = indicator
            try {
                indicator.start()
                requestRenderAsync(indicator, renderId.incrementAndGet(), *parts, breedChanged = breedChanged)
            } catch (_: ProcessCanceledException) {
            }
        }
    }

    private fun requestRenderAsync(
        progressIndicator: ProgressIndicator,
        id: Int,
        vararg parts: Char,
        breedChanged: Boolean
    ): Boolean {

        if (project.isDisposed) {
            return false
        }

        if (DumbService.isDumb(project)) {
            progressIndicator.checkCanceled()
            DumbService.getInstance(project).runWhenSmart {
                if (id < renderId.get()) {
                    return@runWhenSmart
                }
                try {
                    // Ignore process cancelled from dumb service as the exception
                    // seemed to actually be uncaught
                    requestRender(*parts, breedChanged = breedChanged)
                } catch (_: Exception) {}
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
                progressIndicator.cancel()
                null
            } else {
                val newParts: CharArray = if (breedChanged) parts else charArrayOf()
                getUpdatedSpriteSet(progressIndicator, *newParts)
                    ?: getUpdatedSpriteSet(progressIndicator, *allParts)
            }
        } catch (e: java.lang.Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            progressIndicator.cancel()
            mRendering = null
            LOGGER.severe("Failed to locate required sprites Error:(" + e.javaClass.simpleName + ") " + e.localizedMessage)
            e.printStackTrace()
            poseEditor.setRendered(null)
            return false
        }
        if (updatedSprites == null) {
            progressIndicator.cancel()
            mRendering = null
            return false
        }
        progressIndicator.checkCanceled()
        val updatedPose: Pose = invokeAndWait(ModalityState.defaultModalityState()) {
            try {
                if (id < renderId.get()) {
                    progressIndicator.cancel()
                    null
                } else {
                    poseEditor.getPose(progressIndicator)
                }
            } catch (e: ProcessCanceledException) {
                progressIndicator.cancel()
                null
            }
        } ?: return false

        progressIndicator.checkCanceled()

        val visibilityMask = poseEditor.getVisibilityMask() ?: mapOf()

        progressIndicator.checkCanceled()

        return try {
            if (id < renderId.get()) {
                progressIndicator.cancel()
                false
            } else {
                val image = render(variant, updatedSprites, updatedPose, visibilityMask, poseEditor.zoom)
//                progressIndicator.checkCanceled()
                invokeLater{
//                    progressIndicator.checkCanceled()
                    poseEditor.setRendered(image)
                }
                true
            }
        } catch (e: java.lang.Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            LOGGER.severe("Failed to render pose. Error:(" + e.javaClass.simpleName + ") " + e.localizedMessage)
            e.printStackTrace()
            progressIndicator.checkCanceled()
            if (id == renderId.get()) {
                poseEditor.setRendered(null)
            }
            false
        } finally {
            progressIndicator.cancel()
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
            val invalidValue = poseStringPadded[i] !in (if (i < 3) {
                allValidPoseChars
            } else {
                validPartPoseChars
            })
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
        manualAtts[part.lowercaseChar()] = BodyPartFiles(spriteFile!!, file!!)
        this.spriteSet = spriteSet.replacing(part, att)
        requestRender(part, breedChanged = false)
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
        requestRender(*chars, breedChanged = false)
    }

    private fun getUpdatedSpriteSet(progressIndicator: ProgressIndicator, vararg parts: Char): CreatureSpriteSet? {
        if (project.isDisposed) {
            LOGGER.info("Project is disposed on getUpdatedSpriteSet")
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
            spriteSet = null
            getUpdatedSpriteSet(progressIndicator, *allParts)
            requestRender(*allParts, breedChanged = true)
        } catch (_: Exception) {
        }
    }

    fun getBodyPoseActual(bodyDirection: Int, tilt: Int): Int {
        return if (variant.isOld) {
            when (bodyDirection) {
                0 -> tilt
                1 -> 4 + tilt
                2 -> 8
                else -> 9
            }
        } else {
            bodyDirection * 4 + tilt
        }
    }


    fun getHeadPoseActual(
        facing: Int,
        headPoseString: String?,
        headDirection: Int,
        tilt: Int,
        mood: Int,
        eyesClosed: Boolean,
    ): Int {

        val faceIsBack = facing == 3 || (headPoseString != null && headPoseString.lowercase().startsWith("back"))

        if (facing < 0 || headDirection < 0 || tilt < 0 || mood < 0) {
            // UI is not fully initialized
            return 0
        }

        return calculateHeadPose(
            variant,
            if (headDirection == 2 && faceIsBack) 3 else facing,
            headDirection,
            tilt,
            mood,
            eyesClosed
        )
    }

    fun hasTail(files: List<BodyPartFiles>): Boolean {
        return files.any {
            val part = it.spriteFile.name.lowercase()[0]
            part == 'm' || part == 'n'
        }
    }

    companion object {

        @JvmStatic
        @Suppress("SpellCheckingInspection")
        val allPartsChars: List<Char> by lazy {
            "abcdefghijklmnopq".map { it }
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