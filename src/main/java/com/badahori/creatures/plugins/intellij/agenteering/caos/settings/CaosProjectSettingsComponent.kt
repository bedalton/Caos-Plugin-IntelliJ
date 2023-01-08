package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport.DEFAULT_POSE_STRING_VERSION
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
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
class CaosProjectSettingsComponent : CaosProjectSettingsService,
    PersistentStateComponent<CaosProjectSettingsComponent.State>, HasIgnoredCatalogueTags {

    private var state: State = State()

    override fun getState(): State {
        return this.state
            .migrate()
    }

    override fun loadState(state: State) {
        val oldState = this.state
        this.state = state
        onUpdate(oldState, state)
    }

    override var lastVariant: CaosVariant?
        get() = state.lastVariant
        set(value) {
            if (value == state.lastVariant) {
                return
            }
            loadState(state.copy(
                lastVariant = value
            ))
        }

    override var defaultVariant: CaosVariant?
        get() = state.defaultVariant
        set(value) {
            if (value == state.defaultVariant) {
                return
            }
            loadState(state.copy(
                defaultVariant = value
            ))
        }

    override var indent: Boolean
        get() = state.indent
        set(value) {
            if (value == state.indent) {
                return
            }
            loadState(state.copy(
                indent = value
            ))
        }

    override var showLabels: Boolean
        get() = state.showLabels
        set(value) {
            if (value == state.showLabels) {
                return
            }
            loadState(state.copy(
                showLabels = value
            ))
        }


    override var ditherSPR: Boolean
        get() = state.ditherSPR
        set(value) {
            if (value == state.ditherSPR) {
                return
            }
            loadState(state.copy(
                ditherSPR = value
            ))
        }


    override var attScale: Int
        get() = state.attScale
        set(value) {
            if (value == state.attScale) {
                return
            }
            loadState(state.copy(
                attScale = value
            ))
        }


    override var showPoseView: Boolean
        get() = state.showPoseView
        set(value) {
            if (value == state.showPoseView) {
                return
            }
            loadState(state.copy(
                showPoseView = value
            ))
        }


    override var defaultPoseString: String
        get() = state.defaultPoseString
        set(value) {
            if (value == state.defaultPoseString) {
                return
            }
            loadState(state.copy(
                defaultPoseString = value
            ))
        }

    override var useJectByDefault: Boolean
        get() = state.useJectByDefault.apply {
            if (this) {
                LOGGER.severe("JECT should not be enabled as it is not implemented")
                useJectByDefault = false
                // TODO("Set up JECT file settings")
            }
        }
        set(value) {
            if (value == state.useJectByDefault)
                return
            loadState(
                state.copy(
                    useJectByDefault = value
                )
            )
        }

    override var trimBLKs: Boolean?
        get() = state.trimBLKs
        set(value) {
            loadState(state.copy(
                trimBLKs = value
            ))
        }

    override var ignoredCatalogueTags: List<String>
        get() = state.ignoredCatalogueTags
        set(value) {
            loadState(state.copy(
                ignoredCatalogueTags = value.distinct()
            ))
        }

    /**
     * Project state object used to store various properties at the project level
     */
    data class State(
        val lastVariant: CaosVariant? = null,
        val defaultVariant: CaosVariant? = null,
        val indent: Boolean = true,
        val showLabels: Boolean = true,
        val ditherSPR: Boolean = false,
        val attScale: Int = 6,
        var showPoseView: Boolean = true,
        @Attribute(converter = StringListConverter::class)
        val ignoredFilenames: List<String> = listOf(),
        val lastGameInterfaceNames: List<String> = listOf(),
        val defaultPoseString: String = PoseEditorSupport.DEFAULT_POSE_STRING,
        val defaultPoseStringVersion: Int? = null,
        val useJectByDefault: Boolean = false,
        val trimBLKs: Boolean? = null,
        @Attribute(converter = StringListConverter::class)
        val ignoredCatalogueTags: List<String> = emptyList(),
        val stateVersion: Int? = null
    ) {


        @Transient
        private var mInjectionCheckDisabled: Boolean = false

        val injectionCheckDisabled: Boolean get() = mInjectionCheckDisabled

        fun disableInjectionCheck(disable: Boolean) {
            mInjectionCheckDisabled = disable
        }

        /**
         * Checks if this settings object is set to the given CAOS variant
         */
        fun isVariant(variant: CaosVariant): Boolean = variant == this.lastVariant

        fun migrate(): State {
            if (stateVersion == STATE_VERSION) {
                return this
            }
            var out = this
            if (defaultPoseStringVersion != DEFAULT_POSE_STRING_VERSION) {
                out = out.copy(
                    defaultPoseStringVersion = DEFAULT_POSE_STRING_VERSION,
                    defaultPoseString = PoseEditorSupport.DEFAULT_POSE_STRING
                )
            }

            return out.copy(
                stateVersion = STATE_VERSION
            )
        }

        companion object {
            private const val DATA_VERSION = 1
            private const val STATE_VERSION = DATA_VERSION + DEFAULT_POSE_STRING_VERSION
        }
    }

    private fun onUpdate(oldState: State, newState: State) {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(oldState, newState)
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
        fun addSettingsChangedListener(listener: (oldState: State, newState: State) -> Unit) {
            addSettingsChangedListener(object : CaosProjectSettingsChangeListener {
                override fun onChange(oldState: State, newState: State) {
                    listener(oldState, newState)
                }
            })
        }

        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(disposable: Disposable, listener: (oldState: State, newState: State) -> Unit) {
            try {
                addSettingsChangedListener(disposable, object : CaosProjectSettingsChangeListener {
                    override fun onChange(oldState: State, newState: State) {
                        listener(oldState, newState)
                    }
                })
            } catch (ignored: Exception) {}
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosProjectSettingsChangeListener) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosProjectSettingsChangeListener) {
            try {
                ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }
    }
}

