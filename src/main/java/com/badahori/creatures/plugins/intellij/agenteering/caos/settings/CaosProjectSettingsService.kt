package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

/**
 * Service for fetching CAOS project settings
 */
interface CaosProjectSettingsService: PersistentStateComponent<CaosProjectSettingsComponent.State>, HasIgnoredCatalogueTags {

    var lastVariant: CaosVariant?
    var defaultVariant: CaosVariant?
    var indent: Boolean
    var showLabels: Boolean
    var ditherSPR: Boolean
    var attScale: Int
    var showPoseView: Boolean
    var defaultPoseString: String
    var useJectByDefault: Boolean
//    val lastGameInterfaceNames: List<String>
    var trimBLKs: Boolean?
    override var ignoredCatalogueTags: List<String>

    override fun getState(): CaosProjectSettingsComponent.State
    override fun loadState(state: CaosProjectSettingsComponent.State)


    companion object {
        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(project: Project): CaosProjectSettingsService  {
            return ServiceManager.getService(project, CaosProjectSettingsService::class.java)
        }

    }
}
