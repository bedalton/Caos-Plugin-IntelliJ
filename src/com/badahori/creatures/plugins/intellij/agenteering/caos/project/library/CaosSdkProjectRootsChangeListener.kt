package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import java.util.logging.Logger

object CaosSdkProjectRootsChangeListener : ModuleRootListener {

    val LOGGER: Logger = Logger.getLogger("#"+ CaosSdkProjectRootsChangeListener::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        super.rootsChanged(event)
        val project = event.source as? Project
        if (project == null) {
            LOGGER.severe("SdkRootsChangeListener: Failed to get project on roots change. Object is of type: ${event.source.javaClass.canonicalName}")
            return
        }
    }

}