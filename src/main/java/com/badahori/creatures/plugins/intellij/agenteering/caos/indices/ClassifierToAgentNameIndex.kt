@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.MyDataIndexer.Companion.DELIMITER
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileIndexUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.bedalton.common.exceptions.rethrowCancellationException
import com.bedalton.common.util.*
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class ClassifierToAgentNameIndex : FileBasedIndexExtension<String, String>() {

    private val indexer by lazy {
        MyDataIndexer()
    }

    override fun getName(): ID<String, String> = NAME

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return indexer
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor.INSTANCE
    }

    override fun getValueExternalizer(): DataExternalizer<String> {
        return externalizer
    }

    override fun getVersion(): Int {
        return VERSION + MyDataIndexer.VERSION
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return filter
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }
}


private const val VERSION = 1

private val NAME: ID<String, String> = ID.create("bedalton.creatures.ClassifierToAgentNameIndex")

private val filter: FileBasedIndex.InputFilter by lazy {
    FileBasedIndex.InputFilter { file ->
        when (file.fileType) {
            CaosScriptFileType.INSTANCE -> true
            CatalogueFileType -> true
            else -> false
        }
    }
}

private val externalizer: DataExternalizer<String> by lazy {
    object : DataExternalizer<String> {
        override fun save(out: DataOutput, value: String?) {
            out.writeUTF(value ?: "")
        }

        override fun read(`in`: DataInput): String {
            return `in`.readUTF()
        }

    }
}

@Suppress("RegExpUnnecessaryNonCapturingGroup")
private class MyDataIndexer : DataIndexer<String, String, FileContent> {

    override fun map(inputData: FileContent): Map<String, String> {
        return when (inputData.fileType) {
            CaosScriptFileType.INSTANCE -> {
                getAgentsInCaosComments(inputData.contentAsText.toString())
            }

            CatalogueFileType -> {
                getAgentsInCatalogue(inputData.contentAsText.toString())
            }

            else -> {
                emptyMap()
            }
        }
    }

    companion object {

        const val VERSION = 0

        val agentHelpTag =
            "^\\s*TAG\\s+\"Agent\\s+Help\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*\"\\s*".toRegex(RegexOption.IGNORE_CASE)
        val classifierBeforeNameRegex =
            "^\\s*\\*\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(?:[-=:]?\\s*)([a-zA-Z0-9 _]+).*".toRegex()
        val nameBeforeClassifierRegex =
            "^\\s*\\*\\s*([0-9][a-zA-Z_\\-][a-zA-Z0-9 _\\-]*|[a-zA-Z_][a-zA-Z0-9 _\\-]*)(?:[-=:]?\\s*)(\\d+)\\s+(\\d+)\\s+(\\d+).*".toRegex()
        const val DELIMITER = ' '

        fun makeKey(family: Int, genus: Int, species: Int): String {
            return makeKey(family.toString(), genus.toString(), species.toString())
        }

        fun makeKey(family: String, genus: String, species: String): String {
            return "$family$DELIMITER$genus$DELIMITER$species"
        }


        private fun getAgentsInCaosComments(text: String): Map<String, String> {
            return text.split("\r?\n".toRegex())
                .filter { it.trim().startsWith('*') && !it.trim().startsWith("*#") }
                .mapNotNull { comment ->
                    commentToClassifierAndName(comment)
                }.toMap()
        }

        private fun commentToClassifierAndName(comment: String): Pair<String, String>? {
            classifierBeforeNameRegex.matchEntire(comment)
                ?.groupValues
                ?.let {
                    val name = it[4].trim()
                    if (isValidClassifierName(name)) {
                        return makeKeyNamePair(it[1], it[2], it[3], it[4])
                    }
                }
            nameBeforeClassifierRegex.matchEntire(comment)
                ?.groupValues
                ?.let {
                    return makeKeyNamePair(it[2], it[3], it[4], it[1])
                }
            return null
        }

        private fun processName(name: String): String {
            val normalized = name
                .trim()
                .replace("(agent\\s*)?(class|classifier|clas)".toRegex(RegexOption.IGNORE_CASE), "")
                .trim()

            return normalized
        }

        private fun makeKeyNamePair(family: String, genus: String, species: String, name: String): Pair<String, String>? {
            if (isValidClassifierName(name)) {
                return Pair(makeKey(family, genus, species), processName(name))
            }
            return null
        }

        private fun isValidClassifierName(name: String): Boolean {
            var nameNormalized = name
                .trim()
                .lowercase()

            if (name.startsWith("agent")) {
                nameNormalized = name.replace("agent", "")
            }

            if (nameNormalized.isBlank()) {
                return false
            }


            val invalidNameRegex = "(agent\\s*)?(clas|classifier|class)"
                .toRegex(RegexOption.IGNORE_CASE)

            return !invalidNameRegex.matches(nameNormalized)
        }

        private fun getAgentsInCatalogue(text: String): Map<String, String> {
            val lines = text.split("\r?\n".toRegex())
                .filter { it.isNotBlank() }
            val out = mutableMapOf<String, String>()
            var currentClassifier: String? = null
            for (line in lines) {
                if (currentClassifier != null) {
                    val name = line.stripSurroundingQuotes(1)
                        .trim()
                        .nullIfEmpty()
                    if (name != null) {
                        out[currentClassifier] = name
                    }
                    currentClassifier = null
                    continue
                }
                if (!line.uppercase().startsWith("TAG")) {
                    continue
                }
                val c = agentHelpTag.matchEntire(line)
                    ?.groupValues
                    ?: continue
                currentClassifier = makeKey(c[1], c[2], c[3])
            }
            return out
        }

    }
}

internal object ClassifierToAgentNameHelper {

    /**
     * Get agent names by Classifier values
     */
    fun getAgentNames(
        project: Project,
        family: Int,
        genus: Int,
        species: Int,
        scope: GlobalSearchScope? = null
    ): List<String> {
        return getAgentNamesEx(project, MyDataIndexer.makeKey(family, genus, species), scope)
    }

    /**
     * Get all classifiers in project
     */
    fun getAllClassifiers(project: Project, scope: GlobalSearchScope? = null): List<Pair<String, String>> {
        return ReadAction.compute<List<Pair<String,String>>, Exception> {
            try {
                var projectScope = GlobalSearchScope.projectScope(project)
                if (scope != null) {
                    projectScope = projectScope.intersectWith(scope)
                }
                FileIndexUtil.getKeysAndValues(NAME, projectScope)
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                e.rethrowCancellationException()
                LOGGER.severe("Failed to get all classifiers; ${e.className}: ${e.message}")
                emptyList()
            }
        }
    }

    fun getAgentNames(project: Project, classifier: String, scope: GlobalSearchScope? = null): List<String> {
        val classifierFormatted = classifier.trim().replace("\\s+".toRegex(), "" + DELIMITER)
        if (classifier.count { it == DELIMITER } != 2) {
            LOGGER.severe("Malformed classifier string: <$classifier>")
            return emptyList()
        }
        return getAgentNamesEx(project, classifierFormatted, scope)
    }
}

fun getAgentNames(project: Project, classifier: String, scope: GlobalSearchScope? = null): List<String> {
    val classifierFormatted = classifier.trim().replace("\\s+".toRegex(), "" + DELIMITER)
    if (classifier.count { it == DELIMITER } != 2) {
        LOGGER.severe("Malformed classifier string: <$classifier>")
        return emptyList()
    }
    return getAgentNamesEx(project, classifierFormatted, scope)
}

/**
 * Get agent names by Classifier values
 */
private fun getAgentNamesEx(project: Project, classifier: String, scope: GlobalSearchScope? = null): List<String> {
    return ReadAction.compute<List<String>, Exception> {
        try {
            if (project.isDisposed) {
                return@compute emptyList()
            }

            var projectScope = GlobalSearchScope.projectScope(project)

            if (scope != null) {
                projectScope = projectScope.intersectWith(scope)
            }

            FileBasedIndex.getInstance()
                .getValues(
                    NAME,
                    classifier,
                    projectScope
                )

        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            e.rethrowCancellationException()
            emptyList()
        }
    } as List<String>
}