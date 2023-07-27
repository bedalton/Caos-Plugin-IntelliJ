package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey.Companion.isPartName
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.PathUtil.getExtension
import com.bedalton.common.util.nullIfEmpty
import com.bedalton.creatures.common.structs.BreedKey
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.ToggleOptionAction
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
        val groups: MutableMap<BreedKey, MutableList<AbstractTreeNode<*>>> = mutableMapOf()
        val groupsBySlot = mutableMapOf<BreedKey, MutableList<BreedNode>>()

        // Ensure not null project
        val project = parentNode.project
            ?: return childNodes

        // Ensure project is not disposed first
        if (project.isDisposed) {
            return childNodes
        }

        val settings = CaosApplicationSettingsService.getInstance()

        if (!settings.combineAttNodes) {
            return childNodes
        }

        // Do not alter already altered nodes
        if (parentNode is BreedNode || parentNode is BreedNodeBySlot) {
            return childNodes
        }

        // Loop through children, grouping breed files if any
        for (childNode in childNodes) {
            if (childNode is BreedNode) {
                val key = childNode.key.copyWithAgeGroup(null)
                if (groups.containsKey(key)) {
                    groupsBySlot[key]!!.add(childNode)
                } else {
                    groupsBySlot[key] = mutableListOf(childNode)
                }
                continue
            }
            val data = childNode.value

            val name = childNode.nameExtended ?: childNode.name

            if (name == null) {
                out.add(childNode)
                continue
            }

            // Does not have breed file name
            if (!isPartName(name) && !childNode.isBreedNodeName()) {
                out.add(childNode)
                continue
            }

            // Ensure if file not directory
            val isDirectory = (data is PsiFile && data.isDirectory) || (data is VirtualFile && data.isDirectory)
            if (isDirectory) {
                out.add(childNode)
                continue
            }

            // Ensure has breed
            val key = BreedKey.fromFileName(name)
                ?: childNode.breedKey()
            if (key == null) {
                out.add(childNode)
                continue
            }

            if (groups.containsKey(key)) {
                groups[key]!!.add(childNode)
            } else {
                groups[key] = mutableListOf(childNode)
            }
        }

        // Create the breed nodes using the map of breed keys to files
        val breedNodes: List<BreedNode> = groups.mapNotNull map@{ (key, children) ->
            val extensionsForEachFile = children
                .mapNotNull {
                    getExtension(it.nameExtended ?: "")
                        ?.uppercase()
                }
            if (extensionsForEachFile.distinct().filter { it != "ATT" }.size > 2) {
                out.addAll(children)
                return@map null
            }
            try {
                BreedNode(
                    project,
                    parentNode.possibleVirtualFile,
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

        if (settings.combineAttNodesBySlot) {
            for (breedNode in breedNodes) {
                val key = breedNode.key.copyWithAgeGroup(null)
                if (!key.isConcrete()) {
                    out.add(breedNode)
                    continue
                }
                if (groupsBySlot.containsKey(key)) {
                    groupsBySlot[key]!!.add(breedNode)
                } else {
                    groupsBySlot[key] = mutableListOf(breedNode)
                }
            }
            for ((key, children) in groupsBySlot) {
                out.add(
                    BreedNodeBySlot(
                        project,
                        parentNode.possibleVirtualFile,
                        key,
                        nodes = children,
                        viewSettings = viewSettings
                    )
                )
            }
        } else {
            out.addAll(breedNodes)
        }

        // Return the unaltered files and the breed nodes.
        return out
    }

    companion object {



    }
}

@Suppress("ComponentNotRegistered")
class UseBreedNodeOptionAction : ToggleOptionAction({ event ->
    event.project?.let {
        CaosApplicationSettingsService.getInstance().combineAttNodes =
            !CaosApplicationSettingsService.getInstance().combineAttNodes
    }
    UseBreedNodeOption()
})

@Suppress("ComponentNotRegistered")
class UseBreedCombineNodeOptionAction : ToggleOptionAction({ event ->
    event.project?.let {
        CaosApplicationSettingsService.getInstance().combineAttNodes =
            !CaosApplicationSettingsService.getInstance().combineAttNodes
    }
    UseBreedNodeOption()
})

private class UseBreedNodeOption : ToggleOptionAction.Option {

    override fun getName(): String {
        return "Combine Breed Nodes"
    }

    override fun getDescription(): String {
        return "Combine all breed files for a given genus, age and breed into a single project node"
    }

    override fun isSelected(): Boolean {
        return CaosApplicationSettingsService.getInstance().combineAttNodes
    }

    override fun setSelected(selected: Boolean) {
        CaosApplicationSettingsService.getInstance().combineAttNodes = selected
    }

}

private fun BreedKey.isConcrete(): Boolean {
    return genus in 0..3 &&
            gender in 1..2 &&
            (breed in '0'..'9' || breed in 'a'..'z' || breed in 'A'..'Z')
}

private fun AbstractTreeNode<*>.isBreedNodeName(): Boolean {
    val name = PathUtil.getFileNameWithoutExtension(name ?: "")
        ?.nullIfEmpty()
        ?.lowercase()
        ?: return false
    if (name.length != 4) {
        return false
    }
    if (name[0] != '*') {
        return false
    }
    if (name[1] !in '0'..'7') {
        return false
    }
    if (name[2] !in '0'..'7') {
        return false
    }
    return name[3] in 'a'..'z' || name[3] in '0'..'9'
}

private fun AbstractTreeNode<*>.breedKey(): BreedKey? {
    var name = when (val value = this.value) {
        is PsiFile -> value.name.nullIfEmpty()
        is VirtualFile -> value.name.nullIfEmpty()
        else -> name
    } ?: return null
    if (name.startsWith('*')) {
        val nameWithoutExtension = PathUtil.getFileNameWithoutExtension(name)
            ?: return null
        if (nameWithoutExtension.length != 4) {
            return null
        }
        name = 'a' + nameWithoutExtension.substring(1)
    }
    return if (BreedKey.isPartName(name)) {
        BreedKey.fromFileName(name)
    } else {
        return null
    }
}