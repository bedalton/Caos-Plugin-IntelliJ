package com.openc2e.plugins.intellij.caos.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent
import javax.swing.Icon
import com.intellij.openapi.roots.libraries.LibraryType
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptIcons

/**
 * Libarary type for an objective-j frameworkName
 */
class CaosLibraryType : LibraryType<DummyLibraryProperties>(LIBRARY) {

    override fun createPropertiesEditor(component: LibraryEditorComponent<DummyLibraryProperties>): LibraryPropertiesEditor? {
        return null
    }

    override fun createNewLibrary(parentComponent: JComponent, contextDirectory: VirtualFile?, project: Project): NewLibraryConfiguration? {
        return LibraryTypeService.getInstance()
                .createLibraryFromFiles(createLibraryRootsComponentDescriptor(), parentComponent, contextDirectory, this, project);
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
            LibraryType.EP_NAME.findExtension(CaosLibraryType::class.java)!!
        }
    }
}