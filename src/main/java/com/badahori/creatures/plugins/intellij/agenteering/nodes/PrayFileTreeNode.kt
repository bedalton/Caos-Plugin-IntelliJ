package com.badahori.creatures.plugins.intellij.agenteering.nodes

import bedalton.creatures.pray.data.PrayBlock
import bedalton.creatures.pray.parser.parsePrayAgentBlocks
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.AgentScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getFileIcon
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import icons.CaosScriptIcons

internal class PrayFileTreeNode(
    project: Project,
    private val file: VirtualFile,
    private val viewSettings: ViewSettings,
) : VirtualFileBasedNode<VirtualFile>(project, file, viewSettings) {

    private val directory by lazy {
        val directory = CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory("PRAY")
        val name = file.name + '.' + randomString(6)
        directory.createChildDirectory(name)
    }

    /**
     * PRAY blocks in file
     */
    private val blocks: List<PrayBlock> by lazy {
        val stream = VirtualFileStreamReader(file)
        try {
            parsePrayAgentBlocks(stream, "*", true)
        } catch (e: Exception) {
            return@lazy emptyList()
        }
    }

    private val blockChildren by lazy {
        blocks.mapNotNull map@{ block ->
            if (block is PrayBlock.DataBlock) {
                val file = try {
                    directory.createChildWithContent(block.blockName.text, { block.data }, false).apply{
                        this.isWritable = false
                    }
                } catch (e: Exception) {
                    LOGGER.severe("Failed to get data for [${block.blockTag}]->${block.blockName}")
                    return@map null
                }
                GenericFileBasedNode(nonNullProject, file, viewSettings)
            } else {
                try {
                    PrayBlockTreeNode(project, directory, block, viewSettings)
                } catch (e: Exception) {
                    LOGGER.severe("Failed to get block tree node for [${block.blockTag}]->${block.blockName}")
                    e.printStackTrace()
                    return@lazy emptyList()
                }
            }
        }
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = file.name
        presentation.setIcon(getFileIcon(file.name, false) ?: CaosScriptIcons.AGENT_FILE_ICON)
    }

    override fun getChildren(): List<AbstractTreeNode<*>> {
        return blockChildren
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
            "AGNT", "DSAG", "MEDI", "SKIN", "EDAG", "EGGS" -> CaosScriptIcons.PRAY_AGENT_ICON
            else -> null
        }
        presentation.setIcon(icon)
    }


    private val childNodes: List<AbstractTreeNode<*>> by lazy {
        if (prayBlock !is PrayBlock.BlockWithTags) {
            return@lazy emptyList()
        }

        val isAgent = try {
            prayBlock.intTags.any { it.tag like "Agent Type" }
        } catch (e: Exception) {
            LOGGER.severe("Failed to parse block [${prayBlock.blockTag}]->${prayBlock.blockName}")
            e.printStackTrace()
            return@lazy emptyList()
        }

        if (!isAgent) {
            return@lazy emptyList()
        }
        val scriptTagRegex = "Script\\s+(\\d+)".toRegex()
        val variant = if (prayBlock.blockTag.text like "AGNT") {
            CaosVariant.C3
        } else {
            CaosVariant.DS
        }
        val stringTags = try {
            prayBlock.stringTags
        } catch (e: Exception) {
            emptyArray()
        }
        stringTags.filter {
            scriptTagRegex.matches(it.tag)
        }.map map@{
            val caosFile = try {
                AgentScript.createAgentCaosFile(
                    project,
                    it.tag,
                    directory,
                    variant,
                    it.value
                )
            } catch (_: Exception) {
                return@lazy emptyList()
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
            LOGGER.severe("Failed to get child nodes for [${prayBlock.blockTag}]->${prayBlock.blockName}")
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getWeight(): Int {
        return when (prayBlock.blockTag.text.uppercase()) {
            "AGNT", "DSAG", "MEDI", "SKIN", "EDAG", "EGGS" -> - 100
            else -> super.getWeight()
        }
    }

    override fun getLeafState(): LeafState {
        return if (prayBlock !is PrayBlock.BlockWithTags) {
            LeafState.ALWAYS
        } else {
            LeafState.ASYNC
        }
    }
}