package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.forKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsComponent.State
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import java.util.*

internal object CaosApplicationSettings : HasGameInterfaces {
    internal val service get() = CaosApplicationSettingsService.getInstance()

    internal var state: State
        get() = service.state
        set(value) {
            service.loadState(state)
        }

    val isAutoPoseEnabled: Boolean get() = state.autoPoseEnabled == true

    override var gameInterfaceNamesRaw: List<GameInterfaceName>
        get() = service.gameInterfaceNamesRaw
        set(value) {
            service.gameInterfaceNamesRaw = value
        }

    fun lastInterface(variant: CaosVariant, interfaceName: GameInterfaceName) {
        val state = state
        val prefix = variant.lastInterfacePrefix
        service.loadState(state.copy(
            lastGameInterfaceNames = state.lastGameInterfaceNames.filterNot {
                it.startsWith(prefix)
            } + interfaceName.storageKey
        ))
    }


    fun lastInterface(variant: CaosVariant?): GameInterfaceName? {
        if (variant == null)
            return null
        val prefix = variant.lastInterfacePrefix
        return state.lastGameInterfaceNames
            .mapNotNull map@{ entry ->
                if (!entry.startsWith(prefix))
                    return@map null
                val key = entry.substring(prefix.length)
                state.gameInterfaceNames.forKey(variant, key)
            }
            .firstOrNull()
    }
}

/**
 * Service for fetching application level CAOS settings
 */
interface CaosApplicationSettingsService :
    PersistentStateComponent<State>,
    HasGameInterfaces {
    override fun getState(): State
    override fun loadState(state: State)


    fun migrateIfNeeded(stateIn: State): State

    companion object {
        /**
         * Extension method to get settings for the application
         */
        @JvmStatic
        fun getInstance(): CaosApplicationSettingsService {
            return ApplicationManager.getApplication().getService(CaosApplicationSettingsService::class.java)
        }

    }
}


/**
 * State container responsible for getting/setting application level state
 */
@com.intellij.openapi.components.State(
    name = "CaosApplicationSettingsComponent"
)
class CaosApplicationSettingsComponent : CaosApplicationSettingsService,
    PersistentStateComponent<State> {
    private var state: State = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(state: State) {
        val newState = migrateIfNeeded(state)
        if (this.state == newState) {
            return
        }
        this.state = newState
        onUpdate()
    }

    override fun migrateIfNeeded(stateIn: State): State {
        if (state.schema == State.CURRENT_SCHEMA_VERSION) {
            return stateIn
        }
        @Suppress("UnnecessaryVariable")
        val state = stateIn
        return state
    }


    override var gameInterfaceNamesRaw: List<GameInterfaceName>
        get() = state.gameInterfaceNames
        set(value) {
            loadState(
                state.copy(
                    gameInterfaceNames = value
                )
            )
        }

    data class State(
        val schema: Int = CURRENT_SCHEMA_VERSION,
        val autoPoseEnabled: Boolean? = null,
        @Attribute(converter = GameInterfaceListConverter::class)
        val gameInterfaceNames: List<GameInterfaceName> = listOf(),
        val lastGameInterfaceNames: List<String> = listOf(),
    ) {
        companion object {
            internal const val CURRENT_SCHEMA_VERSION = 1
        }
    }

    private fun onUpdate() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(this.state)
    }


    companion object {

        val TOPIC = Topic.create(
            "CAOSApplicationSettingsChangedListener",
            CaosApplicationSettingsChangeListener::class.java
        )

        /**
         * Adds a listener to track and change to the settings component
         */
        @Suppress("unused")
        fun addSettingsChangedListener(listener: (settings: State) -> Unit) {
            addSettingsChangedListener(object : CaosApplicationSettingsChangeListener {
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
            try {
                addSettingsChangedListener(disposable, object : CaosApplicationSettingsChangeListener {
                    override fun onChange(settings: State) {
                        listener(settings)
                    }
                })
            } catch (ignored: Exception) {
            }
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosApplicationSettingsChangeListener) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosApplicationSettingsChangeListener) {
            try {
                ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }
    }
}

interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(settings: State)
}

