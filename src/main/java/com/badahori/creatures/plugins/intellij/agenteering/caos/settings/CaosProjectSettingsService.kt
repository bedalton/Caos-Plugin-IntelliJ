package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute

/**
 * Service for fetching CAOS project settings
 */
interface CaosProjectSettingsService: PersistentStateComponent<CaosProjectSettingsComponent.State> {

    var lastVariant: CaosVariant?
    var defaultVariant: CaosVariant?
    var indent: Boolean
    var showLabels: Boolean
    var ditherSPR: Boolean
    var attScale: Int
    var showPoseView: Boolean
//    var gameInterfaceNames: List<GameInterfaceName>
//    var ignoredFilenames: List<String>
    var combineAttNodes: Boolean
    var defaultPoseString: String
//    var lastGameInterfaceNames: List<String>
    var useJectByDefault: Boolean
    var isAutoPoseEnabled: Boolean


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
