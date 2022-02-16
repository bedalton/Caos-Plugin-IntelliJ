@file:Suppress("SimplifiableCallChain", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.indices

import bedalton.creatures.structs.Pointer
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesByVariantIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.validSpriteExtensions
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors


@Suppress("SimplifiableCallChain")
object BodyPartsIndex {

    internal val BODY_DATA_ATT_KEY = Key<Pointer<Pair<Int, VirtualFile>?>?>("creatures.body-part.att-with-index")

    private const val THIS_INDEX_VERSION = 6

    // Compound version to force a reindex if any of the sourced indices change
    const val VERSION = THIS_INDEX_VERSION + AttFilesIndex.VERSION + BreedSpriteIndex.VERSION

    @JvmStatic
    suspend fun variantParts(
        project: Project,
        gameVariant: CaosVariant,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        return variantParts(project, gameVariant, null, progressIndicator = progressIndicator)
    }

    @JvmStatic
    suspend fun variantParts(
        project: Project,
        gameVariant: CaosVariant,
        searchScope: GlobalSearchScope?,
        coroutineScope: CoroutineScope = GlobalScope,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        val variant = if (gameVariant == CaosVariant.DS)
            CaosVariant.C3
        else
            gameVariant

        return matchSpritesToAtts(
            project,
            variant,
            searchScope,
            coroutineScope,
            progressIndicator
        ) { variantScope ->
            if (DumbService.isDumb(project)) {
                emptyList()
            } else {
                AttFilesByVariantIndex.findMatching(project, variant, variantScope)//, progressIndicator)
            }
        }

    }

    @JvmStatic
    suspend fun findWithKey(
        project: Project,
        searchKey: BreedPartKey,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        return findWithKey(project, searchKey, null, progressIndicator = progressIndicator)
    }

    @JvmStatic
    suspend fun findWithKey(
        project: Project,
        searchKey: BreedPartKey,
        searchScope: GlobalSearchScope?,
        coroutineScope: CoroutineScope = GlobalScope,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        progressIndicator?.checkCanceled()
        // Get sprites matched to ATT
        return matchSpritesToAtts(
            project,
            searchKey.variant,
            searchScope,
            coroutineScope,
            progressIndicator
        ) { newScope ->
            if (DumbService.isDumb(project)) {
                emptyList()
            } else {
                AttFilesIndex.findMatching(project, searchKey, newScope, progressIndicator)
            }
        }
    }


    private suspend fun matchSpritesToAtts(
        project: Project,
        variantIn: CaosVariant?,
        searchScope: GlobalSearchScope?,
        coroutineScope: CoroutineScope = GlobalScope,
        progressIndicator: ProgressIndicator?,
        supplier: (variantScope: GlobalSearchScope?) -> Collection<VirtualFile>,
    ): List<BodyPartFiles> {

        val variant = variantIn?.selfOrC3IfDS

        // Build variant scope
        val newScope = if (variant != null) {
            val variantScope = CaosVariantGlobalSearchScope(project, variant)
            searchScope?.intersectWith(variantScope) ?: variantScope
        } else {
            searchScope
        }

        // Get the actual sprites
        val attFiles: Collection<VirtualFile> = try {
            readNonBlocking(coroutineScope) {
                supplier(newScope)
            }
        } catch (e: Exception) {
            if (e !is ProcessCanceledException) {
                LOGGER.severe("Exception encountered in match sprites to atts. Error: ${e.message}")
                e.printStackTrace()
            }
            return emptyList()
        }

        // Match sprites to ATTs
        val files = try {
            matchSpritesToAtts(project, attFiles, searchScope, variant, progressIndicator)
        } catch (e: Exception) {
            if (e !is ProcessCanceledException) {
                LOGGER.severe("variantParts() -> Exception encountered in match sprites to atts. Error: ${e.message}")
            }
            throw e
        }
        return files
    }

    private suspend fun matchSpritesToAtts(
        project: Project,
        attFiles: Collection<VirtualFile>,
        searchScope: GlobalSearchScope? = null,
        variant: CaosVariant?,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        // If there are no att files, there is nothing to do
        if (attFiles.isEmpty()) {
            return emptyList()
        }

        progressIndicator?.checkCanceled()

        // Match atts to sprites
        val out: List<BodyPartFiles> = attFiles
            .mapAsync map@{ spriteFile ->
                if (!spriteFile.isValid) {
                    return@map null
                }
                matchOther(project, variant, spriteFile, searchScope, progressIndicator)
            }.filterNotNull()

        return out.distinctBy { it.spriteFile.path }
    }


    private suspend fun matchOther(
        project: Project,
        variant: CaosVariant?,
        baseFile: VirtualFile,
        searchScope: GlobalSearchScope?,
        progressIndicator: ProgressIndicator?,
    ): BodyPartFiles? {
        progressIndicator?.checkCanceled()
        // Get the sprite search key
        val key = BreedPartKey.fromFileName(baseFile.name, variant)
            ?: return null


        // If part has been matched, even if it is empty
        // Stop after checking or using
        val bodyPartFile = restoreCached(baseFile)
        if (bodyPartFile != null) {
            return bodyPartFile
        }


        val generalScope = baseFile
            .getModule(project)
            ?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)


//            val scope = generalScope
        val scope = if (searchScope != null)
            generalScope.intersectWith(searchScope)
        else
            generalScope

        val childAtts = baseFile
            .parent
            .children
            .filter { childFile ->
                progressIndicator?.checkCanceled()
                childFile.extension?.lowercase() in variant.validSpriteExtensions &&
                        !childFile.isDirectory &&
                        BreedPartKey.isPartName(childFile.name, variant)
            }

        val strict = childAtts.isNotEmpty()

        var matchingSprite: VirtualFile? = null

        // Find sprites under parent
        // TODO, find nearest path to parent
        if (strict) {
            matchingSprite = childAtts
                .sortedBy {
                    BreedPartKey.fromFileName(it.name, variant)
                        ?.let { aKey ->
                            key.distance(aKey)
                        } ?: Int.MAX_VALUE
                }
                .firstOrNull()
        } else {
            val parent = baseFile.parent

            val keys = listOf(
                key,
                key.copyWithBreed(null).copyWithAgeGroup(null).copyWithGender(null)
            )
            for (theKey in keys) {
                progressIndicator?.checkCanceled()
                val sprites: Collection<VirtualFile> = readNonBlocking {
                    if (DumbService.isDumb(project)) {
                        return@readNonBlocking emptyList()
                    }
                    BreedSpriteIndex.findMatching(project, theKey, searchScope, progressIndicator)
                }
                var matching: List<VirtualFile> = sprites
                    .breedFileSort(
                        variant,
                        key,
                        baseFile.parent
                    )
                matching = readNonBlocking {
                    matching.filter {
                        scope.accept(it)
                    }
                }
                if (matching.isEmpty())
                    continue

                matchingSprite = readNonBlocking {
                    nearby(parent, matching, scope)
                }
                if (matchingSprite != null) {
                    break
                }
            }
        }

        // Set result, whether good or bad
        // That way we do not need to keep checking it
        val pointer = Pointer(matchingSprite?.let { Pair(VERSION, it) })
        SpriteAttPathPropertyPusher.writeToStorage(baseFile, pointer)
        return if (matchingSprite != null) {
            bundle(baseFile, matchingSprite)!!
        } else {
            return null
        }
    }

    private fun bundle(attFile: VirtualFile, matchingSprite: VirtualFile?): BodyPartFiles? {
        // If sprite was found, add it to list of body parts
        if (matchingSprite != null) {
            return BodyPartFiles(spriteFile = matchingSprite, bodyDataFile = attFile)
        }
        return null
    }

    /**
     * @param otherFiles Triple is first = VirtualFile; second = breed key distance; path distance = Pair(pathUp, pathDown)
     */
    private fun nearby(
        parent: VirtualFile,
        otherFiles: Collection<VirtualFile>,
        scope: GlobalSearchScope?,
    ): VirtualFile? {
        val topParent = parent.parent?.parent?.parent ?: parent.parent?.parent ?: parent.parent ?: parent
        return if (scope != null) {
            otherFiles.firstOrNull { file -> scope.accept(file) && VfsUtil.isAncestor(topParent, file, false) }
        } else {
            otherFiles.firstOrNull { file -> VfsUtil.isAncestor(topParent, file, false) }
        }
    }


    private fun restoreCached(attFile: VirtualFile): BodyPartFiles? {
        val attPointer = SpriteAttPathPropertyPusher.readFromStorage(attFile)
            ?: return null
        val pointerValue = attPointer.value
            ?: return null
        val (indexVersion, spriteFile) = pointerValue
        if (indexVersion == VERSION && spriteFile.isValid && spriteFile.exists()) {
            return BodyPartFiles(spriteFile = spriteFile, bodyDataFile = attFile)
        }
        SpriteAttPathPropertyPusher.writeToStorage(attFile, null)
        return null
    }


    suspend fun getImmediate(project: Project, directory: VirtualFile, key: BreedPartKey): Map<Char, BodyPartFiles?>? {
        if (DumbService.isDumb(project)) {
            return null
        }
        val parent = directory.parent
        return ('a'..'q').mapNotNull { part ->
            val bodyPartKey = key
                .copyWithPart(part)

            val breedPartFiles = try {
                findWithKey(project, bodyPartKey, null)
                    .nullIfEmpty()
            } catch (e: ProcessCanceledException) {
                e.printStackTrace()
                return runBlocking {
                    getImmediate(project, directory, key)
                }
            }
            val testKey = bodyPartKey.copyWithAgeGroup(null)

            if (breedPartFiles?.any { !BreedPartKey.isGenericMatch(testKey, it.key) } == true) {
                LOGGER.severe("Breed part does not actually match key: ${testKey.code} in ${directory.path}")
                throw Exception("Breed part does not actually match key")
            }


            val bodyPart = breedPartFiles
                ?.filter first@{ partFiles ->
                    partFiles.spriteFile.isValid
                            && partFiles.bodyDataFile.isValid
                            && partFiles.key!!.part == part
                            && isAncestor(directory, parent, partFiles.spriteFile)
                            && isAncestor(directory, parent, partFiles.bodyDataFile)
                }?.sortedBy {
                    val age = it.bodyDataFile.name[2].lowercase() - 'a'
                    if (bodyPartKey.ageGroup != null) {
                        if (age < bodyPartKey.ageGroup) {
                            bodyPartKey.ageGroup - age
                        } else {
                            age
                        }
                    } else {
                        0
                    }
                }
                ?.firstOrNull()

            if (bodyPart == null && part in 'a'..'l') {
                return null
            }
            part to bodyPart
        }.toMap()
    }

    private fun isAncestor(parent: VirtualFile, parentParent: VirtualFile?, child: VirtualFile): Boolean {
        return VfsUtil.isAncestor(parent, child, false)
                || parentParent != null && VfsUtil.isAncestor(parentParent, child, false)
    }

}


val NULL_FILE_POINTER_PAIR: Pointer<Pair<Int, VirtualFile>?> = Pointer(Pair(-1, NULL_PLACEHOLDER_FILE as VirtualFile))

internal open class SpriteAttPathPropertyPusher
    : FilePropertyPusher<Pointer<Pair<Int, VirtualFile>?>?> {

    override fun getDefaultValue(): Pointer<Pair<Int, VirtualFile>?> = NULL_FILE_POINTER_PAIR

    override fun getFileDataKey(): Key<Pointer<Pair<Int, VirtualFile>?>?> = BodyPartsIndex.BODY_DATA_ATT_KEY

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): Pointer<Pair<Int, VirtualFile>?>? {
        if (file == null)
            return null
        val out = file.getUserData(BodyPartsIndex.BODY_DATA_ATT_KEY)
            ?: readFromStorage(file)
            ?: return null
        val value = out.value
            ?: return out
        return if (value.first < 1 || value.second == NULL_FILE_POINTER_PAIR.value)
            null
        else
            out
    }

    override fun getImmediateValue(module: Module): Pointer<Pair<Int, VirtualFile>?>? {
        return null
    }

    override fun persistAttribute(
        project: Project,
        spriteFile: VirtualFile,
        attFile: Pointer<Pair<Int, VirtualFile>?>,
    ) {
        writeToStorage(spriteFile, attFile)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file.extension in SpriteParser.VALID_SPRITE_EXTENSIONS
    }

    companion object {

        private val fileAttribute by lazy {
            FileAttribute(BodyPartsIndex.BODY_DATA_ATT_KEY.toString())
        }

        fun writeToStorage(spriteFile: VirtualFile, attFile: Pointer<Pair<Int, VirtualFile>?>?) {
//            spriteFile.putUserData(BodyPartsIndex.BODY_DATA_ATT_KEY, attFile)
            writeToStorageStream(spriteFile,
                fileAttribute,
                BodyPartsIndex.BODY_DATA_ATT_KEY,
                attFile
            ) write@{ pointer ->
                writeBoolean(pointer != null)
                if (pointer == null) {
                    return@write Unit
                }
                val value = pointer.value
                writeBoolean(value != null)
                if (value == null) {
                    return@write Unit
                }
                writeInt(value.first)
                writeString(value.second.path)
            }
        }

        fun readFromStorage(spriteFile: VirtualFile): Pointer<Pair<Int, VirtualFile>?>? {
//            return spriteFile.getUserData(BodyPartsIndex.BODY_DATA_ATT_KEY)
            return readFromStorageStream(spriteFile,
                fileAttribute,
                BodyPartsIndex.BODY_DATA_ATT_KEY,
                safe = true) read@{
                if (!readBoolean()) {
                    return@read null
                }
                if (!readBoolean()) {
                    Pointer(null)
                }
                val version = readInt()
                val path = readString()
                if (version < 1) {
                    return@read Pointer(null)
                }
                if (path == null || path == NULL_PLACEHOLDER_FILE.path) {
                    null
                } else if (path.isEmpty()) {
                    Pointer(null)
                } else {
                    val virtualFile = spriteFile.fileSystem.findFileByPath(path)
                    if (virtualFile == null) {
                        null
                    } else {
                        Pointer(Pair(version, virtualFile))
                    }
                }
            }
        }
    }

}


internal object BreedFileUtil {

    /**
     * Sort list of virtual files according to key and distance to parent file
     */
    fun sort(
        variant: CaosVariant?,
        key: BreedPartKey,
        parentPath: VirtualFile? = null,
        matching: Iterable<VirtualFile>,
    ): List<VirtualFile> {
        return sort(
            variant,
            key,
            parentPath,
            matching
        ) {
            it
        }
    }

    /**
     * Sort list of breed objects containing a virtual file according to key and distance to parent file
     */
    internal inline fun <T> sort(
        variant: CaosVariant?,
        key: BreedPartKey,
        parentPath: VirtualFile? = null,
        matching: Iterable<T>,
        get: (T) -> VirtualFile?,
    ): List<T> {
        return matching
            .mapNotNull map@{ item: T ->
                val file = get(item)
                    ?: return@map null
                val distance: Pair<Int, Pair<Int, Int>> =
                    getDistanceScore(variant, parentPath ?: file, file, key)
                        ?: return@map null
                Triple(item, distance.first, distance.second)
            }
            .sortedWith sorted@{ a, b ->
                var offset = a.second - b.second
                if (offset != 0) {
                    return@sorted offset
                }
                offset = a.third.first - b.third.first
                if (offset != 0) {
                    return@sorted offset
                }
                a.third.second - b.third.second
            }
            .map { it.first }
    }

    /**
     * Returns three values with which to sort objects containing a breed virtual file
     * Triple->second = breed key distance (main indicator)
     * Triple->third->first = Distance up from child to the base file (second sort key)
     * Triple->third->second = Distance down from the shared ancestor base file (third sort key)
     */
    private fun getDistanceScore(
        variant: CaosVariant?,
        baseFile: VirtualFile,
        breedFile: VirtualFile,
        key: BreedPartKey,
    ): Pair<Int, Pair<Int, Int>>? {
//        val sharedAncestor = VfsUtil.getCommonAncestor(baseFile, breedFile)
//            ?: return null
        val relativePath = VfsUtil.findRelativePath(baseFile, breedFile, '/')
            ?: return null

        val pathUp = relativePath.count("../")
        val lastIndex = relativePath.lastIndexOf("../")

        // Find distance down, having counted distance up
        val pathDown = when {
            lastIndex == relativePath.lastIndex -> 0
            lastIndex < 0 -> 0
            else -> relativePath.substring(lastIndex).count { it == '/' }
        }
        val pathDistance = Pair(pathUp, pathDown)
        val breedKeyDistance = key.distance(BreedPartKey.fromFileName(breedFile.name, variant))
            ?: Int.MAX_VALUE
        return Pair(breedKeyDistance, pathDistance)
    }
}

/**
 * Sort a list of objects with a breed virtual file by nearness to breed key then distance to base file
 */
internal inline fun <T> Iterable<T>.breedFileSort(
    variant: CaosVariant?,
    key: BreedPartKey,
    baseFile: VirtualFile? = null,
    get: (T) -> VirtualFile?,
): List<T> {
    return BreedFileUtil.sort(variant, key, baseFile, this, get)
}

/**
 * Sort a list of virtual files by nearness to breed key then distance to base file
 */
internal fun Iterable<VirtualFile>.breedFileSort(
    variant: CaosVariant?,
    key: BreedPartKey,
    baseFile: VirtualFile? = null,
): List<VirtualFile> {
    return BreedFileUtil.sort(variant, key, baseFile, this)
}

private val nonBlockingExecutor by lazy { Executors.newCachedThreadPool() }

internal suspend fun <T> readNonBlocking(
    coroutineScope: CoroutineScope = GlobalScope,
    task: () -> T,
): T {
    return withContext(coroutineScope.coroutineContext) {
        runReadAction(task)
    }
}
//
//fun <T> readNonBlocking(
//    project: Project,
//    task: () -> T,
//): T {
//    return ReadAction
//        .nonBlocking(Callable {
//            task()
//        })
//        .inSmartMode(project)
//        .executeSynchronously()
//}