package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

import com.bedalton.creatures.agents.pray.compiler.pray.CATEGORIES
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayElement
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayGroupKw
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.Caos2CompletionProvider
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.InsertInsideQuoteHandler
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.quoter
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTagName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTagValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext


class PrayCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), PrayCompletionProvider)
    }

}

private val quoteInsertHandler = InsertInsideQuoteHandler('"', '"')

object PrayCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet,
    ) {
        val element = parameters.position

        if (element.containingFile !is PrayFile) {
            return
        }

        val prayTagName = element.getSelfOrParentOfType(PrayTagName::class.java)
        if (prayTagName != null) {

            val parentAgent = prayTagName.getParentOfType(PrayAgentBlock::class.java)
                ?: return
            val tags = parentAgent.tagStructs
            Caos2CompletionProvider.addCaos2PrayTagCompletions(
                resultSet,
                tags,
                prayTagName,
                case = null,
                isCaosScriptFile = false,
                eggs = parentAgent.blockTagString == "EGGS"
            )
            getPrayTagCompletions(tags).forEach {
                resultSet.addElement(
                    LookupElementBuilder.create(it)
                        .withInsertHandler(quoteInsertHandler)
                        .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
                )
            }
            return
        }
        var previous = element.getPreviousNonEmptySibling(false)
        if (!element.text.startsWith('"')) {
            if (previous?.lineNumber?.let { it != element.lineNumber } == true) {
                for (tag in listOf("group", "inline")) {
                    resultSet.addElement(
                        LookupElementBuilder.create(tag)
                            .withLookupStrings(
                                listOf(
                                    tag, tag.upperCaseFirstLetter(), tag.uppercase()
                                )
                            )
                    )
                }
            } else if (previous is PrayGroupKw) {
                val tags = if (parameters.isExtendedCompletion) {
                    listOf("AGNT", "DSAG", "EGGS", "DSGB")
                } else {
                    listOf("AGNT", "DSAG", "EGGS")
                }
                for (tag in tags) {
                    resultSet.addElement(
                        LookupElementBuilder.create(tag)
                            .withLookupStrings(
                                listOf(
                                    tag, tag.upperCaseFirstLetter(), tag.lowercase()
                                )
                            )
                            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
                    )
                }
            }
        }
        if (previous is PrayElement) {
            previous = PsiTreeUtil.findElementOfClassAtOffset(
                element.containingFile,
                previous.endOffset - 1,
                PrayTagName::class.java,
                false
            )
        }

        val tagValueTag = (previous as? PrayTagName)?.stringValue ?: previous?.getParentOfType(PrayTag::class.java)
            ?.tagName
        if (tagValueTag != null) {
            addPrayValueCompletions(resultSet, tagValueTag)
        }
        val tagValue = element.getSelfOrParentOfType(PrayTagValue::class.java)
        if (tagValue != null) {
            val text = tagValue.text
            val quoter = quoter(text)
            val parentDirectory = element.containingFile?.directory
                ?: return
            Caos2CompletionProvider.addCaos2PrayFileNameCompletions(
                variant = element.variant ?: CaosVariant.DS,
                resultSet,
                parameters.isExtendedCompletion,
                parentDirectory,
                tagValue,
                quoter
            )
        }
    }
}


private fun getPrayTagCompletions(tags: List<PrayTagStruct<*>>): List<String> {
    val scriptTags = getCountedCompletions(tags, "Script Count", PrayTags.SCRIPT_TAG_FUZZY, "Script")
    val dependencyTags = getCountedCompletions(tags, "Dependency Count", PrayTags.DEPENDENCY_TAG_FUZZY, "Dependency")
    val dependencyCategoryTags =
        getCountedCompletions(tags, "Dependency Count", PrayTags.DEPENDENCY_CATEGORY_TAG_FUZZY, "Dependency Category")
    return (scriptTags + dependencyTags + dependencyCategoryTags).distinct()
}


private fun getCountedCompletions(
    tags: List<PrayTagStruct<*>>,
    countTag: String,
    childrenRegex: Regex,
    prefix: String,
): List<String> {
    val out = mutableListOf<String>()
    val children = tags
        .mapNotNull { tag ->
            childrenRegex
                .matchEntire(tag.tag)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntSafe()
        }
        .distinct()
    val count = (tags.firstOrNull {
        it.tag like countTag
    } as? PrayTagStruct.IntTag)?.value

    if (count != null) {
        val suggestedChildren = (1..count).filter { it !in children }.map { "$prefix $it" }
        out.addAll(suggestedChildren)
    } else {
        out.add(countTag)
    }

    out.add("$prefix ${(children.maxOrNull() ?: 0) + 1}")
    return out
}

private fun addPrayValueCompletions(resultSet: CompletionResultSet, tagName: String) {

    if (PrayTags.DEPENDENCY_CATEGORY_TAG_FUZZY.matches(tagName)) {
        CATEGORIES.forEach {
            resultSet.addElement(
                LookupElementBuilder.create(it.key)
                    .withPresentableText("${it.key} - ${it.value}")
                    .withLookupString("${it.key} - ${it.value}")
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
            )
        }
    }
}