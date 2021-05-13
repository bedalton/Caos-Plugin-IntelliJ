package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteBodyPart
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.compress.utils.Lists
import java.util.logging.Logger

object BreedDataUtil {

    private val LOGGER = Logger.getLogger("#BreedDataUtil")


    /**
     * Gets the body part file given a specific or related breed file
     *
     * @param partChar  char to look for
     * @param breedFile selected breed file in drop down
     * @return Sprite body part data for a part and breed file
     */
    @JvmStatic
    fun getSpriteBodyPart(
        project: Project,
        variant: CaosVariant,
        files: List<BodyPartFiles>,
        manualAtts: Map<Char, VirtualFile>,
        baseBreed: BreedPartKey,
        partChar: Char,
        breedFile: VirtualFile
    ): SpriteBodyPart? {
        // Ensure that breed file has parent
        if (breedFile.parent == null) {
            LOGGER.severe("Breed file parent is null")
            return null
        }
        val parentPath = breedFile.parent.canonicalPath?.toLowerCase()
            ?: breedFile.parent.path
        val partString = "" + partChar.toLowerCase()

        // Generate a key from this breed file
        val key = BreedPartKey.fromFileName(
            "" + partChar + breedFile.nameWithoutExtension.toLowerCase().substring(1),
            variant
        )

        // Find matching breed file for creature
        val matching: List<BodyPartFiles> = files.filter { b: BodyPartFiles ->
            b.key != null && BreedPartKey.isGenericMatch(b.key, key) &&
                    b.bodyDataFile.let { it.canonicalPath ?: it.path }.toLowerCase().startsWith(parentPath) && b.bodyDataFile.name.toLowerCase().startsWith(partString)
        }

        // If item was found for this breed
        // Wrap it to return
        var out = if (matching.isNotEmpty()) {
            matching.firstOrNull()
        } else {
            if (partChar in PoseCalculator.nullableParts) {
                return null
            }
            // Body part data does not exist for this part and breed
            // Happens when upper arm has breed sprite but lower arm doesn't, etc
            // Get a breed free key
            val fallback: BreedPartKey = baseBreed.copy(
                breed = null,
                part = partChar
            )
            // Filter file age, gender, genus and part
            files.firstOrNull { b: BodyPartFiles ->
                b.key != null && BreedPartKey.isGenericMatch(b.key, fallback)
            }
        } ?: return null // If still nothing was found, bail out

        // If manual att was passed in, use it instead
        manualAtts[partChar]?.let {
            out = out.copy(bodyDataFile = it)
        }
        // Return the resolved sprite and att file from the two virtual files
        return out.data(project)
    }

    /**
     * Finds breeds for a given part
     *
     * @param files     all available breed files for any part
     * @param partChars parts to filter the breed list for
     * @return breed body files available for the parts
     */
    @JvmStatic
    fun findBreeds(files: List<BodyPartFiles>, baseBreed: BreedPartKey, partChars: Array<Char>): MutableList<VirtualFile> {
        val key: BreedPartKey = baseBreed.copy(
            breed = null,
            part = null
        )
        return files.filter { part: BodyPartFiles ->
            val thisKey = part.key
                ?: return@filter false
            BreedPartKey.isGenericMatch(thisKey, key) && thisKey.part in partChars
        }
            .map(BodyPartFiles::bodyDataFile)
            .sortedBy {
                it.nameWithoutExtension
            }
            .toMutableList()
    }

    @JvmStatic
    fun findMatchingBreedInList(items: List<VirtualFile?>, rootPath: String?, baseBreed: BreedPartKey, allowNull:Boolean = false): Int? {
        // Find all breed files matching this path

        // Find all breed files matching this path
        val parentPath = rootPath?.toLowerCase()
        val matchingBreedFiles: MutableList<Pair<Int, VirtualFile>> = Lists.newArrayList()
        val baseBreedString = "" + baseBreed[1] + "" + baseBreed[2] + "" + baseBreed[3]
        for (i in items.indices) {
            val file: VirtualFile? = items[i]
            val thisBreed = file?.nameWithoutExtension?.toLowerCase()?.substring(1)
            if (thisBreed == baseBreedString) {
                if (parentPath != null && file.path.toLowerCase().startsWith(parentPath)) {
                    return i
                }
                // Breed string matches
                matchingBreedFiles.add(Pair(i, file))
            }
        }

        if (matchingBreedFiles.isEmpty() || allowNull) {
            return null
        }

        // TODO: figure out if I should prioritize matching breed or matching path
        //  I think breed though. Not sure.

        // If root path was set, find matching att files for this part.
        if (parentPath != null) {
            // If a matching breed was not found in root folder
            // Look for any other breed file in the root folder.
            for (i in items.indices) {
                if (items[i]?.canonicalPath?.toLowerCase()?.startsWith(parentPath).orFalse()) {
                    return i
                }
            }
        }

        // If there are any breed files matching the base breed
        // Use them first.

        // If there are any breed files matching the base breed
        // Use them first.
        if (matchingBreedFiles.isNotEmpty()) {
            return matchingBreedFiles[0].first
        }

        // If there are any breed files matching the base breed
        // Use them first.
        return 0
    }
}