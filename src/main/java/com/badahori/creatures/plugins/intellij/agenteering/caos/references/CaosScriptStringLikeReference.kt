package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineText
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInputFileName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayString
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.MAME
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType.NAME
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.notLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parentParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.toNavigableElement
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil

/**
 * Checks that a string literal is reference to other named game vars
 */
abstract class CaosScriptStringLikeReference<T : CaosScriptStringLike>(element: T) :
    PsiPolyVariantReferenceBase<T>(element, getStringNameRangeInString(element.text)) {

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

    private val fileInfo: Int by lazy {
        (element.parent as? PrayInputFileName ?: element.parent?.parent as? PrayInputFileName)
            ?.let {
                when (it.parent) {
                    is PrayInlineFile -> NEEDS_EXTENSION
                    is PrayInlineText -> NEEDS_EXTENSION
                    else -> 0
                }
            }
            ?: (element.parent as? PrayTagValue ?: element.parent?.parent as? PrayTagValue)
                ?.parent
                ?.let { parent ->
                    (parent as? PrayTag)?.let { tag ->
                        if (tag.tagName.lowercase() == "thumbnail") {
                            NEEDS_EXTENSION
                        } else {
                            tagRequiresFileOfType(tag.tagName)?.let {
                                if (it.second)
                                    NO_EXTENSION
                                else
                                    NEEDS_EXTENSION
                            } ?: 0
                        }
                    } ?: (if (parent is CaosScriptCaos2Command) NEEDS_EXTENSION else 0)
                }
            ?: (if (parameterFileExtensions != null) NO_EXTENSION else 0)
    }

    private val relative: Boolean by lazy {
        (element.parent is PrayInputFileName) ||
                (element.parent is PrayTagValue)
    }

    private val isCatalogueTag by lazy {
        if (valuesListName?.lowercase()?.equals("catalogue.tag") == true) {
            true
        } else {
            val inCatalogueCommand = containingCommand?.command?.uppercase() in listOf(
                "READ",
                "REAQ",
                "REAN"
            )
            inCatalogueCommand &&  parameter?.index == 0
        }
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

    private val parameterFileExtensions: List<String>? by lazy {
        val name = valuesListName
            ?: return@lazy null
        if (!name.startsWith("File.", ignoreCase = true))
            return@lazy null
        name.lowercase().substring(5).split('/')
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

    private val key: String by lazy {
        ((element.parent?.parent as? CaosScriptNamedGameVar)
            ?: (element.parent?.parent?.parent as? CaosScriptNamedGameVar))?.key ?: UNDEF
    }


    /**
     * Returns true for any matching var in project
     * TODO: Mask NAME and MAME by agent class
     */
    override fun isReferenceTo(anElement: PsiElement): Boolean {


        // Ensure that variables share the same variant,
        // otherwise the variables cannot be the same
        if (anElement.variant notLike myElement.variant) {
            return false
        }

        if (anElement is CatalogueItemName) {
            return isCatalogueTag && anElement.name.lowercase() == element.name?.lowercase()
        }

        if (anElement is CaosScriptQuoteStringLiteral && anElement.stringStubKind == StringStubKind.JOURNAL) {
            return isReferenceToJournal(anElement)
        }

        if (shouldResolveToFile) {
            return isReferenceToFile(anElement)
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
                return selfOnlyResult
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
            return resolveToNamedGameVars(project, variant)
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
        if (relative) {
            val file = myElement
                .resolveToFile(fileInfo == NO_EXTENSION, relative)
                ?.toNavigableElement(project)
                ?: return ResolveResult.EMPTY_ARRAY
            return PsiElementResolveResult.createResults(file)
        }

        val fileName = myElement.name

        // Get scope if any
        myElement.containingFile?.module?.moduleContentScope
        val scope = myElement.containingFile?.module?.moduleScope
            ?: GlobalSearchScope.projectScope(project)

        // Get all files matching name and possible extensions
        return parameterFileExtensions
            ?.flatMap { extension ->
                CaseInsensitiveFileIndex
                    .findWithFileName(
                        project,
                        "$fileName.$extension",
                        scope
                    )
                    .map {
                        it.toNavigableElement(project)
                    }
            }
            ?.nullIfEmpty()
            ?.let {
                PsiElementResolveResult.createResults(it)
            }
            ?: ResolveResult.EMPTY_ARRAY

    }

    private fun resolveToCatalogueTag(project: Project): Array<ResolveResult> {
        val name = myElement.stringValue
        val results = CatalogueEntryElementIndex.Instance[name, project]
            .nullIfEmpty()
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
                    val thisMeta = it.meta
                    meta == -1 || thisMeta == -1 || meta == thisMeta && journalName.equalsIgnoreCase(it.stringValue)
                } else {
                    false
                }
            }.distinctBy { it.textRange }
    }

    private fun resolveToNamedGameVars(project: Project, variant: CaosVariant): Array<ResolveResult> {

        // Only get results if named game var is not null
        val namedVarType = namedVarType
            ?: return ResolveResult.EMPTY_ARRAY

        // Find variables by type and name
        val references = if (namedVarType == MAME || namedVarType == NAME) {
            // MAME AND NAME resolve to the same variables
            // ...if TARG and OWNR are the same; OWNR and TARG classes are not resolved currently
            getNamed(variant, project, MAME) + getNamed(variant, project, NAME)
        } else {
            getNamed(variant, project, namedVarType)
        }

        return if (references.isEmpty()) {
            ResolveResult.EMPTY_ARRAY
        } else {
            PsiElementResolveResult.createResults(references.filter { it != myElement && it != myElement.parent })
        }
    }

    private fun getNamed(
        variant: CaosVariant,
        project: Project,
        type: CaosScriptNamedGameVarType,
    ): List<CaosScriptStringLike> {
        return CaosScriptNamedGameVarIndex.instance[type, key, project, CaosVariantGlobalSearchScope(project, variant)]
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
                }
            }
    }

    private fun isReferenceToFile(anElement: PsiElement): Boolean {
        if (anElement !is PsiFile)
            return false
        val otherVirtualFile = anElement.virtualFile
            ?: return false
        if (relative) {
            return myElement.resolveToFile(
                ignoreExtension = fileInfo == NO_EXTENSION,
                relative = relative
            )?.path == otherVirtualFile.path
        }
        if (anElement.name notLike myElement.name)
            return false
        val parameterExtensions = parameterFileExtensions
            ?: return false
        return (otherVirtualFile.extension in parameterExtensions)
    }

    private fun isReferenceToJournal(anElement: CaosScriptQuoteStringLiteral): Boolean {
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
}


class CaosScriptQuoteStringReference(element: CaosScriptQuoteStringLiteral) :
    CaosScriptStringLikeReference<CaosScriptQuoteStringLiteral>(element) {
    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}

class CaosScriptStringTextReference(element: CaosScriptStringText) :
    CaosScriptStringLikeReference<CaosScriptStringText>(element) {

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}

class PrayQuoteStringReference(element: PrayString) : CaosScriptStringLikeReference<PrayString>(element) {
    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}

class CaosScriptCaos2ValueTokenReference(element: CaosScriptCaos2ValueToken) :
    CaosScriptStringLikeReference<CaosScriptCaos2ValueToken>(element) {
    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}

const val NEEDS_EXTENSION = 1
const val NO_EXTENSION = 2

internal fun getStringNameRangeInString(text: String): TextRange {
    if (text.isEmpty())
        return TextRange(0, 0)
    val firstChar = text[0]
    val startQuote = if (firstChar == '"' || firstChar == '\'')
        firstChar
    else
        null
    if (startQuote == null || text.length == 1)
        return TextRange(0, text.length)

    val lastChar = text.last()
    val endOffset = if (lastChar == firstChar)
        1
    else
        0
    return TextRange(1, text.length - endOffset)
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