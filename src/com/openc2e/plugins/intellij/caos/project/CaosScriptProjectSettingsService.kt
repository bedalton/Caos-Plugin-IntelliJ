package com.openc2e.plugins.intellij.caos.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettingsService.ProjectSettings

@State(
        name = "caosScript",
        reloadable = true
)
data class CaosScriptProjectSettingsService(private var _state:ProjectSettings = ProjectSettings()) : PersistentStateComponent<ProjectSettings> {

    /**
     * Gets the current state
     */
    override fun getState(): ProjectSettings = _state

    /**
     * Loads state from .idea folder
     */
    override fun loadState(newState: ProjectSettings) {
        _state = newState
    }

    /**
     * Data class holding project settings
     */
    @Storage("caosScript.xml")
    data class ProjectSettings(
            val baseVariant:String = DEFAULT_VARIANT,
            val indentCode:Boolean = true
    )
    companion object {
        const val DEFAULT_VARIANT = "DS"
    }
}
