package com.openc2e.plugins.intellij.caos.project

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettingsService.ProjectSettings


/**
 * Data class holding project settings
 */
@State(
        name = "caosScript.xml",
        reloadable = true,
        storages = [Storage(value = "caosScript.xml")]
)
class CaosScriptProjectSettingsService : PersistentStateComponent<ProjectSettings> {

    private var _state:ProjectSettings = ProjectSettings()
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

    val variant:String get() = _state.baseVariant

    fun setVariant(variant:String) {
        _state = state.copy(baseVariant = variant)
    }
    fun isVariant(variant:String): Boolean = variant == _state.baseVariant

    data class ProjectSettings(
            val baseVariant:String = DEFAULT_VARIANT,
            val indentCode:Boolean = true
    )
    companion object {
        const val DEFAULT_VARIANT = "DS"
        fun getInstance(project:Project) : CaosScriptProjectSettingsService {
            return ServiceManager.getService(project, CaosScriptProjectSettingsService::class.java)
                    ?: CaosScriptProjectSettingsService()
        }
    }
}
