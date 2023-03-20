@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.nullIfEmpty
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
import com.intellij.psi.PsiFile
import icons.CaosScriptIcons

internal class BreedNode(
    private val notNullProject: Project,
    key: BreedPartKey,
    private val files: List<AbstractTreeNode<*>>,
    viewSettings: ViewSettings?,
) : ProjectViewNode<BreedPartKey>(notNullProject, key.copyWithPart('*'), viewSettings) {

    private val key = key.copyWithPart(null)

    private val hasSprites by lazy {
        spriteExtensions.isNotEmpty()
    }

    private val hasAtts by lazy {
        files.any { PathUtil.getExtension(it.nameExtended ?: "")?.lowercase() == "att" }
    }

    private val spriteExtensions by lazy {
        files.mapNotNull { PathUtil.getExtension(it.nameExtended ?: "")?.uppercase() }
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

    override fun update(presentation: PresentationData) {
        if (notNullProject.isDisposed || virtualFile?.isValid == false) {
            return
        }
        val extension = if (hasAtts && spriteExtensions.isEmpty()) {
            ".att"
        } else if (!hasAtts && spriteExtensions.size == 1) {
            "." + spriteExtensions.first()
        } else {
            ""
        }
        presentation.presentableText = "*${key.code}.$extension"
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
        return files
//        return files
//            .mapNotNull map@{
//                if (notNullProject.isDisposed || !it.isValid) {
//                    return@map null
//                }
//                if (it.extension?.lowercase() in SpriteParser.VALID_SPRITE_EXTENSIONS) {
//                    SpriteFileTreeNode(notNullProject, it, viewSettings)
//                } else {
//                    AttFileNode(notNullProject, it, viewSettings)
//                }
//            }
    }


    private fun ensureValid() {
        if (notNullProject.isDisposed) {
            throw Exception("Cannot create breed file bundle. Project is disposed")
        }
        if (files.isEmpty()) {
            throw Exception("Cannot create body data bundle without files")
        }
        val invalidFiles = files.filterNot { file ->
            PathUtil.getExtension(file.nameExtended ?: "")?.uppercase()?.let { it likeAny spriteExtensions || it == "ATT" }
                .orFalse()
        }
        if (invalidFiles.isNotEmpty()) {
            throw Exception("Invalid file(s) body files found. Expected ${spriteExtensions.joinToString(", ")} and ATT files only")
        }
        val valid = files.all { file ->
            BreedPartKey.isGenericMatch(BreedPartKey.fromFileName(file.nameExtended ?: ""), key)
        }
        if (!valid) {
            throw Exception("Mismatched body data files found in bundle")
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return files.any { ((it.value as? VirtualFile) ?: ((it as? ProjectViewNode)?.virtualFile)) == file }
    }

    override fun expandOnDoubleClick(): Boolean {
        return true
    }

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun getName(): String {
        return "*${key.code}"
    }

    override fun getWeight(): Int {
        return SORT_WEIGHT
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



    override fun canNavigateToSource(): Boolean {
        return super.canNavigateToSource()
    }

    override fun getChildren(): Collection<AbstractTreeNode<Any>> {
        return emptyList()
    }

}


internal val AbstractTreeNode<*>.nameExtended: String? get() {
    return when (val value = this.value) {
        is PsiFile -> value.name.nullIfEmpty()
        is VirtualFile -> value.name.nullIfEmpty()
        else -> null
    }
        ?: name?.nullIfEmpty()
}