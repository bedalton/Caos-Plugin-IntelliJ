package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineText
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInputFileName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayString
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptNamedGameVarIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.notLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.caos.scopes.CaosVariantGlobalSearchScope
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.toNavigableElement
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

/**
 * Checks that a string literal is reference to other named game vars
 */
abstract class CaosScriptStringLikeReference<T : CaosScriptStringLike>(element: T) :
    PsiPolyVariantReferenceBase<T>(element, getRange(element.text)) {

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

    private val parameterFileExtensions by lazy {
        val variant = variant
            ?: return@lazy null
        val name = parameter
            ?.valuesList
            ?.get(variant)
            ?.name
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
        val parameters = containingCommand
            ?.parameters
            ?.nullIfEmpty()
            ?: return@lazy null
        val index = parentArgument?.index
            ?: return@lazy null

        if (index < 0)
            null
        else
            parameters.getOrNull(index)
    }

    private val type: CaosScriptNamedGameVarType by lazy {
        ((element.parent?.parent as? CaosScriptNamedGameVar)
            ?: (element.parent?.parent?.parent as? CaosScriptNamedGameVar))?.varType?.let {
            return@lazy it
        }

        val command = element.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return@lazy CaosScriptNamedGameVarType.UNDEF
        when (command.commandStringUpper) {
            "DELG" -> CaosScriptNamedGameVarType.GAME
            "DELE" -> CaosScriptNamedGameVarType.EAME
            "DELN" -> CaosScriptNamedGameVarType.NAME
            else -> CaosScriptNamedGameVarType.UNDEF
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
    override fun isReferenceTo(element: PsiElement): Boolean {


        // Ensure that variables share the same variant,
        // otherwise the variables cannot be the same
        if (element.variant notLike myElement.variant)
            return false

        if (shouldResolveToFile) {
            if (element !is PsiFile)
                return false
            val otherVirtualFile = element.virtualFile
                ?: return false
            if (relative)
                return myElement.resolveToFile(ignoreExtension = fileInfo == NO_EXTENSION,
                    relative = relative)?.path == otherVirtualFile.path
            if (element.name notLike myElement.name)
                return false
            val parameterExtensions = parameterFileExtensions
                ?: return false
            return (otherVirtualFile.extension in parameterExtensions)
        }


        // Ensure other is or has named game var parent element
        val namedGameVarParent = (element.parent?.parent as? CaosScriptNamedGameVar
            ?: element.parent?.parent?.parent as? CaosScriptNamedGameVar)
            ?: return false
        // Check that type and key are the same
        return namedGameVarParent.varType == type && namedGameVarParent.key == key
    }

    /**
     * Resolves to itself to prevent weird ctrl+click behavior
     */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project

        if (project.isDisposed) {
            return ResolveResult.EMPTY_ARRAY
        }

        if (DumbService.isDumb(project))
            return selfOnlyResult

        if (shouldResolveToFile) {
            if (relative) {
                val file = myElement
                    .resolveToFile(fileInfo == NO_EXTENSION, relative)
                    ?.toNavigableElement(project)
                    ?: return ResolveResult.EMPTY_ARRAY
                return PsiElementResolveResult.createResults(file)
            }

            val fileName = myElement.name
            myElement.containingFile?.module?.moduleContentScope
            val scope = myElement.containingFile?.module?.moduleScope
                ?: GlobalSearchScope.projectScope(project)
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


        val variant = variant
            ?: return selfOnlyResult
        val references = if (type == CaosScriptNamedGameVarType.MAME || type == CaosScriptNamedGameVarType.NAME) {
            getNamed(variant, project, CaosScriptNamedGameVarType.MAME) +
                    getNamed(variant, project, CaosScriptNamedGameVarType.NAME)
        } else
            getNamed(variant, project, type)
        if (references.isEmpty())
            return PsiElementResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(references.filter { it != myElement && it != myElement.parent })
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
                    if (stringLiteral.isValid)
                        stringLiteral
                    else
                        null
                }
            }
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

private fun getRange(text: String): TextRange {
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