@file:Suppress("SimplifiableCallChain", "unused")

package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.bedalton.common.structs.Pointer
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesByVariantIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getVariant
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
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val ATT_TO_SPRITE = false

@Suppress("SimplifiableCallChain")
object BodyPartsIndex {

    internal val BODY_DATA_ATT_KEY = Key<Pointer<Pair<Int, VirtualFile>?>?>("creatures.body-part.att-with-index")

    private const val THIS_INDEX_VERSION = 10

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
            searchScope ?: GlobalSearchScope.everythingScope(project),
            coroutineScope,
            progressIndicator
        ) { variantScope ->

            // Make sure indices are not dumb
            if (DumbService.isDumb(project)) {
                return@matchSpritesToAtts emptyList()
            }

            // Get scope filter for all matching virtual files
            val nonNullScope = searchScope ?: GlobalSearchScope.everythingScope(project)


            // Get initial files from breed specific indices by variant
            val initialFiles = if (ATT_TO_SPRITE) {
                AttFilesByVariantIndex.findMatching(project, variant, variantScope)
            } else {
                BreedSpriteIndex.findMatching(project, variant, variantScope, progressIndicator)
            }

            // If files where found, then index is not empty, so return
            if (initialFiles.isNotEmpty()) {
                return@matchSpritesToAtts initialFiles
            }

            val isC3DS = variant.isC3DS

            // Create filter if needed
            val filter = filter@{ virtualFile: VirtualFile ->
                return@filter BreedPartKey.isPartName(virtualFile.nameWithoutExtension)
                        && virtualFile.getVariant(project, true).let {
                            it == variant || (isC3DS && variant.isC3DS)
                }

            }

            // Get extension for source files based
            val extensions = if (ATT_TO_SPRITE) {
                setOf("att")
            } else {
                when (gameVariant) {
                    CaosVariant.C1 -> setOf("spr")
                    CaosVariant.C2 -> setOf("s16")
                    else -> setOf("s16", "c16")
                }
            }

            // Get files from raw file name indices
            getRawWithCaselessIndex(
                project,
                nonNullScope,
                progressIndicator,
                extensions,
                filter
            ).nullIfEmpty()
                ?: getRawWithCaseSensitiveIndex(
                    project,
                    gameVariant,
                    nonNullScope,
                    progressIndicator,
                    extensions
                )
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
            searchScope ?: GlobalSearchScope.everythingScope(project),
            coroutineScope,
            progressIndicator
        ) { newScope ->

            // Make sure file is not dumb
            if (DumbService.isDumb(project)) {
                return@matchSpritesToAtts emptyList()
            }

            // Get absolutely not null scope
            // Used to filter virtual files
            val nonNullScope = newScope ?: GlobalSearchScope.everythingScope(project)


            // Get initial files the sprite/att index's findMatching with breed key
            val initialFiles = if (ATT_TO_SPRITE) {
                AttFilesIndex.findMatching(project, searchKey, nonNullScope, progressIndicator)
            } else {
                BreedSpriteIndex.findMatching(project, searchKey, nonNullScope, progressIndicator)
            }

            // If initial files is not empty, that means indices should be build
            if (initialFiles.isNotEmpty()) {
                return@matchSpritesToAtts initialFiles
            }

            // Create filter by key function
            val filter = filter@{ file: VirtualFile ->
                progressIndicator?.checkCanceled()
                val key = BreedPartKey.fromFileName(file.nameWithoutExtension)
                    ?: return@filter false
                BreedPartKey.isGenericMatch(key, searchKey)
            }

            // Get target file extensions
            val extensions = if (ATT_TO_SPRITE) {
                setOf("att")
            } else {
                when (searchKey.variant) {
                    CaosVariant.C1 -> setOf("spr")
                    CaosVariant.C2 -> setOf("s16")
                    null -> setOf("spr", "s16", "c16")
                    else -> setOf("s16", "c16")
                }
            }

            // Get files first with caseless index, then with case-sensitive index
            getRawWithCaselessIndex(
                project,
                nonNullScope,
                progressIndicator,
                extensions,
                filter
            ).nullIfEmpty()
                ?: getRawWithCaseSensitiveIndex(
                    project,
                    searchKey,
                    nonNullScope,
                    progressIndicator,
                    extensions
                )
        }
    }

    private inline fun getRawWithCaselessIndex(
        project: Project,
        searchScope: GlobalSearchScope,
        progressIndicator: ProgressIndicator?,
        extensions: Set<String>,
        filter: (file: VirtualFile) -> Boolean,
    ): Collection<VirtualFile> {
        return extensions
            .flatMap map@{ extension ->
                progressIndicator?.checkCanceled()
                CaseInsensitiveFileIndex
                    .findWithExtension(project, extension, searchScope)
            }
            .filter(filter)

    }

    private fun getRawWithCaseSensitiveIndex(
        project: Project,
        searchKey: BreedPartKey,
        searchScope: GlobalSearchScope,
        progressIndicator: ProgressIndicator?,
        extensions: Set<String>,
    ): Collection<VirtualFile> {
        val extensionsLowercase = extensions.mapNotNull { it.nullIfEmpty()?.lowercase() }
        return FilenameIndex
            .getAllFilenames(project)
            .filter { fileName ->
                progressIndicator?.checkCanceled()
                val key = BreedPartKey.fromFileName(fileName)
                    ?: return@filter false
                BreedPartKey.isGenericMatch(key, searchKey) &&
                        PathUtil.getExtension(fileName)?.lowercase() in extensionsLowercase
            }
            .flatMap { fileName ->
                FilenameIndex.getVirtualFilesByName(fileName, false, searchScope)
            }

    }

    private fun getRawWithCaseSensitiveIndex(
        project: Project,
        variant: CaosVariant,
        searchScope: GlobalSearchScope,
        progressIndicator: ProgressIndicator?,
        extensions: Set<String>,
    ): Collection<VirtualFile> {
        val extensionsLowercase = extensions.mapNotNull { it.nullIfEmpty()?.lowercase() }
        return FilenameIndex
            .getAllFilenames(project)
            .filter { fileName ->
                progressIndicator?.checkCanceled()
                BreedPartKey.isPartName(fileName) && PathUtil.getExtension(fileName)
                    ?.lowercase() in extensionsLowercase
            }
            .flatMap { fileName ->
                FilenameIndex.getVirtualFilesByName(fileName, false, searchScope)
                    .filter { virtualFile ->
                        virtualFile.getVariant(project, true) == variant
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
        val newScope: GlobalSearchScope? = if (variant != null) {
            val variantScope = CaosVariantGlobalSearchScope(project, variant)
            searchScope?.intersectWith(variantScope) ?: variantScope
        } else {
            searchScope
        }

        // Get the actual sprites
        val baseFiles: Collection<VirtualFile> = try {
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
            matchSpritesToAtts(project, baseFiles, searchScope, variant, progressIndicator)
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
        baseFiles: Collection<VirtualFile>,
        searchScope: GlobalSearchScope? = null,
        variant: CaosVariant?,
        progressIndicator: ProgressIndicator?,
    ): List<BodyPartFiles> {
        // If there are no att files, there is nothing to do
        if (baseFiles.isEmpty()) {
            return emptyList()
        }

        progressIndicator?.checkCanceled()

        // Match atts to sprites
        val out: List<BodyPartFiles> = baseFiles
            .mapAsync map@{ baseFile ->
                if (!baseFile.isValid) {
                    return@map null
                }
                matchOther(project, variant, baseFile, searchScope, progressIndicator)
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
        val scope = if (searchScope != null) {
            generalScope.intersectWith(searchScope)
        } else {
            generalScope
        }

        val validExtensions = if (ATT_TO_SPRITE) {
            variant.validSpriteExtensions
        } else {
            setOf("att")
        }
        val childFiles = baseFile
            .parent
            .children
            .filter { childFile ->
                progressIndicator?.checkCanceled()
                childFile.extension?.lowercase() in validExtensions &&
                        !childFile.isDirectory &&
                        BreedPartKey.isPartName(childFile.name, variant)
            }

        val strict = childFiles.isNotEmpty()

        var matchingFile: VirtualFile? = null

        // Find sprites under parent
        // TODO, find nearest path to parent
        if (strict) {
            matchingFile = childFiles
                .sortedBy {
                    BreedPartKey.fromFileName(it.name, variant)
                        ?.let { aKey ->
                            key.distance(aKey)
                        } ?: Int.MAX_VALUE
                }
                .firstOrNull()
        } else {
//            val parent = baseFile.parent

            val keys = listOf(
                key,
                key.copyWithAgeGroup(null),
                key.copyWithAgeGroup(null).copyWithGender(null),
                key.copyWithBreed(null).copyWithAgeGroup(null).copyWithGender(null)
            )
            for (theKey in keys) {
                progressIndicator?.checkCanceled()
                val otherFiles: Collection<VirtualFile> = readNonBlocking {
                    if (DumbService.isDumb(project)) {
                        return@readNonBlocking emptyList()
                    }
                    if (ATT_TO_SPRITE) {
                        BreedSpriteIndex.findMatching(project, theKey, searchScope, progressIndicator)
                    } else {
                        // Sprite to att
                        AttFilesIndex.findMatching(project, theKey, searchScope, progressIndicator)
                    }
                }
                var matching: List<VirtualFile> = otherFiles
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
                if (matching.isEmpty()) {
                    continue
                }

                matchingFile = readNonBlocking {
//                    nearby(parent, matching, scope)
                    VirtualFileUtil.nearest(baseFile, matching)
                }

                if (matchingFile != null) {
                    break
                }
            }
        }

        // Set result, whether good or bad
        // That way we do not need to keep checking it
        val pointer = Pointer(matchingFile?.let { Pair(VERSION, it) })
        SpriteAttPathPropertyPusher.writeToStorage(baseFile, pointer)
        return if (matchingFile != null) {
            bundle(baseFile, matchingFile)!!
        } else {
            return null
        }
    }

    private fun bundle(baseFile: VirtualFile, matchingFile: VirtualFile?): BodyPartFiles? {
        // If sprite was found, add it to list of body parts
        if (matchingFile != null) {
            return if (ATT_TO_SPRITE) {
                BodyPartFiles(bodyDataFile = baseFile, spriteFile = matchingFile)
            } else {
                BodyPartFiles(spriteFile = baseFile, bodyDataFile = matchingFile)
            }
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

        if (scope != null) {
            var aParent = parent
            while (scope.contains(aParent)) {
                otherFiles.firstOrNull { file -> scope.accept(file) && VfsUtil.isAncestor(aParent, file, false) }?.let {
                    return it
                }
                aParent = aParent.parent
                    ?: return null
            }
            return null
        } else {
            val parents = listOfNotNull(
                parent,
                parent.parent,
                parent.parent?.parent,
                parent.parent?.parent?.parent,
            )
            return parents.firstNotNullOfOrNull { aParent ->
                otherFiles.firstOrNull { file -> VfsUtil.isAncestor(aParent, file, false) }
            }
        }
    }


    private fun restoreCached(baseFile: VirtualFile): BodyPartFiles? {
        val otherPointer = SpriteAttPathPropertyPusher.readFromStorage(baseFile)
            ?: return null
        val pointerValue = otherPointer.value
            ?: return null
        val (indexVersion, otherFile) = pointerValue
        if (indexVersion == VERSION && otherFile.isValid && otherFile.exists()) {
            return if (ATT_TO_SPRITE) {
                BodyPartFiles(bodyDataFile = baseFile, spriteFile = otherFile)
            } else {
                // Sprite to ATT
                BodyPartFiles(spriteFile = baseFile, bodyDataFile = otherFile)
            }
        }
        SpriteAttPathPropertyPusher.writeToStorage(baseFile, null)
        return null
    }


    suspend fun getImmediate(project: Project, directory: VirtualFile, key: BreedPartKey): Map<Char, BodyPartFiles?>? {
        if (DumbService.isDumb(project)) {
            return null
        }
        val thisDirectory = if (directory.isDirectory) {
            directory
        } else {
            directory.parent
        }
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
                            && isAncestor(thisDirectory, thisDirectory.parent, partFiles.spriteFile)
                            && isAncestor(thisDirectory, thisDirectory.parent, partFiles.bodyDataFile)
                }?.sortedBy {
                    val age = it.bodyDataFile.name[2].lowercase() - '0'
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
                return@mapNotNull null
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

    companion object {

        private val fileAttribute by lazy {
            FileAttribute(BodyPartsIndex.BODY_DATA_ATT_KEY.toString())
        }

        fun writeToStorage(baseFile: VirtualFile, otherFile: Pointer<Pair<Int, VirtualFile>?>?) {
//            spriteFile.putUserData(BodyPartsIndex.BODY_DATA_ATT_KEY, attFile)
            writeToStorageStream(
                baseFile,
                fileAttribute,
                BodyPartsIndex.BODY_DATA_ATT_KEY,
                otherFile
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

        fun readFromStorage(baseFile: VirtualFile): Pointer<Pair<Int, VirtualFile>?>? {
//            return spriteFile.getUserData(BodyPartsIndex.BODY_DATA_ATT_KEY)
            return readFromStorageStream(
                baseFile,
                fileAttribute,
                BodyPartsIndex.BODY_DATA_ATT_KEY,
                safe = true
            ) read@{
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
                    val virtualFile = baseFile.fileSystem.findFileByPath(path)
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