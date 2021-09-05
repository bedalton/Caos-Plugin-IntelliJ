package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient


/**
 * State container responsible for getting/setting project state
 */
@State(
    name = "CaosProjectSettingsComponent"
)
class CaosProjectSettingsComponent: CaosProjectSettingsService,
    PersistentStateComponent<CaosProjectSettingsComponent.State> {

    private var state: State = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(state: State) {
        this.state = state
        onUpdate()
    }

    /**
     * Project state object used to store various properties at the project level
     */
    data class State(
        val lastVariant: CaosVariant? = null,
        val defaultVariant: CaosVariant? = null,
        val indent: Boolean = true,
        val showLabels: Boolean = true,
        val ditherSPR:Boolean = false,
        val attScale: Int = 6,
        var showPoseView: Boolean = true,
        @Attribute(converter = GameInterfaceListConverter::class)
        val gameInterfaceNames: List<GameInterfaceName> = listOf(),
        @Attribute(converter = StringListConverter::class)
        val ignoredFilenames: List<String> = listOf(),
        val defaultPoseString: String = "313122122111111",
        val lastGameInterfaceNames: List<String> = listOf(),
        @Transient
        var injectionCheckDisabled: Boolean = false,
        var useJectByDefault: Boolean = false
    ) {
        /**
         * Checks if this settings object is set to the given CAOS variant
         */
        fun isVariant(variant: CaosVariant): Boolean = variant == this.lastVariant

    }

    private fun onUpdate() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(this.state)
    }

    companion object {
        val TOPIC = Topic.create(
            "CAOSProjectSettingsChangedListener",
            CaosProjectSettingsChangeListener::class.java
        )

        /**
         * Adds a listener to track and change to the settings component
         */
        @Suppress("unused")
        fun addSettingsChangedListener(listener: (settings: State) -> Unit) {
            addSettingsChangedListener(object: CaosProjectSettingsChangeListener {
                override fun onChange(settings: State) {
                    listener(settings)
                }
            })
        }
        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(disposable: Disposable, listener: (settings: State) -> Unit) {
            addSettingsChangedListener(disposable, object: CaosProjectSettingsChangeListener {
                override fun onChange(settings: State) {
                    listener(settings)
                }
            })
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosProjectSettingsChangeListener) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosProjectSettingsChangeListener) {
            ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
        }
    }
}
