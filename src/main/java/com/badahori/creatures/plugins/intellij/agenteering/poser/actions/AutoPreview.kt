package com.badahori.creatures.plugins.intellij.agenteering.poser.actions

import bedalton.creatures.sprite.util.SpriteType
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.Pose
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartsIndex
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

class AutoPreview : AnAction() {

    val strict: Boolean = true
    private val defaultZoom = 2

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project == null || !project.settings.state.isAutoPoseEnabled) {
            e.presentation.isVisible = false
            return
        }

        e.presentation.isVisible =
            e.files.any { headSpriteRegex.matches(it.name) } || e.files.filter { it.isDirectory }.any {
                VirtualFileUtil.childrenWithExtensions(it, true, "spr", "s16", "c16")
                    .any { child -> headSpriteRegex.matches(child.name) }
            }
    }

    private fun getZoom(project: Project, defaultZoom: Int): Int {
        return FilenameIndex.getFilesByName(project, "caos.cfg", GlobalSearchScope.everythingScope(project))
            .flatMap { file ->
                file.text
                    .trim()
                    .split("(\r?\n)+".toRegex())
                    .map { line ->
                        line.split('=')
                            .let { parts ->
                                Pair(parts.first(), parts.getOrNull(1) ?: "")
                            }
                    }
            }
            .firstNotNullOfOrNull { property ->
                if (property.first.lowercase() == "autopreview.zoom") {
                    property.second.toIntOrNull()
                } else {
                    null
                }
            } ?: defaultZoom
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
            ?: throw Exception("No project available to generate preview")

        if (project.isDisposed) {
            return
        }

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart {
                if (project.isDisposed) {
                    return@runWhenSmart
                }
                actionPerformed(e)
            }
            return
        }

        val headFiles = (e.files.filter {
            headSpriteRegex.matches(it.name)
        } + e.files.filter { it.isDirectory }.flatMap {
            VirtualFileUtil.childrenWithExtensions(it, true, "spr", "s16", "c16")
                .filter { child -> headSpriteRegex.matches(child.name) }
        }).distinctBy { it.path }

        val projectFile = project.projectFile?.let {
            VfsUtil.virtualToIoFile(it)
        }
        val zoom = getZoom(project, defaultZoom)
        GlobalScope.launch {
            renderPreviews(project, projectFile, headFiles, zoom)
        }

    }

    private suspend fun renderPreviews(project: Project, projectFile: File?, headFiles: List<VirtualFile>, zoom: Int) {
        val renderedFiles: List<File> = headFiles
            .map { headFile ->
                GlobalScope.async {
                    renderFromHeadSprite(project, projectFile, headFile, zoom)
                }
            }
            .map { it.await() }
            .filterNotNull()
        for (rendered in renderedFiles) {
            ProgressIndicatorProvider.checkCanceled()
            try {
                // Simply refreshes the files to ensure that they show up in project view
                VfsUtil.findFileByIoFile(rendered, true)
                    ?: throw Exception("Failed to find virtual file after render at ${rendered.path}")
            } catch (e: Exception) {
                invokeLater {
                    try {
                        val errorFile = File(projectFile ?: rendered.parentFile, "BreedPreviewError.txt")
                        if (!errorFile.exists())
                            errorFile.createNewFile()
                        errorFile.appendText("\nError: ${e.message} in ${rendered.path}")
                    } catch (e2: Exception) {
                        CaosNotifications.showError(
                            project,
                            "Breed Preview Error",
                            "Save file failed with error '${e.message}' for file at ${rendered.path}"
                        )
                    }
                    LOGGER.severe("RefreshError: ${e.message} in ${rendered.path}")
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun renderFromHeadSprite(project: Project, projectFile: File?, headFile: VirtualFile, zoom: Int): File? {
        if (!headFile.isValid || project.isDisposed) {
            return null
        }
        val variant = getInitialVariant(project, headFile).nullIfUnknown()
        if (variant == null) {
            invokeLater {
                CaosNotifications.showError(
                    project,
                    "Breed Preview Error",
                    "Could not generate breed preview for ${headFile.path}; Game variant could not be determined"
                )
            }
            return null
        }
        val key = BreedPartKey.fromFileName(headFile.name, variant)
            .apply {
                if (this == null) {
                    invokeLater {
                        CaosNotifications.showError(
                            project,
                            "Breed Preview Error",
                            "Failed to parse breed name from filename '${headFile.name}'"
                        )
                    }
                }
            }
            ?: return null
        try {
            return render(variant, project, headFile.parent, key, zoom).apply {
                if (this == null) {
                    invokeLater {
                        CaosNotifications.showError(
                            project,
                            "Breed Preview Error",
                            "Failed to render image for Breed $key"
                        )
                    }
                }
            }
        } catch (e: Exception) {

            invokeLater {
                try {
                    val errorFile =
                        File(projectFile ?: VfsUtil.virtualToIoFile(headFile.parent), "BreedPreviewError.txt")
                    if (!errorFile.exists())
                        errorFile.createNewFile()
                    errorFile.appendText("\nRender failed with error [${key.code}]: ${e.message} in ${headFile.parent.path}")
                } catch (e2: Exception) {
                    CaosNotifications.showError(
                        project,
                        "Breed Preview Error",
                        "Render failed with error [${key.code}]: ${e.message} in ${headFile.parent.path}"
                    )
                }
                LOGGER.severe("Failed to render ${key.code} in ${headFile.parent.path}")
                e.printStackTrace()
            }
            return null
        }
    }

    private fun getPose(variant: CaosVariant, key: BreedPartKey): Pose {
//        return Pose.fromString(variant, 1, null, "141111111111111").second
//        return Pose.fromString(variant, 3, null, "140000000000000").second
        val straight = if (variant.isOld)
            "140000000000000"
        else
            "113122122111111111"
        val seed = Random.nextInt(0, 300)
        val youth = if (variant.isOld) 2 else 3
        return when (key.gender) {
            0 -> {
                if (key.ageGroup?.let { it < youth } == true) {
                    if (seed < 100) {
                        Pose.fromString(variant, 2, null, "223322111013311").second
                    } else if (seed < 200) {
                        Pose.fromString(variant, 2, null, "213322100111211").second
//                    } else if (seed < 300) {
//                        Pose.fromString(variant, 2, null, "142233022022333000").second
                    } else {
                        return Pose.fromString(variant, 1, null, "242010222032211000").second
                    }
                } else {
                    if (seed > 300) {
                        return Pose.fromString(variant, 1, null, "142230322333333000").second
                    }
                    return Pose.fromString(variant, 1, null, straight).second
                }
            }
            1 -> {
                if (key.ageGroup?.let { it < youth } == true) {
                    if (seed < 100) {
                        Pose.fromString(variant, 3, null, "343322100210311000").second
                    } else if (seed < 200) {
                        Pose.fromString(variant, 3, null, "312010222220011").second
//                    } else if (seed < 300) {
//                        Pose.fromString(variant, 3, null, "141222022102211000").second
                    } else {
                        return Pose.fromString(variant, 1, null, "333020332330011").second
                    }
                } else {
//                    if (seed > 300) {
//                        return Pose.fromString(variant, 1, null, "142313222330033000").second
//                    }
                    return Pose.fromString(variant, 1, null, straight).second
                }
            }
            else -> {
                return Pose.fromString(variant, 1, null, straight).second
            }
        }
    }


    private suspend fun render(
        variantIn: CaosVariant?,
        project: Project,
        directory: VirtualFile,
        key: BreedPartKey,
        zoom: Int
    ): File? {

        if (project.isDisposed || !directory.isValid || !directory.exists()) {
            return null
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart {
                GlobalScope.launch {
                    try {
                        render(variantIn, project, directory, key, zoom)
                    } catch (e: Exception) {
                        CaosNotifications.showError(project,
                            "Breed Preview Error",
                            "Render failed for ${key.code} in ${directory.path}. ${e::className}: ${e.message}")
                    }
                }
            }
            return null
        }
        val mappedParts = ('a'..'q').map { part ->
            val bodyPartKey = key
                .copyWithPart(part)

            val breedPartFiles = BodyPartsIndex
                .findWithKey(project, bodyPartKey, null)
                .nullIfEmpty()
            val testKey = bodyPartKey.copyWithAgeGroup(null)

            if (breedPartFiles?.any { !BreedPartKey.isGenericMatch(testKey, it.key) } == true) {
                LOGGER.severe("Breed part does not actually match key: ${testKey.code} in ${directory.path}")
                throw Exception("Breed part does not actually match key")
            }
            val bodyPart = breedPartFiles?.firstOrNull first@{ partFiles ->
                partFiles.spriteFile.isValid
                        && partFiles.bodyDataFile.isValid
                        && partFiles.key!!.part == part
                        && VfsUtil.isAncestor(directory, partFiles.spriteFile,false)
                        && VfsUtil.isAncestor(directory, partFiles.bodyDataFile, false)
            }

            if (bodyPart == null && part in 'a'..'l') {
                return null
            }
            part to bodyPart?.data(project)
        }.toMap()
        val variant = variantIn
            ?: if (mappedParts['a']!!.bodyData.lines.size > 10)
                CaosVariant.C3
            else if (mappedParts['n'] != null || mappedParts.values.any { it?.sprite?.type == SpriteType.S16})
                CaosVariant.C2
            else
                CaosVariant.C1

        val sprites = PoseRenderer.CreatureSpriteSet(
            head = mappedParts['a']!!,
            body = mappedParts['b']!!,
            leftThigh = mappedParts['c']!!,
            leftShin = mappedParts['d']!!,
            leftFoot = mappedParts['e']!!,
            rightThigh = mappedParts['f']!!,
            rightShin = mappedParts['g']!!,
            rightFoot = mappedParts['h']!!,
            leftUpperArm = mappedParts['i']!!,
            leftForearm = mappedParts['j']!!,
            rightUpperArm = mappedParts['k']!!,
            rightForearm = mappedParts['l']!!,
            tailBase = mappedParts['m'],
            tailTip = mappedParts['n'],
            leftEar = mappedParts['o'],
            rightEar = mappedParts['p'],
            hair = mappedParts['q']
        )


        val rendered = PoseRenderer.render(
            variant,
            sprites,
            getPose(variant, key),
            emptyMap(),
            zoom
        )

        val hasSprites = directory.children.any {
            it.extension likeAny listOf(
                "spr",
                "s16",
                "c16"
            )
        }
        val outputDirectory = if (hasSprites) {
            directory
        } else {
            (directory.parent ?: directory)
        }
        val parentFile = VfsUtil.virtualToIoFile(outputDirectory)
        val fileName = "${formatKey(variant, key.copyWithVariant(variant))}.png"
        val file = File(parentFile, fileName)
        return withContext(Dispatchers.IO) {
            if (!ImageIO.write(rendered, "PNG", file)) {
                throw Exception("Failed to write rendered image to ${file.path}")
            }
            file
        }
    }

    companion object {
        val headSpriteRegex = "[Aa][0-8][0-7][a-zA-Z0-9]\\.(spr|s16|c16)".toRegex(RegexOption.IGNORE_CASE)

        fun formatKey(variant: CaosVariant, key: BreedPartKey): String {
            val genus = when (key.genus) {
                0 -> "N"
                1 -> "G"
                2 -> "E"
                3 -> "S"
                else -> "X"
            }
            val gender = when (key.gender) {
                0 -> "- Male"
                1 -> "- Female"
                else -> ""
            }
            val age = when (key.ageGroup) {
                -1 -> "- Embryo"
                0 -> "- Baby"
                1 -> "- Child"
                2 -> "- Adolescent"
                3 -> "- Youth"
                4 -> "- Adult"
                5 -> "- Old"
                6 -> "- Senile"
                else -> ""
            }
            return "Breed ${variant.code}${genus}${key.breed!!.uppercase()} $gender $age".trim()
        }
    }

}