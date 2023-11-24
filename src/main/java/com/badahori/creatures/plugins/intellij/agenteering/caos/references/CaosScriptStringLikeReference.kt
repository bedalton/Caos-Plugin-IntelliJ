package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineText
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInputFileName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayString
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.MAME
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.NAME
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.notLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parentParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isPossiblyCaos
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.stringTextToAbsolutePath
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.bedalton.common.util.PathUtil.getFileNameWithoutExtension
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil

/**
 * Checks that a string literal is reference to other named game vars
 */
abstract class CaosScriptStringLikeReference<T : CaosScriptStringLike>(
    element: T,
    range: TextRange = getStringNameRangeInString(element)
) : PsiPolyVariantReferenceBase<T>(element, range) {

    private val selfOnlyResult by lazy {
        if (myElement.isValid)
            PsiElementResolveResult.createResults(myElement)
        else
            ResolveResult.EMPTY_ARRAY
    }

    private val variant by lazy {
        myElement.variant
    }
    private val inPrayFile get() = element.containingFile is PrayFile
    private val inCatalogueFile get() = element.containingFile is CatalogueFile

    protected val fileInfo: Int by lazy {
        getFileInfoType(element, parameterFileExtensions)
    }

    private val relative: Boolean by lazy {
        (element.parent is PrayInputFileName) ||
                (element.parent is PrayTagValue)
    }

    private val isCatalogueTag by lazy {
        if (valuesListName?.lowercase()?.equals("catalogue.tag") == true) {
            return@lazy true
        }
        val containingCommand = containingCommand
            ?.command
            ?.uppercase()
        val inCatalogueCommand = containingCommand in listOf(
            "READ",
            "REAQ",
            "REAN"
        )
        if (!inCatalogueCommand) {
            return@lazy false
        }
        val parameterIndex = myElement.getParentOfType(CaosScriptArgument::class.java)?.index
        parameterIndex == 0
    }

    private val journalData: Pair<String, Int>? by lazy {
        getJournalData(element)
    }

    private val valuesListName by lazy {
        val variant = variant
            ?: return@lazy null
        parameter
            ?.valuesList
            ?.get(variant)
            ?.name
    }

    private val parameterFileExtensions: Set<String>? by lazy {

        // Get stub extensions first if possible
        stubExtensions()?.let {
            return@lazy it
        }

        // Get values list name if any for string parameter
        val name = valuesListName
            ?: return@lazy null

        // Ensure this values list is files kind list
        if (!name.startsWith("File.", ignoreCase = true)) {
            return@lazy null
        }

        // Get file extensions from values list name
        name.lowercase()
            .substring(5)
            .split('/')
            .toSet()
    }

    private val shouldResolveToFile: Boolean
        get() {
            return fileInfo > 0
        }

    private val parentArgument by lazy {
        myElement.getSelfOrParentOfType(CaosScriptArgument::class.java)
    }

    private val containingCommand by lazy {
        (parentArgument?.parent as? CaosScriptCommandElement)
            ?.commandDefinition
    }

    private val parameter by lazy {
        (element as? CaosScriptCompositeElement)
            ?.parentParameter
    }

    private val namedVarType: CaosScriptNamedGameVarType? by lazy {
        ((element.parent?.parent as? CaosScriptNamedGameVar)
            ?: (element.parent?.parent?.parent as? CaosScriptNamedGameVar))?.varType?.let {
            return@lazy it
        }

        val command = element.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return@lazy null
        when (command.commandStringUpper) {
            "DELG" -> CaosScriptNamedGameVarType.GAME
            "DELE" -> CaosScriptNamedGameVarType.EAME
            "DELN" -> NAME
            else -> null
        }

    }

    private val key: String? by lazy {
        element.getSelfOrParentOfType(CaosScriptNamedGameVar::class.java)?.key
    }


    private val isCaos2FileString by lazy {
        isCaos2FileString(myElement)
    }

    private val isPrayFileString by lazy {
        isPrayFileString(myElement)
    }


    /**
     * Returns true for any matching var in project
     * TODO: Mask NAME and MAME by agent class
     */
    override fun isReferenceTo(anElement: PsiElement): Boolean {

        if (shouldResolveToFile) {
            return isReferenceToFile(anElement)
        }

        // Ensure that variables share the same variant,
        // otherwise the variables cannot be the same
        val thisVariant = myElement.variant
        val otherVariant = anElement.variant
        if (thisVariant != null && otherVariant != null && otherVariant notLike thisVariant) {
            return false
        }

        if (anElement is CatalogueItemName) {
            return isCatalogueTag && anElement.name.lowercase() == element.name?.lowercase()
        }

        if (anElement is CaosScriptStringLike && anElement.stringStubKind == StringStubKind.JOURNAL) {
            return isReferenceToJournal(anElement)
        }

        return isReferenceToNamedVar(anElement)
    }

    /**
     * Resolves to itself to prevent weird ctrl+click behavior
     */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project

        if (project.isDisposed) {
            return ResolveResult.EMPTY_ARRAY
        }

        if (DumbService.isDumb(project)) {
            return selfOnlyResult
        }

        if (isCatalogueTag) {
            return try {
                resolveToCatalogueTag(project)
            } catch (_: IndexNotReadyException) {
                selfOnlyResult
            }
        }

        if (shouldResolveToFile) {
            return try {
                resolveToFile(project)
            } catch (_: IndexNotReadyException) {
                selfOnlyResult
            }
        }


        val variant = variant
            ?: return selfOnlyResult


        // C1/C2 can only resolve to files
        if (variant.isOld) {
            return selfOnlyResult
        }

        // Get named variable references
        if (namedVarType != null) {
            return resolveToNamedGameVars(project)
        }

        // If this is a journal file name return only journal name matches
        val journalMatches = getJournalNameMatches(variant, project)
        if (journalMatches != null) {
            return PsiElementResolveResult.createResults(journalMatches)
        }

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Resolve element to any physical files
     */
    private fun resolveToFile(project: Project): Array<ResolveResult> {

        // Get scope if any
        val scope = GlobalSearchScope.projectScope(project)

        val files = CaosStringToFileResolver
            .resolveToFiles(project, myElement, scope)
            .nullIfEmpty()
            ?: return ResolveResult.EMPTY_ARRAY


        val text = myElement.text
        val range = rangeInElement
        var substring = myElement.text.substring(range.startOffset)
        if (substring.endsWith('"') && !substring.endsWith("\\\"")) {
            substring = substring.substringFromEnd(0, 1)
        }

        val pathSeparatorChar = getPathSeparator(text)

        val resolved = if (!text.contains(pathSeparatorChar)) {
            files.mapNotNull {
                it.toNavigableElement(project)
            }
        } else {
            files.mapNotNull { file ->
                val split = substring
                    .split(pathSeparatorChar)
                    .reversed()

                if (split.size == 1) {
                    return@mapNotNull file.toNavigableElement(project)
                }

                var theFile = file
                var lastComponentWasJumpUp = false
                for (component in split.drop(1)) {
                    theFile = when (component) {
                        ".." -> theFile.parent?.also {
                            lastComponentWasJumpUp = true
                        }

                        "." -> continue
                        else -> {
                            if (lastComponentWasJumpUp) {
                                lastComponentWasJumpUp = false
                                theFile.findChild(component)
                            } else {
                                theFile.parent?.parent?.findChild(component)
                            }
                        }
                    } ?: return@mapNotNull null
                }
                theFile.toNavigableElement(project)
            }
        }

        return PsiElementResolveResult.createResults(resolved)
    }

    private fun resolveToCatalogueTag(project: Project): Array<ResolveResult> {
        val name = myElement.stringValue

        val results = CatalogueEntryElementIndex
            .Instance[name, project]
            .nullIfEmpty()
            ?.mapNotNull {
                it.nameIdentifier
            }
            ?: return ResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(results)
    }

    private fun getJournalNameMatches(
        variant: CaosVariant,
        project: Project,
    ): List<CaosScriptStringLike>? {
        if (!variant.isNotOld) {
            return null
        }
        val parentFolder = myElement.virtualFile?.parent
            ?: return null

        val (journalName, meta) = journalData
            ?: return null

        val indexedStrings = if (!DumbService.isDumb(project)) {
            CaosScriptStringLiteralIndex
                .instance
                .getAllInScope(project, GlobalSearchScopes.directoryScope(project, parentFolder, false))
                .nullIfEmpty()
                ?: CaosScriptStringLiteralIndex
                    .instance
                    .getAllInScope(project, GlobalSearchScope.projectScope(project))
        } else {
            // If index is dumb, just gather all sibling file's strings
            element.collectElementsOfTypeInParentChildren(
                CaosScriptQuoteStringLiteral::class.java,
                false
            )
        }

        // Get all strings within this file
        val inFileStrings = PsiTreeUtil.collectElementsOfType(
            element.containingFile,
            CaosScriptQuoteStringLiteral::class.java
        )

        return (indexedStrings + inFileStrings)
            .filter {
                if (it.stringStubKind == StringStubKind.JOURNAL) {
                    val thisMeta = (it as? CaosScriptQuoteStringLiteral)?.meta
                        ?: return@filter false
                    meta == -1 || thisMeta == -1 || meta == thisMeta && journalName.equalsIgnoreCase(it.stringValue)
                } else {
                    false
                }
            }.distinctBy { it.textRange }
    }

    private fun resolveToNamedGameVars(project: Project): Array<ResolveResult> {

        // Only get results if named game var is not null
        val namedVarType = namedVarType
            ?: return ResolveResult.EMPTY_ARRAY

        // Find variables by type and name
        val references = if (namedVarType == MAME || namedVarType == NAME) {
            // MAME AND NAME resolve to the same variables
            // ...if TARG and OWNR are the same; OWNR and TARG classes are not resolved currently
            getNamed(project, MAME) + getNamed(project, NAME)
        } else {
            getNamed(project, namedVarType)
        }



        return if (references.isEmpty()) {
            ResolveResult.EMPTY_ARRAY
        } else {
            if (references.size == 1 && (references[0] == myElement || references[0] == myElement.parent)) {
                return ResolveResult.EMPTY_ARRAY
            }
            PsiElementResolveResult.createResults(references)
        }
    }

    private fun getNamed(
        project: Project,
        type: CaosScriptNamedGameVarType,
    ): List<PsiElement> {
        val key = key
            ?: return emptyList()
        return CaosScriptNamedGameVarIndex
            .instance[type, key, project, GlobalSearchScope.projectScope(project)]
            .filter { anElement ->
                ProgressIndicatorProvider.checkCanceled()
                anElement.isValid
            }
            .mapNotNull { namedGameVar ->
                ProgressIndicatorProvider.checkCanceled()
                namedGameVar.rvalue?.quoteStringLiteral?.let { stringLiteral ->
                    if (stringLiteral.isValid) {
                        stringLiteral
                    } else {
                        null
                    }
                } ?: namedGameVar.nameIdentifier
            }
    }

    private fun isReferenceToFile(anElement: PsiElement): Boolean {

        if (anElement is PsiDirectory && fileInfo > 0) {
            val range = rangeInElement
            return myElement.text.substring(range.startOffset, range.endOffset).lowercase() == anElement.name
        }

        if (anElement !is PsiFile) {
            return false
        }

        val otherVirtualFile = anElement.virtualFile
            ?: return false

        if (fileInfo != NO_EXTENSION) {
            return resolvesWithRelativePath(otherVirtualFile)
        }

        val extensions = parameterFileExtensions
            .nullIfEmpty()
            ?: return false

        val myName = myElement.name
            ?: return false

        val myNameWithoutExtension = getFileNameWithoutExtension(myName)
            ?.nullIfEmpty()
            ?.lowercase()
            ?: return false

        val otherName = otherVirtualFile.name
            .lowercase()

        val possibleNames = extensions.map { "$myNameWithoutExtension.$it".lowercase() }
        return (otherName in possibleNames)
    }


    private fun resolvesWithRelativePath(otherFile: VirtualFile): Boolean {
        val myPathLowercase = myElement.stringTextToAbsolutePath()
            ?: return false
        val otherPathLowercase = otherFile.path
        return otherPathLowercase like myPathLowercase
    }


    private fun isReferenceToJournal(anElement: CaosScriptStringLike): Boolean {
        val (journalName, meta) = journalData
            ?: return false
        val (otherName, otherMeta) = getJournalData(anElement)
            ?: return false
        if (meta != -1 && otherMeta != -1 && meta != otherMeta) {
            return false
        }
        return journalName.equalsIgnoreCase(otherName)
    }

    private fun isReferenceToNamedVar(anElement: PsiElement): Boolean {
        // Ensure other is or has named game var parent element
        val namedGameVarParent = (anElement.parent?.parent as? CaosScriptNamedGameVar
            ?: anElement.parent?.parent?.parent as? CaosScriptNamedGameVar)
            ?: return false
        // Check that type and key are the same
        return namedGameVarParent.varType == namedVarType && namedGameVarParent.key == key
    }

    private fun stubExtensions(): Set<String>? {
        val stubKind = myElement.stringStubKind
        return stubKind?.extensions
            ?.map { it.lowercase() }
            .nullIfEmpty()
            ?.toSet()
    }

    protected fun handleElementRename(element: CaosScriptStringLike, newElementName: String): PsiElement {

        // If is File
//        val newPathOrName = if (fileInfo > 0) {
//            getFileNameString(element, newElementName)
//        } else {
//            newElementName
//        }
        val newPathOrName = newElementName
        return (element as? PsiNamedElement)?.setName(newPathOrName) ?: element

    }

//    private fun getFileNameString(element: CaosScriptStringLike, newElementName: String): String {
//        CaosNotifications.showInfo(
//            myElement.project,
//            "Rename",
//            "New Name: $newElementName"
//        )
//
//        if (fileInfo == NO_EXTENSION) {
//            return getFileNameWithoutExtension(newElementName) ?: newElementName
//        }
//        if (isReferenceToJournal(element)) {
//            return newElementName
//        }
//        if (!isRelativePath(element)) {
//            return PathUtil.getLastPathComponent(newElementName) ?: newElementName
//        }
//        val newPath = CaosStringRenameFileProcessor.createPathFromElement(element, newElementName)
//            ?: return newElementName
//        val parentPath = element.virtualFile?.let { file ->
//            if (file.isDirectory) {
//                file
//            } else {
//                file.parent
//            }
//        } ?: return newElementName
//        return PathUtil.relativePath(parentPath.path, newPath) ?: newElementName
//    }

}


class CaosScriptQuoteStringReference(element: CaosScriptQuoteStringLiteral, range: TextRange) :
    CaosScriptStringLikeReference<CaosScriptQuoteStringLiteral>(element, range) {
    constructor(element: CaosScriptQuoteStringLiteral) : this(element, getStringNameRangeInString(element))

    override fun handleElementRename(newElementName: String): PsiElement {
        return handleElementRename(myElement, newElementName)
    }
}

class CaosScriptStringTextReference(element: CaosScriptStringText) :
    CaosScriptStringLikeReference<CaosScriptStringText>(element) {

    override fun handleElementRename(newElementName: String): PsiElement {
        val newName = if (myElement.parent is CaosScriptQuoteStringLiteral) {
            escapeQuotesInQuotedElement(newElementName)
        } else {
            newElementName
        }
        return handleElementRename(myElement, newName)
    }
}

class PrayQuoteStringReference(element: PrayString) : CaosScriptStringLikeReference<PrayString>(element) {
    override fun handleElementRename(newElementName: String): PsiElement {
        val newName = if (fileInfo == NEEDS_EXTENSION) {
            newElementName
        } else {
            getFileNameWithoutExtension(newElementName) ?: newElementName
        }
        return myElement.setName(newName)
    }
}

class CaosScriptCaos2ValueTokenReference(
    element: CaosScriptCaos2ValueToken,
    range: TextRange
) : CaosScriptStringLikeReference<CaosScriptCaos2ValueToken>(element, range) {

    constructor(element: CaosScriptCaos2ValueToken) : this(element, getStringNameRangeInString(element))

    override fun handleElementRename(newElementName: String): PsiElement {
        return handleElementRename(myElement, newElementName)
    }
}

const val NEEDS_EXTENSION = 1
const val NO_EXTENSION = 2

internal fun getStringNameRangeInString(element: PsiElement): TextRange {
    if (element.isInvalid) {
        return element.textRange
    }
    val text = element.text
    if (text.isEmpty()) {
        return TextRange(0, 0)
    }

    if (text.length == 1 && (text[0] == '"' || text[0] == '\'')) {
        return TextRange(0, 0)
    }

    val firstChar = text[0]
    val startQuote = if (firstChar == '"' || firstChar == '\'') {
        firstChar
    } else {
        null
    }

    val lastChar = text.last()

    val startIndex = if (startQuote != null) {
        1
    } else {
        0
    }
    val endOffset = if (startQuote != null && lastChar == firstChar) {
        1
    } else if (startQuote == null && (lastChar == '"' || lastChar == '\'')) {
        1
    } else {
        0
    }

    val endIndex = text.length - endOffset

    return TextRange(startIndex, endIndex)

}

private fun getJournalData(element: CaosScriptCompositeElement): Pair<String, Int>? {
    val quoteStringLiteral = (element as? CaosScriptQuoteStringLiteral)
        ?: ((element as? CaosScriptStringText)?.getParentOfType(CaosScriptQuoteStringLiteral::class.java))
        ?: return null

    val kind = quoteStringLiteral.stringStubKind
        ?: return null

    if (kind != StringStubKind.JOURNAL) {
        return null
    }
    val meta = quoteStringLiteral.meta
    return Pair(quoteStringLiteral.stringValue, meta)
}

private fun PrayTag.needsExtensionType(): Int {
    return if (tagName.lowercase() == "thumbnail") {
        NEEDS_EXTENSION
    } else {
        val (_, needsExtension) = tagRequiresFileOfType(tagName)
            ?: return 0
        if (needsExtension)
            NO_EXTENSION
        else {
            NEEDS_EXTENSION
        }
    }
}

private fun getFileInfoType(element: CaosScriptStringLike, parameterFileExtensions: Set<String>?): Int {
    val prayInputFileName = (element.parent as? PrayInputFileName) ?: (element.parent?.parent as? PrayInputFileName)
    if (prayInputFileName != null) {
        return when (prayInputFileName.parent) {
            is PrayInlineFile -> NEEDS_EXTENSION
            is PrayInlineText -> NEEDS_EXTENSION
            else -> 0
        }
    }
    val prayTagParent = (element.parent as? PrayTagValue ?: element.parent?.parent as? PrayTagValue)?.parent
    if (prayTagParent != null) {
        return when (prayTagParent) {
            is PrayTag -> prayTagParent.needsExtensionType()
            is CaosScriptCaos2Command -> NEEDS_EXTENSION
            else -> 0
        }
    }
    return when {
        parameterFileExtensions.isNotNullOrEmpty() -> NO_EXTENSION
        else -> 0
    }
}

private fun isCaos2FileString(element: PsiElement): Boolean {
    val command = element.getParentOfType(CaosScriptCaos2Command::class.java)
        ?.commandName
        ?.uppercase()
        ?: return false
    if (command == "DEPEND" || command == "LINK" || command == "ATTACH") {
        return true
    }
    val stringValue = element.getSelfOrParentOfType(CaosScriptStringLike::class.java)
        ?.stringValue
        ?: return false
    return if (command == "RSCR") {
        !isPossiblyCaos(stringValue)
    } else {
        false
    }
}


private fun isPrayFileString(element: PsiElement): Boolean {
    if (element.containingFile !is PrayFile) {
        return false
    }
    return element.hasParentOfType(PrayInputFileName::class.java)
}