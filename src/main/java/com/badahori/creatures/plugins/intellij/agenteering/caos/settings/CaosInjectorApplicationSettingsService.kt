package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.JsonToXMLStringConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.fasterxml.jackson.databind.annotation.JsonAppend.Attr
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import java.util.*


@State(
    name = "caos-injectors",
    storages = [Storage(
        value = "CAOS_Injectors.xml",
        roamingType = RoamingType.DISABLED
    )],
)
class CaosInjectorApplicationSettingsService :
    PersistentStateComponent<CaosInjectorApplicationSettingsService.CaosWineSettings> {

    @Attribute(converter = CaosWineApplicationSettingsConverter::class)
    private var myState: CaosWineSettings? = null

    @Suppress("MemberVisibilityCanBePrivate")
    internal val stateNonNull: CaosWineSettings
        get() = myState ?: (CaosWineSettings()).also {
            loadState(it)
        }

    override fun getState(): CaosWineSettings? {
        return myState
    }

    override fun loadState(state: CaosWineSettings) {

        // Ensure we do not serialize default game interface names
        val actualState = state.copy(gameInterfaceNames = state.gameInterfaceNames
            .filter {
                @Suppress("SENSELESS_COMPARISON")
                it != null && (it !is NativeInjectorInterface || !it.isDefault)
            })

        if (actualState == this.myState) {
            return
        }
        val oldState = this.myState
        this.myState = actualState
//        XmlSerializerUtil.copyBean(state, this)

        // Notify listeners of update
        onUpdate(oldState, state)
    }

    var lastWineDirectory: String?
        get() = stateNonNull.lastWineDirectory
        set(value) {
            if (stateNonNull.lastWineDirectory == value) {
                return
            }
            loadState(
                stateNonNull.copy(
                    lastWineDirectory = value
                )
            )
        }

    var gameInterfaceNames: List<GameInterfaceName>
        get() = stateNonNull.gameInterfaceNames
        set(value) {
            loadState(
                stateNonNull.copy(
                    gameInterfaceNames = value
                )
            )
        }

    var winePath: String?
        get() = stateNonNull.winePath ?: wine32Path ?: wine64Path
        set(value) {
            loadState(
                stateNonNull.copy(
                    winePath = value,
                    wine64Path = stateNonNull.wine64Path ?: value,
                    wine32Path = stateNonNull.wine32Path ?: value
                )
            )
        }

    var wine32Path: String?
        get() = stateNonNull.wine32Path
        set(value) {
            if (stateNonNull.wine32Path == value) {
                return
            }
            loadState(
                stateNonNull.copy(
                    wine32Path = value
                )
            )
        }

    var wine64Path: String?
        get() = stateNonNull.wine64Path
        set(value) {
            if (wine64Path == value) {
                return
            }
            loadState(
                stateNonNull.copy(
                    wine64Path = value
                )
            )
        }

    @Serializable
    data class CaosWineSettings(
        @Attribute(converter = GameInterfaceListConverter::class)
        val gameInterfaceNames: List<GameInterfaceName> = listOf(),
        @Attribute(converter = StringListConverter::class)
        val lastGameInterfaceNames: List<String> = listOf(),
        @Attribute
        val lastWineDirectory: String? = null,
        @Attribute
        val winePath: String? = null,
        @Attribute
        val wine32Path: String? = null,
        @Attribute
        val wine64Path: String? = null,
    )


    class CaosWineApplicationSettingsConverter : JsonToXMLStringConverter<CaosWineSettings>() {
        override val serializer: SerializationStrategy<CaosWineSettings>
            get() = CaosWineSettings.serializer()
        override val deserializer: DeserializationStrategy<CaosWineSettings>
            get() = CaosWineSettings.serializer()
    }

    interface CaosWineSettingsChangeListener : EventListener {
        fun onChange(oldState: CaosWineSettings?, newState: CaosWineSettings)
    }

    companion object {
        private val TOPIC = Topic.create(
            "CAOSWineSettingsChangedListener",
            CaosWineSettingsChangeListener::class.java
        )

        /**
         * Adds a listener to track and change to the settings component
         */
        @Suppress("unused")
        fun addSettingsChangedListener(listener: (oldState: CaosWineSettings?, newState: CaosWineSettings) -> Unit) {
            addSettingsChangedListener(object : CaosWineSettingsChangeListener {
                override fun onChange(oldState: CaosWineSettings?, newState: CaosWineSettings) {
                    listener(oldState, newState)
                }
            })
        }

        private fun onUpdate(oldState: CaosWineSettings?, newState: CaosWineSettings) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(oldState, newState)
        }

        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(
            disposable: Disposable,
            listener: (oldState: CaosWineSettings?, newState: CaosWineSettings) -> Unit
        ) {
            try {
                addSettingsChangedListener(disposable, object : CaosWineSettingsChangeListener {
                    override fun onChange(oldState: CaosWineSettings?, newState: CaosWineSettings) {
                        listener(oldState, newState)
                    }
                })
            } catch (ignored: Exception) {
            }
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosWineSettingsChangeListener) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosWineSettingsChangeListener) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            try {
                ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }

        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(): CaosInjectorApplicationSettingsService {
            return ApplicationManager.getApplication().getService(CaosInjectorApplicationSettingsService::class.java)
        }
    }

}

