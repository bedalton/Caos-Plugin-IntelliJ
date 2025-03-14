package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.libraries.LibraryType.EP_NAME
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import javax.swing.Icon
import javax.swing.JComponent

/**
 * Library type for the CAOS library
 */
class CaosLibraryType : LibraryType<DummyLibraryProperties>(CAOS_PERSISTENT_LIBRARY) {

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

    override fun getCreateActionName(): String {
        return CaosBundle.message("caos.sources.library.action-name")
    }

    override fun getIcon(properties: DummyLibraryProperties?): Icon? {
        return CaosScriptIcons.SDK_ICON
    }

}

val CAOS_PERSISTENT_LIBRARY: PersistentLibraryKind<DummyLibraryProperties> by lazy {
    object : PersistentLibraryKind<DummyLibraryProperties>(CaosBundle.message("caos.sources.library.library-name")) {
        override fun createDefaultProperties(): DummyLibraryProperties {
            return DummyLibraryProperties()
        }
    }
}

val CAOS_LIBRARY_INSTANCE: CaosLibraryType by lazy {
    EP_NAME.findExtension(CaosLibraryType::class.java)!!
}