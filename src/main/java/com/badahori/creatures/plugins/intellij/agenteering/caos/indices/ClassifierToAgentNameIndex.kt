@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import bedalton.creatures.common.util.className
import bedalton.creatures.common.util.nullIfEmpty
import bedalton.creatures.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.MyDataIndexer.Companion.DELIMITER
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileIndexUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
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

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        private const val VERSION = 1

        private val NAME: ID<String, String> = ID.create("bedalton.creatures.ClassifierToAgentNameIndex")

        val filter: FileBasedIndex.InputFilter by lazy {
            FileBasedIndex.InputFilter { file ->
                when (file.fileType) {
                    CaosScriptFileType.INSTANCE -> true
                    CatalogueFileType -> true
                    else -> false
                }
            }
        }

        val externalizer: DataExternalizer<String> by lazy {
            object : DataExternalizer<String> {
                override fun save(out: DataOutput, value: String?) {
                    out.writeUTF(value ?: "")
                }

                override fun read(`in`: DataInput): String {
                    return `in`.readUTF()
                }

            }
        }

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

        /**
         * Get agent names by Classifier values
         */
        private fun getAgentNamesEx(project: Project, classifier: String, scope: GlobalSearchScope? = null): List<String> {
            return ReadAction.compute<List<String>, Exception> {
                try {
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
                } catch (_: Exception) {
                    emptyList()
                }
            } as List<String>
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
            "^\\s*\\*\\s*([0-9][a-zA-Z_][a-zA-Z0-9 _]*|[a-zA-Z_][a-zA-Z0-9 _]*)(?:[-=:]?\\s*)(\\d+)\\s+(\\d+)\\s+(\\d+).*".toRegex()
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
                    if (it[4].trim().isNotBlank()) {
                        return Pair(makeKey(it[1], it[2], it[3]), it[4].trim())
                    }
                }
            nameBeforeClassifierRegex.matchEntire(comment)
                ?.groupValues
                ?.let {
                    if (it[1].trim().isNotBlank()) {
                        return Pair(makeKey(it[2], it[3], it[4]), it[1].trim())
                    }
                }
            return null
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