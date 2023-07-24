//@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport.DEFAULT_POSE_STRING_VERSION
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosVariantConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.ProjectSettingsConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient
import kotlinx.serialization.Serializable
import java.util.*


/**
 * State container responsible for getting/setting project state
 */
@State(
    name = "CaosProjectSetting",
)
class CaosProjectSettingsService(
    val project: Project
) : PersistentStateComponent<CaosProjectSettingsService.CaosProjectSettings>,
    HasIgnoredCatalogueTags {

    @Attribute(converter = ProjectSettingsConverter::class)
    @Suppress("MemberVisibilityCanBePrivate")
    var myState: CaosProjectSettings? = null

    val stateNonNull: CaosProjectSettings
        get() = myState ?: CaosProjectSettings().also {
            myState = it
        }

    override fun getState(): CaosProjectSettings? {
        return this.myState
            ?.migrate()
    }

    fun setState(newState: CaosProjectSettings?) {
        if (newState == null) {
            myState = CaosProjectSettings()
        } else if (newState != myState) {
            loadState(newState)
        }
    }

    override fun loadState(state: CaosProjectSettings) {
        val oldState = this.stateNonNull
        this.myState = state
//        XmlSerializerUtil.copyBean(state, this)
        if (!project.isDisposed) {
            onUpdate(project, oldState, state)
        }
        LOGGER.info("Applying project service settings")
    }

    var lastVariant: CaosVariant?
        get() = stateNonNull.lastVariant
        set(value) {
            val state = stateNonNull
            if (value == state.lastVariant) {
                return
            }
            loadState(
                state.copy(
                    lastVariant = value
                )
            )
        }

    var defaultVariant: CaosVariant?
        get() = stateNonNull.defaultVariant
        set(value) {
            val state = stateNonNull
            if (value == state.defaultVariant) {
                return
            }
            loadState(
                state.copy(
                    defaultVariant = value
                )
            )
        }

    var indent: Boolean
        get() = stateNonNull.indent
        set(value) {
            val state = stateNonNull
            if (value == state.indent) {
                return
            }
            loadState(
                state.copy(
                    indent = value
                )
            )
        }

    var showLabels: Boolean
        get() = stateNonNull.showLabels
        set(value) {
            val state = stateNonNull
            if (value == state.showLabels) {
                return
            }
            loadState(
                state.copy(
                    showLabels = value
                )
            )
        }


    var ditherSPR: Boolean
        get() = stateNonNull.ditherSPR
        set(value) {
            val state = stateNonNull
            if (value == state.ditherSPR) {
                return
            }
            loadState(
                state.copy(
                    ditherSPR = value
                )
            )
        }


    var attScale: Int
        get() = stateNonNull.attScale
        set(value) {
            val state = stateNonNull
            if (value == state.attScale) {
                return
            }
            loadState(
                state.copy(
                    attScale = value
                )
            )
        }


    var showPoseView: Boolean
        get() = stateNonNull.showPoseView
        set(value) {
            val state = stateNonNull
            if (value == state.showPoseView) {
                return
            }
            loadState(
                state.copy(
                    showPoseView = value
                )
            )
        }


    var defaultPoseString: String
        get() = stateNonNull.defaultPoseString
        set(value) {
            val state = stateNonNull
            if (value == state.defaultPoseString) {
                return
            }
            loadState(
                state.copy(
                    defaultPoseString = value
                )
            )
        }

    var useJectByDefault: Boolean
        get() = stateNonNull.useJectByDefault.apply {
            if (this) {
                LOGGER.severe("JECT should not be enabled as it is not implemented")
                useJectByDefault = false
                // TODO("Set up JECT file settings")
            }
        }
        set(value) {
            val state = stateNonNull
            if (value == state.useJectByDefault)
                return
            loadState(
                state.copy(
                    useJectByDefault = value
                )
            )
        }

    var trimBLKs: Boolean?
        get() = stateNonNull.trimBLKs
        set(value) {
            loadState(
                stateNonNull.copy(
                    trimBLKs = value
                )
            )
        }

    override var ignoredCatalogueTags: List<String>
        get() = stateNonNull.ignoredCatalogueTags
        set(value) {
            loadState(
                stateNonNull.copy(
                    ignoredCatalogueTags = value.distinct()
                )
            )
        }

    /**
     * Project state object used to store various properties at the project level
     */
    @Serializable
    data class CaosProjectSettings(
        @Attribute(converter = CaosVariantConverter::class)
        val lastVariant: CaosVariant? = null,
        @Attribute(converter = CaosVariantConverter::class)
        val defaultVariant: CaosVariant? = null,
        val indent: Boolean = true,
        val showLabels: Boolean = true,
        val ditherSPR: Boolean = false,
        val attScale: Int = 6,
        var showPoseView: Boolean = true,
        @Attribute(converter = StringListConverter::class)
        val ignoredFilenames: List<String> = listOf(),
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

        fun migrate(): CaosProjectSettings {
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

    private fun onUpdate(project: Project, oldState: CaosProjectSettings?, newState: CaosProjectSettings) {
        if (project.isDisposed) {
            return
        }
        project.messageBus.syncPublisher(TOPIC).onChange(oldState, newState)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaosProjectSettingsService

        return myState == other.myState
    }

    override fun hashCode(): Int {
        return myState?.hashCode() ?: 0
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
        fun addSettingsChangedListener(
            project: Project,
            listener: (oldState: CaosProjectSettings?, newState: CaosProjectSettings) -> Unit
        ) {
            addSettingsChangedListener(project, object : CaosProjectSettingsChangeListener {
                override fun onChange(oldState: CaosProjectSettings?, newState: CaosProjectSettings) {
                    listener(oldState, newState)
                }
            })
        }

        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(
            project: Project,
            disposable: Disposable,
            listener: (oldState: CaosProjectSettings?, newState: CaosProjectSettings) -> Unit
        ) {
            try {
                addSettingsChangedListener(project, disposable, object : CaosProjectSettingsChangeListener {
                    override fun onChange(oldState: CaosProjectSettings?, newState: CaosProjectSettings) {
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
        fun addSettingsChangedListener(project: Project, listener: CaosProjectSettingsChangeListener) {
            if (project.isDisposed) {
                return
            }
            project.messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(
            project: Project,
            disposable: Disposable,
            listener: CaosProjectSettingsChangeListener
        ) {
            if (project.isDisposed) {
                return
            }
            try {
                project.messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }

        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(project: Project): CaosProjectSettingsService {
            return project.getService(CaosProjectSettingsService::class.java)
        }
    }
}

interface CaosProjectSettingsChangeListener : EventListener {
    fun onChange(
        oldState: CaosProjectSettingsService.CaosProjectSettings?,
        newState: CaosProjectSettingsService.CaosProjectSettings
    )
}

