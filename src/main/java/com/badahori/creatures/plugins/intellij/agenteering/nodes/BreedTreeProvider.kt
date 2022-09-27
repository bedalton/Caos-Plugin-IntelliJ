package com.badahori.creatures.plugins.intellij.agenteering.nodes

import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey.Companion.isPartName
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

class BreedTreeProvider : TreeStructureProvider {

    override fun modify(
        parentNode: AbstractTreeNode<*>,
        childNodes: Collection<AbstractTreeNode<*>>,
        viewSettings: ViewSettings?,
    ): Collection<AbstractTreeNode<*>> {

        // Create list for unaltered files
        val out = mutableListOf<AbstractTreeNode<*>>()

        // Map of breed files indexed by their breed part key
        val groups: MutableMap<BreedPartKey, MutableList<AbstractTreeNode<*>>> = mutableMapOf()

        // Ensure not null project
        val project = parentNode.project
            ?: return childNodes

        // Ensure project is not disposed first
        if (project.isDisposed) {
            return childNodes
        }

        if (!project.settings.combineAttNodes) {
            return childNodes
        }

        // Do not alter already altered nodes
        if (parentNode is BreedNode) {
            return childNodes
        }

        // Loop through children, grouping breed files if any
        for (childNode in childNodes) {
            val data = childNode.value

            val name = childNode.nameExtended

            if (name == null || !isPartName(name)) {
                out.add(childNode)
                continue
            }
            val isOkayFile = (data is PsiFile && !data.isDirectory) || (data is VirtualFile && !data.isDirectory)
            if (!isOkayFile) {
                out.add(childNode)
            }
            val key = BreedPartKey
                .fromFileName(name)
                ?.copyWithPart(null)
            if (key == null) {
                out.add(childNode)
                continue
            }
            if (!groups.containsKey(key)) {
                groups[key] = mutableListOf(childNode)
            } else {
                groups[key]!!.add(childNode)
            }
        }

        // Create the breed nodes using the map of breed keys to files
        val breedNodes: List<AbstractTreeNode<*>> = groups.mapNotNull map@{ (key, children) ->
            val extensions = children.mapNotNull { FileNameUtil.getExtension(it.nameExtended ?: "")?.uppercase() }
            if (extensions.filter { it == "ATT" }.size < 6 && extensions.filterNot { it == "ATT" }.size < 6 ) {
                out.addAll(children)
                return@map null
            }
            try {
                BreedNode(
                    project,
                    key,
                    children,
                    viewSettings
                )
            } catch (e: Exception) {
                LOGGER.severe("Failed to create breed node: ${e.message}")
                e.printStackTrace()
                null
            }
        }

        // Return the unaltered files and the breed nodes.
        return out + breedNodes
    }

    companion object {
        @Suppress("ComponentNotRegistered")
        class UseBreedNodeOptionAction : ToggleOptionAction({ event ->
            event.project?.let { it.settings.combineAttNodes = !it.settings.combineAttNodes }
            UseBreedNodeOption(event.project)
        })

        private class UseBreedNodeOption(private val project: Project?) : ToggleOptionAction.Option {

            override fun getName(): String {
                return "Combine Breed Nodes"
            }

            override fun getDescription(): String {
                return "Combine all breed files for a given genus, age and breed into a single project node"
            }

            override fun isSelected(): Boolean {
                return project?.settings?.combineAttNodes == true
            }

            override fun setSelected(selected: Boolean) {
                project?.settings?.combineAttNodes = selected
            }

        }
    }
}