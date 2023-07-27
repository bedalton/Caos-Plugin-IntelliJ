@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualDirectory
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.capitalize
import com.bedalton.creatures.common.structs.BreedKey
import com.bedalton.creatures.common.util.getGenusString
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons

internal class BreedNodeBySlot(
    private val notNullProject: Project,
    val parent: VirtualFile?,
    private val key: BreedKey,
    private val nodes: List<BreedNode>,
    private val virtualFiles: List<VirtualFile> = nodes.flatMap { it.children }.virtualFiles,
    viewSettings: ViewSettings?,
) : VirtualFileBasedNode<CaosVirtualDirectory>(notNullProject, CaosVirtualDirectory(notNullProject, parent, name(key), nodes.map { it.virtualFile }), viewSettings) {

    private val hasSprites by lazy {
        spriteExtensions.isNotEmpty()
    }

    private val hasAtts by lazy {
        nodes.any { PathUtil.getExtension(it.nameExtended ?: "")?.lowercase() == "att" }
    }

    private val spriteExtensions by lazy {
        nodes.mapNotNull { PathUtil.getExtension(it.nameExtended ?: "")?.uppercase() }
            .filter { it likeAny SpriteParser.VALID_SPRITE_EXTENSIONS }
            .toSet()
    }

    private val tailText by lazy {
        if (hasSprites && hasAtts) {
            "ATT|" + spriteExtensions.joinToString("|")
        } else if (hasAtts) {
            "ATT"
        } else {
            spriteExtensions.joinToString("|")
        }
    }


    init {
        ensureValid()
    }

    internal val mName: String by lazy {
        val genus = getGenusString(key.genus!!)!!.capitalize()
        val slot = key.breed?.uppercase()
            ?: throw IllegalStateException("Key should have a valid breed slot for BreedNodeBySlot node")
        val gender = when (key.gender) {
            1 -> "M"
            2 -> "F"
            else -> throw IllegalStateException("Key should have a valid gender for BreedNodeBySlot node")
        }
        "$genus $slot ($gender)"
    }

    override fun update(presentation: PresentationData) {
        if (notNullProject.isDisposed || virtualFiles.any { !it.isValid }) {
            return
        }
        presentation.presentableText = name
        val icon = if (hasAtts) {
            CaosScriptIcons.ATT_FILE_ICON
        } else {
            when(spriteExtensions.firstOrNull()) {
                "SPR" -> CaosScriptIcons.SPR_FILE_ICON
                "S16" -> CaosScriptIcons.S16_FILE_ICON
                "C16" -> CaosScriptIcons.C16_FILE_ICON
                else -> null
            }
        }
        presentation.setIcon(icon)
        presentation.locationString = tailText
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return nodes
    }


    private fun ensureValid() {
        if (notNullProject.isDisposed) {
            throw Exception("Cannot create breed file bundle. Project is disposed")
        }
        if (nodes.isEmpty()) {
            throw Exception("Cannot create body data bundle without files")
        }
        val invalidFiles = virtualFiles.filterNot { file ->
            file.extension?.uppercase()?.let { it likeAny spriteExtensions || it == "ATT" }
                .orFalse()
        }
        if (invalidFiles.isNotEmpty()) {
            throw Exception("Invalid file(s) body files found. Expected ${spriteExtensions.joinToString(", ")} and ATT files only")
        }
        val valid = nodes.all { node ->
            BreedKey.isGenericMatch(node.key, key)
        }
        if (!valid) {
            throw Exception("Mismatched body data files found in bundle")
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return file in virtualFiles
    }

    override fun expandOnDoubleClick(): Boolean {
        return true
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getName(): String {
        return mName
    }

    override fun getWeight(): Int {
        return SORT_WEIGHT
    }
    companion object {
        fun name(key: BreedKey): String {
            val genus = getGenusString(key.genus!!)!!.capitalize()
            val slot = key.breed?.uppercase()
                ?: throw IllegalStateException("Key should have a valid breed slot for BreedNodeBySlot node")
            val gender = when (key.gender) {
                1 -> "M"
                2 -> "F"
                else -> throw IllegalStateException("Key should have a valid gender for BreedNodeBySlot node")
            }
            return "$genus $slot ($gender)"
        }
    }
}