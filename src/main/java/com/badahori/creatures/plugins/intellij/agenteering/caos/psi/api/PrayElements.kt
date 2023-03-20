package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.pathSeparatorChar
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.VirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope


interface PrayTag: CaosScriptCompositeElement {
    val tagName: String
    val isNumberValue: Boolean
    val isStringValue: Boolean
    val valueAsInt: Int?
    val valueAsString: String?
}

interface PrayChildElement: CaosScriptCompositeElement

interface PrayTagName : PrayChildElement {
    val stringValue: String
}

interface PrayTagValue: PrayChildElement {
    // If value is int or float, though float is invalid
    val isNumberValue: Boolean
    // If is number and number is float, number value will be null
    val valueAsInt: Int?
    val isStringValue: Boolean
    val valueAsString: String?
}




internal fun CaosScriptStringLike.resolveToFile(ignoreExtension: Boolean, relative: Boolean, scope: GlobalSearchScope? = null): VirtualFile? {
    val relativePath = stringValue
        .replace("\\[\\d+]".toRegex(), "")
        .nullIfEmpty()
        ?: return null
    val directory = this.directory
        ?: return null
    return if (relative) {
        VirtualFileUtil
            .findChildIgnoreCase(
                parent = directory,
                ignoreExtension = ignoreExtension,
                directory = false,
                relativePath,
                scope = scope
            )
    } else {
        (if (ignoreExtension) {
            val fileName = PathUtil.getFileNameWithoutExtension(relativePath) ?: relativePath
            CaseInsensitiveFileIndex.findWithoutExtension(project, fileName, scope)
        } else {
            CaseInsensitiveFileIndex.findWithFileName(project, PathUtil.getLastPathComponent(relativePath) ?: relativePath, scope)
        }).minByOrNull {
            maxOf(directory.path.length, it.path.length) - (VfsUtil.findRelativePath(directory, it, pathSeparatorChar)?.length ?: 0)
        }
    }
}