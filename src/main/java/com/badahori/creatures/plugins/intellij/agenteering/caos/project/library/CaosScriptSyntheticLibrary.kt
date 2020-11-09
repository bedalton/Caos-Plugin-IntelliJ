package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.mutableListOfNotNull
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon


@ApiStatus.Experimental
internal object CaosSyntheticLibrary: SyntheticLibrary(), ItemPresentation {

    val sources: VirtualFile? get() = libraryPath()

    override fun equals(other: Any?): Boolean = other == this

    override fun hashCode(): Int = sources.hashCode()
    override fun getPresentableText(): String = "CAOS-Std-Lib"

    override fun getLocationString(): String? = null

    override fun getIcon(p0: Boolean): Icon? = CaosScriptIcons.SDK_ICON

    override fun getSourceRoots(): MutableCollection<VirtualFile> {
        val vfsLibraryVirtualFile = CaosVirtualFileSystem
                .instance
                .getOrCreateRootChildDirectory(BUNDLE_DEFINITIONS_FOLDER)
        return mutableListOfNotNull(sources, vfsLibraryVirtualFile)
    }
}

/**
 * Gets a path to the plugins store of CaosLibs
 */
internal fun libraryPath() : VirtualFile? {
    val libraryPath = CaosFileUtil.getPluginResourceFile(BUNDLE_DEFINITIONS_FOLDER)
    if (libraryPath == null) {
        val pluginRoot = CaosFileUtil.PLUGIN_HOME_DIRECTORY
        if (pluginRoot == null || !pluginRoot.exists()) {
            com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER.severe("Failed to locate bundled caos definition files: Plugin root is invalid")
        } else {
            com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER.severe("Failed to locate bundled caos definition files: Files in plugin root is <${pluginRoot.children?.map { it.name }}>")
        }
        return null
    }
    return libraryPath
}