package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon
import javax.swing.JComponent

/**
 * Library type for an objective-j frameworkName
 */
class CaosLibraryType : LibraryType<DummyLibraryProperties>(LIBRARY) {

    override fun createPropertiesEditor(component: LibraryEditorComponent<DummyLibraryProperties>): LibraryPropertiesEditor? {
        return null
    }

    override fun createNewLibrary(parentComponent: JComponent, contextDirectory: VirtualFile?, project: Project): NewLibraryConfiguration? {
        return LibraryTypeService.getInstance()
                .createLibraryFromFiles(createLibraryRootsComponentDescriptor(), parentComponent, contextDirectory, this, project)
    }

    override fun createLibraryRootsComponentDescriptor(): LibraryRootsComponentDescriptor {
        // todo create descriptor
        return CaosLibraryRootComponentDescriptor()
    }

    override fun getCreateActionName(): String? {
        return CaosBundle.message("caos.sources.library.action-name")
    }

    override fun getIcon(properties: DummyLibraryProperties?): Icon? {
        return CaosScriptIcons.SDK_ICON
    }

    companion object {
        val LIBRARY: PersistentLibraryKind<DummyLibraryProperties> = object : PersistentLibraryKind<DummyLibraryProperties>(CaosBundle.message("caos.sources.library.library-name")) {
            override fun createDefaultProperties(): DummyLibraryProperties {
                return DummyLibraryProperties()
            }
        }

        val INSTANCE: CaosLibraryType by lazy {
            EP_NAME.findExtension(CaosLibraryType::class.java)!!
        }
    }
}

@ApiStatus.Experimental
internal object CaosSyntheticLibrary: SyntheticLibrary(), ItemPresentation {

    val sources:VirtualFile? get() = libraryPath()

    override fun equals(other: Any?): Boolean = other == this

    override fun hashCode(): Int = sources.hashCode()
    override fun getPresentableText(): String = "CAOS-Std-Lib"

    override fun getLocationString(): String? = null

    override fun getIcon(p0: Boolean): Icon? = CaosScriptIcons.SDK_ICON

    override fun getSourceRoots(): MutableCollection<VirtualFile> {
        return sources?.let { mutableListOf(it) } ?: mutableListOf()
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
            LOGGER.severe("Failed to locate bundled caos definition files: Plugin root is invalid")
        } else {
            LOGGER.severe("Failed to locate bundled caos definition files: Files in plugin root is <${pluginRoot.children?.map { it.name }}>")
        }
        return null
    }
    return libraryPath
}