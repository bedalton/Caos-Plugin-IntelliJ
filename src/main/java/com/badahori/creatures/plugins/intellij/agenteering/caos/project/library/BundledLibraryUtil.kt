package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable.ModifiableModel
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import java.util.logging.Logger

internal const val BUNDLE_DEFINITIONS_FOLDER = "lib"
private val LOGGER:Logger = Logger.getLogger("#BundledLibraryUtil")

internal fun canRegisterSourcesAsLibrary(directories: List<String>) : Boolean {
    if (!CaosFileUtil.PLUGIN_HOME_DIRECTORY?.exists().orFalse()) {
        LOGGER.severe("Failed to find plugin home directory")
        return false
    }
    return directories.all {directory ->
        CaosFileUtil.getPluginResourceFile("$BUNDLE_DEFINITIONS_FOLDER/$directory")?.exists().orFalse()
    }
}