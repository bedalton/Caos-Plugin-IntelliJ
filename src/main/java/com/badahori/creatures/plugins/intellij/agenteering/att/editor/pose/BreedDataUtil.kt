package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import bedalton.creatures.util.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.breedFileSort
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.util.logging.Logger

object BreedDataUtil {

    private val LOGGER = Logger.getLogger("#BreedDataUtil")

    /**
     * Whether to prioritize breed matches when searching for matches
     * Or breeds inside the parent path
     */
    private const val PRIORITIZE_BREED = true

    /**
     * Finds breeds for a given part
     *
     * @param files     all available breed files for any part
     * @param partChars parts to filter the breed list for
     * @return breed body files available for the parts
     */
    @JvmStatic
    fun findBreeds(
        files: List<BodyPartFiles>,
        baseBreed: BreedPartKey,
        partChars: Array<Char>,
    ): MutableList<Triple<String, BreedPartKey, List<BodyPartFiles>>?> {
        val filtered = files.filter { part: BodyPartFiles ->
            val thisKey = part.key
                ?: return@filter false
            thisKey.part?.lowercase() in partChars
        }
        val roots = filtered
            .mapNotNull {
                it.spriteFile.parent?.path
            }
            .distinct()
        val keys = filtered.mapNotNull { it.key?.copyWithPart(null) }
            .filter { baseBreed.ageGroup == null || it.ageGroup == baseBreed.ageGroup }
            .distinct()
        val filesByRoot: List<Pair<String, List<Pair<BreedPartKey, List<BodyPartFiles>>>>> = roots
            .map { root: String ->
                root to filtered.filter {
                    it.spriteFile.parent?.path == root
                }
            }
            .mapNotNull { (root: String, files: List<BodyPartFiles>) ->
                // Find parts in directory and pair them with their key
                keys
                    .mapNotNull { aKey ->
                        val matches = files.filter { BreedPartKey.isGenericMatch(aKey, it.key) }.toMutableList()
                        var foundParts = matches.mapNotNull { file -> file.key?.part }
                        val missing = partChars.filter { it !in foundParts }

                        matches += missing.mapNotNull { char ->
                            val exactPartKey = aKey.copyWithPart(char)
                            val base = files.firstOrNull { file -> exactPartKey == file.key }
                            if (base != null) {
                                base
                            } else {
                                val fudgedPartKey = aKey.copy(
                                    part = char,
                                    ageGroup = null
                                )
                                files
                                    .filter { file ->
                                        BreedPartKey.isGenericMatch(fudgedPartKey, file.key)
                                    }.minByOrNull { file ->
                                        aKey.distance(file.key) ?: Int.MAX_VALUE
                                    }
                            }
                        }
                        foundParts = matches.mapNotNull { file -> file.key?.part }.distinct()
                        if (partChars.any { it !in foundParts }) {
                            null
                        } else {
                            aKey to matches
                        }
                    }
                    .nullIfEmpty()
                    ?.let {
                        root to it
                    }
            }

        val out: List<Triple<String, BreedPartKey, List<BodyPartFiles>>> = filesByRoot
            //List<Pair<String, List<Pair<BreedPartKey, List<BodyPartFiles>>>>>
            .flatMap { (root: String, items: List<Pair<BreedPartKey, List<BodyPartFiles>>>) ->
                // Filter items in root to only those containing all parts for a given key in a folder
                items
                    .filter { (aKey: BreedPartKey, files: List<BodyPartFiles>) ->
                        partChars.all { char ->
                            val partKey = aKey.copyWithPart(char)
                            files.any { it.key == partKey }
                        }
                    }
                    .let { theseItems: List<Pair<BreedPartKey, List<BodyPartFiles>>> ->
                        theseItems.mapNotNull { pair ->
                            pair.second
                                .nullIfEmpty()
                                ?.let { bodyFiles ->
                                    Triple(root, pair.first, bodyFiles)
                                }
                        }
                    }
            }
        return out
            .sortedBy {
                it.second.code
            }
            .toMutableList()
    }

    @JvmStatic
    fun findMatchingBreedInList(
        variant: CaosVariant?,
        items: List<Triple<String, BreedPartKey, List<BodyPartFiles>>?>,
        rootPath: VirtualFile?,
        baseBreed: BreedPartKey,
        allowNull: Boolean = false,
        scope: GlobalSearchScope? = null,
    ): Int? {

        // If no items, return null
        if (items.isEmpty()) {
            return null
        }

        val matchingBreedFiles: MutableList<Pair<Int, Triple<String, BreedPartKey, List<BodyPartFiles>>>> =
            mutableListOf()
        val notMatchingFiles: MutableList<Pair<Int, Triple<String, BreedPartKey, List<BodyPartFiles>>>> =
            mutableListOf()
        val baseBreedString = baseBreed.copyWithPart(null)
        val genderNeutralBreedString = baseBreedString.copyWithGender(null)

        for (i in items.indices) {
            val triple: Triple<String, BreedPartKey, List<BodyPartFiles>> = items[i]
                ?: continue
            val thisBreed = triple.second
            val data = Pair(i, triple)
            if (BreedPartKey.isGenericMatch(thisBreed, genderNeutralBreedString)) {
                matchingBreedFiles.add(data)
            } else {
                notMatchingFiles.add(data)
            }
        }

        val sortAndScope: (items: Iterable<Pair<Int, Triple<String, BreedPartKey, List<BodyPartFiles>>>>) -> List<Pair<Int, Triple<String, BreedPartKey, List<BodyPartFiles>>>> =
            { list: Iterable<Pair<Int, Triple<String, BreedPartKey, List<BodyPartFiles>>>> ->
                list.breedFileSort(variant, baseBreed, rootPath) { (_, triple) ->
                    triple.third.first().spriteFile
                }.filter { (_, triple) ->
                    scope == null || scope.accept(triple.third.first().spriteFile)
                }
            }

        // TODO: figure out if I should prioritize matching breed or matching path
        //  I think breed though. Not sure.
        val matchTemp = (if (PRIORITIZE_BREED) {
                // If there are any breed files matching the base breed
                // Use them first.
                sortAndScope(matchingBreedFiles)
                    .firstOrNull()
                    ?: sortAndScope(notMatchingFiles)
                        .firstOrNull()
            } else {
                // Start with the nearest file and move forward from there
                sortAndScope(notMatchingFiles)
                    .firstOrNull()
                    ?: sortAndScope(matchingBreedFiles)
                        .firstOrNull()
            })

        if (allowNull) {
            if (matchTemp == null) {
                return null
            }
            val matchKey = matchTemp.second.second.copyWithPart(null)
            return if (BreedPartKey.isGenericMatch(matchKey, baseBreed.copyWithPart(null))) {
                matchTemp.first
            } else {
                null
            }
        } else if (matchTemp != null) {
            // If you cannot allow null, take matchTemp even if not an exact match
            return matchTemp.first
        }


        // If there are any breed files matching the base breed
        // Use them first.
        if (matchingBreedFiles.isNotEmpty()) {
            return matchingBreedFiles[0].first
        } else if (notMatchingFiles.isNotEmpty()) {
            return notMatchingFiles[0].first
        }

        // If there are any breed files matching the base breed
        // Use them first.
        LOGGER.warning("Reached end of line, and somehow have not chosen an index")
        return null
    }
}