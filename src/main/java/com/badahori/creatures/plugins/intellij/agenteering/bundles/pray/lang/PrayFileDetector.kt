package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang

import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile
import kotlin.math.min

/**
 * File Overrider to mark .TXT and .PS files as PRAY files
 */
@Suppress("UnstableApiUsage")
class PrayFileOverrider : FileTypeOverrider {

    /**
     * Checks a virtual file to see if it is a PRAY file
     */
    override fun getOverriddenFileType(virtualFile: VirtualFile): FileType? {
        if (virtualFile.isDirectory)
            return null

        // Get and normalize extension if any
        val extension = virtualFile.extension
            ?.lowercase()
            ?.nullIfEmpty()
            ?: return null

        // Make sure file has valid PRAY file extension
        if (extension != "txt" && extension != "ps")
            return null

        val contents = try {
            virtualFile.contents
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            return null
        }
        // If file passes pray validation, return pray file type
        return if (PrayFileDetector.isPrayFile(contents)) {
            PrayFileType
        } else {
            null
        }
    }
}

/**
 *
 */
class PrayFileDetector : FileTypeRegistry.FileTypeDetector {

    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        if (firstCharsIfText == null) {
            return null
        }
        return if (isPrayFile(firstCharsIfText)) {
            PrayFileType
        } else {
            null
        }
    }

    companion object {
        private const val VERSION = 2
        private const val STRING = "\"([^\"]|\\\\\")+\""
        private val LANGUAGE_HEADER_REGEX_CORRECT = "\"[a-z]{2}-[A-Z]{2}\"".toRegex()
        private val LANGUAGE_HEADER_REGEX_INCORRECT = "\"[a-zA-Z]{2}-[a-zA-Z]{2}\"".toRegex()
        private val AGENT_TAG = "group\\s+[a-zA-Z0-9_]{4}\\s+\"[^\"]+\"".toRegex(RegexOption.IGNORE_CASE)
        private val STRING_ASSIGN_REGEX = "$STRING\\s+$STRING".toRegex()
        private val INT_ASSIGN_REGEX = "$STRING\\s+[0-9]+".toRegex()
        private val LOADER_ASSIGN_REGEX = "$STRING\\s+[@]\\s+$STRING".toRegex()
        private val INLINE_FILE = "inline\\s+FILE\\s+$STRING\\s+$STRING".toRegex(RegexOption.IGNORE_CASE)
        private val INLINE_BLOCK = "inline\\s+[A-Za-z0-9_]{4}\\s+$STRING\\s+$STRING".toRegex(RegexOption.IGNORE_CASE)
        private val STRING_REGEX = "${STRING}[ ]+$".toRegex()

        private const val MIN_HIT_POINTS = 16
        private const val MAX_SKIPPED_INCOMPLETE = 3
        private const val MIN_LINES = 3
        /**
         * Checks a text string for its adherence to PRAY file format
         * If any part of the PRAY file is invalid, false is returned
         * - Method is capable of assessing partial text contents as well
         *   that do not terminate on a line boundary
         */
        internal fun isPrayFile(firstCharsIfText: CharSequence?): Boolean {

            // IF file is blank, then type cannot be determined
            if (firstCharsIfText.isNullOrBlank()) {
                return false
            }

            // Split text by newline, and remove empty lines
            val lines = firstCharsIfText
                .trim()
                .split('\n')
                .filter { it.isNotBlank() }
                .nullIfEmpty()
                ?: return false

            // Ensure there are a minimum number of non-empty
            // lines before preceding with check.
            // If lines are low, but first line is a language header
            // Continue with check
            // TODO find right number of lines
            if (lines.size < MIN_LINES && !LANGUAGE_HEADER_REGEX_CORRECT.matches(lines.firstOrNull() ?:"" ) )
                return false

            // Check that this file meets the criteria for
            var hits = 0

            // Bool to see if line should be checked for end of document
            var inComment = false

            // Boolean to show whether the line being processed was part of a comment
            var lineWasCommentRelated: Boolean

            // Boolean to show if a line can be a language header.
            // Language header must come first, after any initial comments
            var lineCanBeLanguage = true

            var skippedIncomplete = 0

            // Loop though lines and check each one for statement validity
            for (i in lines.indices) {
                val line = lines[i].trim()
                lineWasCommentRelated = false
                val points = when {

                    // Check if language header is an exact format match
                    lineCanBeLanguage && LANGUAGE_HEADER_REGEX_CORRECT.matches(line) -> 2

                    // Check if language header is a fuzzy language format match
                    lineCanBeLanguage && LANGUAGE_HEADER_REGEX_INCORRECT.matches(line) -> 2

                    // If starts with double quote, see if it is a tag line
                    line.startsWith('"') && isTag(line) -> 1

                    // If line starts with single quote and is tag line, ignore it
                    // As it could be an incorrectly formated tag, or unrelated to PRAY
                    line.startsWith('"') && isTag(line) -> 0

                    // If line starts with '#', then it is a comment line
                    // Comments starting with '#' are common in languages, so ignore it
                    line[0] == '#' -> {
                        lineWasCommentRelated = true
                        0
                    }

                    // This is a distinctive comment format, so it is worth a point to start
                    line.startsWith("(-") -> {
                        lineWasCommentRelated = true
                        inComment = true;
                        1
                    }

                    // Tag is agent tag like, i.e. group AGNT "..."
                    line.matches(AGENT_TAG) -> 2

                    // Tag is inline FILE "" ""
                    line.matches(INLINE_FILE) -> 3

                    // Tag is inline block format. i.e. inline GENE "..." "..."
                    line.matches(INLINE_BLOCK) -> 1

                    // If in Comment, check if it is comment end.
                    // If it is, gain points, if not, ignore line
                    inComment -> {
                        lineWasCommentRelated = true
                        // Line is comment end
                        if (line.endsWith("-)")) {
                            inComment = false
                            2
                        // Line is some line possibly inside a comment
                        } else {
                            0
                        }
                    }

                    // If nothing matches, but this is the last line,
                    // Line may be incomplete, so we should exit here
                    i == lines.lastIndex -> break

                    line.matches(STRING_REGEX) -> {
                        if (++skippedIncomplete > MAX_SKIPPED_INCOMPLETE)
                            return false
                        else
                            0
                    }

                    // If line matches no known format, return false
                    // We do not want to be too eager to set file to PRAY
                    else -> {
                        return false
                    }
                }

                // Comments can precede the LANGUAGE tag,
                // So if the last line was a comment,
                // then we should allow the tag to still be possible
                // If it is not a comment, then the LANGUAGE tag can not come
                // After it, even if this line was the language
                // There can be only one, and it must be at the start of the file
                // With only comments before it

                // Here this line was not a comment,
                // So a language tag is impossible from this point on
                if (!lineWasCommentRelated) {
                    lineCanBeLanguage = false
                }
                // Add the points from the match to the hits counter
                hits += points

                // If hit points is greater than MIN_HIT_POINTS
                // Then assume the file is a match
                // NOTE: prevents needing to evaluate too much.
                if (hits > MIN_HIT_POINTS) {
                    return true
                }
            }

            // If this file ends in a comment, then that means the comment was not exited
            // This could be due to incomplete fragment,
            // or because the comment start token '(-' was used in a non-pray file
            return (!inComment && hits > min(8, lines.size))
        }

        private fun isTag(line: String): Boolean {
            return line.matches(STRING_ASSIGN_REGEX) ||
                    line.matches(INT_ASSIGN_REGEX) ||
                    line.matches(LOADER_ASSIGN_REGEX)
        }
    }
}
