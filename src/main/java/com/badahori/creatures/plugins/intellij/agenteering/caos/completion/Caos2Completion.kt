package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.bedalton.common.util.*
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.max
import kotlin.math.min


private val whiteSpaceOrQuote = "[ \t'\"]".toRegex()

internal object Caos2CompletionProvider {

    internal fun addCaos2Completion(
        resultSet: CompletionResultSet,
        extendedSearch: Boolean,
        variant: CaosVariant,
        caos2Block: CaosScriptCaos2Block,
        caos2Child: PrayChildElement,
    ) {
        // Ensure element has caos file parent
        val caosFile = caos2Child.containingFile as? CaosScriptFile
            ?: return

        // Make sure parent is command or tag, as those should be the only parents
        val parent = (caos2Child.parent as? CaosScriptCaos2Statement)
            ?: (caos2Child.parent?.parent as? CaosScriptCaos2Statement)

        if (parent == null) {
            return
        }

        val caos2Cob = variant.isOld && caos2Block.isCaos2Cob
        val caos2Pray = variant.isNotOld && caos2Block.isCaos2Pray

        val text = caos2Child.text
        // Get case for Command name completion which is case flexible (at least in my plugin)
        val case = text.stripSurroundingQuotes().replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
            .nullIfEmpty()
            ?.case
            ?: Case.CAPITAL_FIRST

        if (caos2Cob) {
            // Add tag completions here too, as tag is command before any other values are added
            addCaos2CobTagCompletions(
                resultSet,
                variant,
                caosFile,
                caos2Child
            )
        } else {
            addCaos2PrayTagCompletions(
                resultSet,
                caosFile.prayTags,
                caos2Child,
                case,
                isCaosScriptFile = true
            )
        }


        if (caos2Child is CaosScriptCaos2CommandName && variant.isNotOld) {
            val file = caos2Child.containingFile
            for (action in PrayCommand.getActionCommands()) {
                val insertAction = LinkFilesInsertHandler(action, true)
                if (!insertAction.isAvailable(file)) {
                    continue
                }
                val addMany = LookupElementBuilder
                    .create("${action.matchCase(case)} files of type..")
                    .withInsertHandler(insertAction)
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
                resultSet.addElement(addMany)
            }
        }

        // Add completions for CAOS2 command names or tag names
        if (caos2Child is CaosScriptCaos2CommandName) {
            // Get all previous commands to prevent suggesting them again
            val previousCommands = caos2Block
                .commands
                .map { it.first }
                .toSet()
            if (caos2Cob) {
                addCaos2CobCommandCompletions(resultSet, variant, case, previousCommands)
            } else if (caos2Pray) {
                addCaos2PrayCommandCompletions(resultSet, case, previousCommands)
            }
            return
        } else if (caos2Child !is PrayTagValue) {
            // Is not a value, so no need to continue
            return
        }

        // Get child as pray tag value
        val value = caos2Child

        val directory = caos2Child.directory
            ?: return

        val quoter = quoter(text)
        if (caos2Cob) {
            val cobTag = (parent as? CaosScriptCaos2Tag)?.let { tagElement ->
                CobTag.fromString(tagElement.tagName)
            }
            addCaos2CobFileNameCompletions(
                resultSet,
                directory,
                cobTag,
                value,
            )
            return
        }

        addCaos2PrayFileNameCompletions(variant, resultSet, extendedSearch, directory, value, quoter)
    }

    /**
     * Add CAOS2Cob TAG name completion
     */
    private fun addCaos2CobTagCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        caosFile: CaosScriptFile,
        tagNameElement: PrayChildElement,
    ) {
        if (tagNameElement !is CaosScriptCaos2CommandName && tagNameElement !is CaosScriptCaos2TagName) {
            return
        }
        val existingTags: List<CobTag> = caosFile.prayTags.map { it.tag }.mapNotNull { tagString ->
            CobTag.fromString(tagString)
        }
        val tagsRaw = CobTag.getTags(variant).filterNot { tag -> tag in existingTags }

        val tagTextRaw = tagNameElement.tagText
            ?: return
        val tagTextRawLength = tagTextRaw.length
        // Get tag text if any including previous siblings
        val textComponents = tagTextRaw
            .split("\\s+".toRegex())

        val case = tagTextRaw.nullIfEmpty()?.case ?: Case.CAPITAL_FIRST

        // Getting all possible tag names
        // Filter them down to the variant of the tag, that is closest to the string already types
        // In this plugin, there are name alternatives for tags in CAOS2Cob
        val tags: List<LookupElement> = tagsRaw.mapNotNull map@{ tag ->
            // Get tag keys
            val keys = tag.keys
            // If one starts with text, return it first
            keys.firstOrNull { key -> key.startsWith(tagTextRaw) }?.let { key ->
                return@map key
            }

            // Get the entered texts distance from all keys in this tag
            @Suppress("SimplifiableCallChain")
            val match = keys.mapNotNull keys@{ key ->
                val keyLength = key.length
                if (keyLength == 0)
                    return@keys null

                val mod = min(tagTextRawLength, keyLength) / max(tagTextRawLength, keyLength)
                val distance = tagTextRaw.levenshteinDistance(key) * mod
                Pair(distance, key)
            }.sortedBy { it.first }
                .firstOrNull()
                ?: return@map null
            // Decide on completion tag. If tags are too far out of
            (if (match.first > 0.8) {
                keys.first()
            } else {
                match.second
            }).matchCase(case)
        }.map { key ->
            val parts = key.split(' ')
            val skip = textComponents.size - 1
            val text = parts.drop(skip).joinToString(" ")
            return@map LookupElementBuilder.createWithSmartPointer(text, tagNameElement)
                .withLookupStrings(listOf(key, text))
                .withPresentableText(key)
                .withInsertHandler(EqualSignInsertHandler)
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        }
        resultSet.addAllElements(tags)
    }


    /**
     * Add CAOS2Cob command name completion
     */
    private fun addCaos2CobCommandCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        case: Case,
        commandsUsed: Set<String>,
    ) {
        CobCommand.getCommands(variant).forEach command@{ command ->
            if (command.singleton && commandsUsed.any { command.key.matches(it) })
                return@command
            resultSet.addElement(
                LookupElementBuilder.create(command.keyStrings.first().matchCase(case))
                    .withInsertHandler(SpaceAfterInsertHandler)
            )
        }
        resultSet.addElement(
            LookupElementBuilder.create("Link".matchCase(case))
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        )
    }

    /**
     * Add PRAY command completion for CAOS2Pray files
     */
    private fun addCaos2PrayCommandCompletions(
        resultSet: CompletionResultSet,
        case: Case,
        commandsUsed: Set<String>,
    ) {
        PrayCommand.values().forEach command@{ command ->
            if (command.singleton && commandsUsed.any { command.key.matches(it) })
                return@command
            resultSet.addElement(
                LookupElementBuilder.create(command.keyStrings.first().matchCase(case))
                    .withInsertHandler(SpaceAfterInsertHandler)
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            )
        }
    }


    /**
     * Add Tag completion for CAOS2Cob files
     */
    internal fun addCaos2PrayTagCompletions(
        resultSet: CompletionResultSet,
        prayTags: List<PrayTagStruct<*>>,
        tagElement: PrayChildElement?,
        case: Case?,
        isCaosScriptFile: Boolean,
        eggs: Boolean = false,
    ) {
        if (tagElement is CaosScriptCaos2Value || (tagElement?.parent as? CaosScriptCaos2Command)?.firstChild != tagElement) {
            return
        }
        val existingTags: List<String> = prayTags.map { it.tag.lowercase() }

        val tags = PrayTags.allLiterals
            .keys
            .filter {
                !(isCaosScriptFile && PrayTags.isAutogenerated(
                    it,
                    true
                )) && it.lowercase() !in existingTags && PrayTags.getShortTagReversed(it, true) !in existingTags
            } + (
                if (isCaosScriptFile)
                    PrayTags.shortTagLiterals
                        .keys
                        .filter { tagName ->
                            tagName.lowercase() !in existingTags && PrayTags.normalize(tagName)?.lowercase()
                                ?.let { normalizedTag ->
                                    !(PrayTags.isAutogenerated(
                                        normalizedTag,
                                        true
                                    )) && normalizedTag !in existingTags
                                } == true
                        }.let { tags ->
                            if (case != null) {
                                tags.map { tag ->
                                    tag.matchCase(case)
                                }
                            } else
                                tags
                        }
                else
                    emptyList()
                ) + (
                if (eggs) {
                    PrayTags.eggTagLiterals
                        .keys
                        .filter { it.lowercase() !in existingTags }
                } else
                    emptyList()
                )

        val hasTagElement = tagElement != null
        var elements = tags.map { tag ->
            (if (hasTagElement) {
                LookupElementBuilder.createWithSmartPointer(tag, tagElement!!)
            } else {
                LookupElementBuilder.create(tag)
            })
                .withLookupStrings(
                    listOf(
                        tag,
                        tag.lowercase(),
                        tag.matchCase(Case.CAPITAL_FIRST)
                    )
                )
                .withPresentableText(tag)
        }
        val start = tagElement?.parent?.startOffset

        // Get possible equal sign insert handler
        val equalSignInsertHandler: InsertHandler<LookupElement>? = if (isCaosScriptFile) {
            EqualSignInsertHandler
        } else {
            null
        }

        // Get actual insert handler
        val insertHandler: InsertHandler<LookupElement>? = if (start != null) {
            ReplaceFromStartInsertHandler(start, EqualSignInsertHandler)
        } else if (isCaosScriptFile) {
            equalSignInsertHandler
        } else {
            null
        }

        // Apply insert handler
        if (insertHandler != null) {
            elements = elements.map {
                it.withInsertHandler(insertHandler)
            }
        }
        resultSet.addAllElements(elements.map {
            it.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        })
    }


    /**
     * Add filename completion for CAOS2Cob tag
     */
    private fun addCaos2CobFileNameCompletions(
        resultSet: CompletionResultSet,
        directory: VirtualFile,
        tag: CobTag?,
        valueElement: PrayTagValue,
    ) {

        //
        if (tag != CobTag.THUMBNAIL) {
            return
        }


        // Get files of appropriate type
        val files = VirtualFileUtil.childrenWithExtensions(
            directory,
            true,
            "spr",
            "s16",
            "c16",
            "png",
            "jpg",
            "jpeg",
            "gif",
            "bmp",
            "wav"
        )

        // Ensure there are matching files
        if (files.isEmpty())
            return

        // Get parent path length for sizing down the relative child paths
        val parentPathLength = directory.path.length + 1

        val openQuote = valueElement.text?.firstOrNull()?.let { if (it == '\'' || it == '"') it else null }
        val quoteFallback = openQuote?.toString() ?: "\""
        val quoter = quoter(valueElement.text)

        // Loop through files and add lookup elements
        for (file in files) {
            // Get relative path from parent
            val relativePath = file.path.substring(parentPathLength)

            // Get quotes to add if any
            val needsQuote = relativePath.contains(whiteSpaceOrQuote)
            val quote = if (!needsQuote) {
                openQuote?.toString().orEmpty()
            } else {
                quoteFallback
            }

            // Add quotes and also escape any quotes found inside this path
            val quotedPath = quoter(relativePath.replace(quote, "\\" + quote))

            val element = if (file.extension likeAny SpriteParser.VALID_SPRITE_EXTENSIONS) {
                // Process filenames if is thumbnail
                val fileParts = quotedPath.split(".")
                if (fileParts.size < 2)
                    continue
                val basePath = fileParts.dropLast(1).joinToString(".")

                // Create insert handler for use by thumbnail tags to move the cursor between the [^] in sprite thumbnails
                val insertHandler = OffsetCursorInsertHandler(basePath.length + 1) // ^].spr"

                LookupElementBuilder.createWithSmartPointer(basePath + "[#]." + fileParts.last(), valueElement)
                    .withLookupString(basePath + "[]." + fileParts.last())
                    .withInsertHandler(insertHandler)
            } else {
                LookupElementBuilder.createWithSmartPointer(relativePath, valueElement)
                    .withLookupStrings(listOf(file.name, relativePath))
            }
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(element)
        }
    }

    /**
     * Add CAOS2Pray file name completion for known tags
     */
    internal fun addCaos2PrayFileNameCompletions(
        variant: CaosVariant,
        resultSet: CompletionResultSet,
        extendedSearch: Boolean,
        directory: VirtualFile,
        element: PrayTagValue,
        quoter: (String) -> String,
    ) {
        val (requiredExtensions: List<String>?, dropExtension: Boolean) = (element.parent as? PrayTag)?.let { tag ->
            val tagName = tag.tagName
                .nullIfEmpty()
                ?: return
            tagRequiresFileOfType(tagName)
        } ?: (element.parent as? CaosScriptCaos2Command)?.let { command ->
            val extensions =
                if (element.variant?.nullIfUnknown()?.isNotOld ?: (element.containingCaosFile?.caos2 != CAOS2Cob)) {
                    val prayCommand = PrayCommand.fromString(command.commandName, variant)
                        ?: return
                    when (prayCommand) {
                        PrayCommand.LINK -> listOf("cos", "caos")
                        PrayCommand.INLINE -> attachableFileExtensions
                        PrayCommand.ATTACH -> attachableFileExtensions
                        PrayCommand.DEPEND -> attachableFileExtensions
                        PrayCommand.REMOVAL_SCRIPTS -> listOf("cos", "caos")
                        PrayCommand.PRAY_FILE -> return
                    }
                } else {
                    val cobCommand = CobCommand.fromString(command.commandName, variant)
                        ?: return
                    when (cobCommand) {
                        CobCommand.LINK -> listOf("cos", "caos")
                        CobCommand.COBFILE -> return
                        CobCommand.INSTALL_SCRIPTS -> listOf("cos", "caos")
                        CobCommand.REMOVAL_SCRIPTS -> listOf("cos", "caos")
                        CobCommand.ATTACH -> listOf("wav", "s16")
                        CobCommand.INLINE -> listOf("wav", "s16")
                        CobCommand.DEPEND -> listOf("wav", "s16")
                    }
                }

            Pair(extensions, false)
        } ?: null.let {
            return
        }

        val rawText = element.textWithoutCompletionIdString
            .trim(' ', '"', '\n', '\t')

        val pathSeparatorChar = if (rawText.contains('/')) {
            '/'
        } else {
            pathSeparatorChar
        }

        val pathSeparator = "$pathSeparatorChar"

        val components = rawText.split(pathSeparatorChar)

        var trueDirectory = directory
        for (component in components) {
            val testPath = PathUtil.combine(trueDirectory.path, component)
            val temp = VfsUtil.findFile(Paths.get(testPath), false)
                ?: break
            if (!temp.isDirectory) {
                break
            }
            trueDirectory = temp
        }

//        LOGGER.info("Getting children in directory: ${trueDirectory.path}")
        // Get files of appropriate type
        val childFiles = if (requiredExtensions != null) {
            VirtualFileUtil.childrenWithExtensions(trueDirectory, extendedSearch, *requiredExtensions.toTypedArray())
        } else {
            VirtualFileUtil.collectChildFiles(trueDirectory, extendedSearch)
        }

        val childDirectories = trueDirectory.children
            .orEmpty()
            .filter { it.isDirectory }

        val files = childFiles + childDirectories

        // Ensure there are matching files
        if (files.isEmpty()) {
            return
        }
        // Loop through files and add lookup elements
        for (file in files) {
            val childPath = VfsUtil.findRelativePath(directory, file, pathSeparatorChar)
                ?: continue
            val relativePath = if (dropExtension) {
                val parentPath = PathUtil.getWithoutLastPathComponent(childPath)
                val baseName = PathUtil.getFileNameWithoutExtension(childPath)
                    ?: continue
                (parentPath ?: "") + baseName
            } else {
                childPath
            }
            val tail = if (file.isDirectory) {
                pathSeparator
            } else {
                ""
            }
            val lookupString = quoter(relativePath + tail)
            val builder = LookupElementBuilder
                .createWithSmartPointer(lookupString, element)
                .withLookupStrings(listOf(file.name, childPath))
                .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            resultSet.addElement(builder)
        }
    }


    private val PrayChildElement.tagText: String?
        get() {
            if (this is PrayTagName)
                return this.text
            if (this is CaosScriptCaos2CommandName)
                return this.text
            else if (this.firstChild is CaosScriptQuoteStringLiteral && this.prevSibling != null) {
                // This is a quoted string not at the beginning, so it has to a value inside a command
                return null
            }
            // Get parent caos2 statement
            val parent = this.parent as CaosScriptCaos2Statement

            // Get all non-whitespace children
            val children = parent.children.filter { it.tokenType != TokenType.WHITE_SPACE }
                .nullIfEmpty()
                ?: return ""

            // This element has child elements in quotes, so it cannot be a tag
            if (children.drop(1).any { child -> child.text.firstOrNull()?.let { it == '\'' || it == '"' } == true })
                return null

            val startOffset = this.startOffset
            val previousSiblings = children.filter { it.startOffset < startOffset }

            // This element comes after an equal sign
            if (previousSiblings.any { it.tokenType == CaosScriptTypes.CaosScript_EQUAL_SIGN })
                return null

            // This tag appears to be in the middle of a set of words
            val text = previousSiblings.joinToString(" ") { it.text }.trim() + ' ' + this.text

            return if (text.endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
                text.substringFromEnd(0, CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.length).trim()
            else
                text
        }
}
