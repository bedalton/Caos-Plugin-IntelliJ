package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.bedalton.creatures.agents.pray.data.BlockWithTags
import com.bedalton.creatures.agents.pray.data.DataBlock
import com.bedalton.creatures.agents.pray.data.PrayBlock
import com.bedalton.creatures.agents.pray.parser.parsePrayAgentBlocks
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.bedalton.creatures.agents.pray.data.PrayDataBlockDecompressionException
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.bedalton.common.util.className
import com.bedalton.creatures.agents.pray.compiler.getLinkFilenameReferenceFromCaosText
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons
import kotlinx.coroutines.runBlocking

private val PRAY_TREE_CHILD_CACHE_KEY = Key<Pair<String, List<AbstractTreeNode<*>>>>("bedalton.node.pray.PRAY_CHILDREN")

private var gzipError = false

internal class PrayFileTreeNode(
    project: Project,
    private val file: VirtualFile,
    private val viewSettings: ViewSettings,
) : VirtualFileBasedNode<VirtualFile>(project, file, viewSettings) {


    private val directory by lazy {
        val baseDirectory = CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory("PRAY")
        val directory = baseDirectory.createChildDirectory(randomString(6))
        val name = file.name
        directory.createChildDirectory(name)
    }

    /**
     * PRAY blocks in file
     */
    private val blocks: List<PrayBlock> by lazy {
        val stream = VirtualFileStreamReader(file)
        runBlocking {
            try {
                parsePrayAgentBlocks(stream, "*")
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                return@runBlocking emptyList()
            }
        }
    }

    private val blockChildren by lazy {
        val md5 = virtualFile.md5()
        virtualFile.getUserData(PRAY_TREE_CHILD_CACHE_KEY)?.let {
            if (it.first == md5) {
                return@lazy it.second
            }
        }
        blocks.mapNotNull map@{ block ->
            if (block is DataBlock) {
                val name = block.blockName.text
                val file = directory[name] ?: (
                        try {
                            val data: suspend () -> ByteArray? = {
                                try {
                                    block.data()
                                } catch (e: PrayDataBlockDecompressionException) {
                                    if (!gzipError) {
                                        gzipError = true
                                        CaosNotifications.createErrorNotification(
                                            project,
                                            "Unpack Error",
                                            "Failed to extract data for ${block.blockName}; ${e.message}"
                                        )
                                            .show()
                                    }
                                    null
                                }
                            }
                            directory.createChildWithContent(name, data, false).apply {
                                this.isWritable = false
                            }
                        } catch (e: Exception) {
                            if (e is ProcessCanceledException) {
                                throw e
                            }
                            LOGGER.severe("Failed to get data for [${block.blockTag}]->${block.blockName}; ${e.className}: ${e.message}")
                            return@map null
                        })
                GenericFileBasedNode(nonNullProject, file, viewSettings)
            } else {
                try {
                    PrayBlockTreeNode(project, directory, block, viewSettings)
                } catch (e: Exception) {
                    if (e is ProcessCanceledException) {
                        throw e
                    }
                    LOGGER.severe("Failed to get block tree node for [${block.blockTag}]->${block.blockName}; ${e.className}: ${e.message}")
                    e.printStackTrace()
                    return@lazy emptyList()
                }
            }
        }.apply {
            virtualFile.putUserData(PRAY_TREE_CHILD_CACHE_KEY, Pair(md5 ?: "", this))
        }
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = file.name
        presentation.setIcon(getFileIcon(file.name, false) ?: CaosScriptIcons.AGENT_FILE_ICON)
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        return blockChildren
    }

    override fun getLeafState(): LeafState {
        return LeafState.ASYNC
    }

}


internal class PrayBlockTreeNode(
    project: Project,
    private val parentDirectory: CaosVirtualFile,
    private val prayBlock: PrayBlock,
    private val viewSettings: ViewSettings,
) : AbstractTreeNode<PrayBlock>(project, prayBlock) {

    private val directory: CaosVirtualFile by lazy {
        parentDirectory.createChildDirectory(prayBlock.blockTag.text + '.' + prayBlock.blockName.text)
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = prayBlock.blockName.text
        presentation.locationString = "(${prayBlock.blockTag.text})"
        val icon = when (prayBlock.blockTag.text) {
            "AGNT", "DSAG", "EGGS", "DSGB" -> CaosScriptIcons.PRAY_AGENT_ICON
            else -> null
        }
        presentation.setIcon(icon)
    }


    private val childNodes: List<AbstractTreeNode<*>> by lazy {
        if (prayBlock !is BlockWithTags) {
            return@lazy emptyList()
        }

        val isAgent = try {
            runBlocking {
                prayBlock.intTags.any { it.tag like "Agent Type" } || prayBlock.stringTags
                    .any { it.tag like "Agent Type" }
            }
        } catch (e: Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            LOGGER.severe("Failed to parse block [${prayBlock.blockTag}]->${prayBlock.blockName}")
            e.printStackTrace()
            return@lazy emptyList()
        }

        if (!isAgent) {
            return@lazy emptyList()
        }
        val scriptTagRegex = "Script\\s+(\\d+)|Remove script".toRegex(RegexOption.IGNORE_CASE)
        val variant = if (prayBlock.blockTag.text like "AGNT") {
            CaosVariant.C3
        } else {
            CaosVariant.DS
        }
        val stringTags = runBlocking {
            try {
                prayBlock.stringTags
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                emptyArray()
            }
        }
        stringTags.filter {
            scriptTagRegex.matches(it.tag)
        }.mapNotNull map@{
            val caosFile = try {
                val text = it.value
                val filename = getLinkFilenameReferenceFromCaosText(text)?.let { fileName ->
                    fileName + " (${it.tag})"
                } ?: it.tag
                AgentScript.createAgentCaosFile(
                    project,
                    filename,
                    directory,
                    variant,
                    text
                )
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                LOGGER.severe("Failed to create agent CAOS file; ${e.className}: ${e.message};\n${e.stackTrace}")
                return@map null
            }
            ChildCaosScriptFileTreeNode(
                project,
                prayBlock.blockName.text,
                caosFile,
                scriptTagRegex.matchEntire(it.tag)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                it.tag,
                viewSettings
            )
        }
    }


    override fun getChildren(): List<AbstractTreeNode<*>> {
        return try {
            childNodes
        } catch (e: Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            LOGGER.severe("Failed to get child nodes for [${prayBlock.blockTag}]->${prayBlock.blockName}")
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getWeight(): Int {
        return when (prayBlock.blockTag.text.uppercase()) {
            "AGNT", "DSAG", "EGGS", "DSGB" -> -100
            else -> super.getWeight()
        }
    }

    override fun getLeafState(): LeafState {
        return if (prayBlock !is BlockWithTags) {
            LeafState.ALWAYS
        } else {
            LeafState.ASYNC
        }
    }
}