package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.directory
import com.badahori.creatures.plugins.intellij.agenteering.utils.VirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.vfs.VirtualFile


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




internal fun CaosScriptStringLike.resolveToFile(ignoreExtension: Boolean): VirtualFile? {
    val relativePath = stringValue
        .replace("\\[[^]]*\\]".toRegex(), "")
        .nullIfEmpty()
        ?: return null
    val directory = this.directory
        ?: return null
    return VirtualFileUtil
        .findChildIgnoreCase(
            parent = directory,
            ignoreExtension = ignoreExtension,
            directory = false,
            relativePath
        )
}