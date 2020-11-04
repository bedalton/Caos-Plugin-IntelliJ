/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified for plugin By: Daniel Badal
 *
 */
package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.projectRoots.ui.Util
import com.intellij.openapi.roots.JavadocOrderRootType
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.AttachRootButtonDescriptor
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor
import com.intellij.openapi.roots.libraries.ui.OrderRootTypePresentation
import com.intellij.openapi.roots.libraries.ui.RootDetector
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor
import com.intellij.openapi.vfs.VirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import icons.CaosScriptIcons
import javax.swing.JComponent

class CaosLibraryRootComponentDescriptor : LibraryRootsComponentDescriptor() {
    override fun getRootTypePresentation(type: OrderRootType): OrderRootTypePresentation? {
        if (type != OrderRootType.SOURCES)
            return null
        return OrderRootTypePresentation("CAOS-Lib", CaosScriptIcons.SDK_ICON)
    }

    override fun getRootDetectors(): List<RootDetector> {
        return listOf(
                CaosLibRootDetector(OrderRootType.SOURCES, CaosBundle.message("caos.sources.library.root-detector.sources.name"))
        )
    }

    override fun createAttachButtons(): List<AttachRootButtonDescriptor> {
        return listOf(AttachUrlJavadocDescriptor())
    }

    private class AttachUrlJavadocDescriptor : AttachRootButtonDescriptor(JavadocOrderRootType.getInstance(), ProjectBundle.message("module.libraries.javadoc.url.button")) {

        override fun selectFiles(parent: JComponent,
                                 initialSelection: VirtualFile?,
                                 contextModule: Module?,
                                 libraryEditor: LibraryEditor): Array<VirtualFile> {
            val vFile = Util.showSpecifyJavadocUrlDialog(parent)
            return vFile?.let { arrayOf(it) } ?: VirtualFile.EMPTY_ARRAY
        }
    }
}