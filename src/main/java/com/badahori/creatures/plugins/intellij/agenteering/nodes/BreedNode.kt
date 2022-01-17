package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons

internal class BreedNode(
    project: Project?,
    key: BreedPartKey,
    private val files: List<VirtualFile>,
    private val viewSettings: ViewSettings?,
) : ProjectViewNode<BreedPartKey>(project, key.copyWithPart('*'), viewSettings) {

    private val key = key.copyWithPart(null)

    private val hasSprites by lazy {
        spriteExtensions.isNotEmpty()
    }

    private val hasAtts by lazy {
        files.any { it.extension?.lowercase() == "att" }
    }

    private val spriteExtensions by lazy {
        files.mapNotNull { it.extension?.uppercase() }.filter { it likeAny SpriteParser.VALID_SPRITE_EXTENSIONS }
    }

    private val tailText by lazy {
        if (hasSprites && hasAtts) {
            "ATT/" + spriteExtensions.joinToString("/")
        } else if (hasAtts) {
            "ATT"
        } else {
            spriteExtensions.joinToString("|")
        }
    }


    init {
        ensureValid()
    }

    override fun update(presentation: PresentationData) {
        if (virtualFile?.isValid == false) {
            return
        }
        presentation.setIcon(CaosScriptIcons.ATT_FILE_ICON)
        presentation.locationString = tailText
        presentation.presentableText = "*${key.code}"
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return files
            .mapNotNull map@{
                if (!it.isValid) {
                    return@map null
                }
                if (it.extension?.lowercase() in SpriteParser.VALID_SPRITE_EXTENSIONS) {
                    SpriteFileTreeNode(project!!, it, viewSettings)
                } else {
                    AttFileNode(project!!, it, viewSettings)
                }
            }
    }


    private fun ensureValid() {
        if (files.isEmpty()) {
            throw Exception("Cannot create body data bundle without files")
        }
        val invalidFiles = files.filterNot { file ->
           file.extension?.uppercase()?.let { it likeAny spriteExtensions || it == "ATT" }.orFalse()
        }
        if (invalidFiles.isNotEmpty()) {
            throw Exception("Invalid file(s) body files found. Expected ${spriteExtensions.joinToString(", ")} and ATT files only")
        }
        val valid = files.all { file ->
            BreedPartKey.isGenericMatch(BreedPartKey.fromFileName(file.name), key)
        }
        if (!valid) {
            throw Exception("Mismatched body data files found in bundle")
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return file in files
    }

    override fun canNavigateToSource(): Boolean {
        return false
    }

    override fun getName(): String {
        return "*${key.code}"
    }
}

internal class AttFileNode(
    project: Project,
    private val att: VirtualFile,
    viewSettings: ViewSettings?,
) : VirtualFileBasedNode<VirtualFile>(project, att, viewSettings) {
    override fun update(presentation: PresentationData) {
        if (!att.isValid) {
            return
        }
        presentation.setIcon(CaosScriptIcons.ATT_FILE_ICON)
        presentation.presentableText = att.name
    }

    override fun getChildren(): Collection<AbstractTreeNode<Any>> {
        return emptyList()
    }

}