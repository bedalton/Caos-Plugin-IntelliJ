package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.CaosScriptsQuickCollapseToLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

/**
 * Class responsible for Injecting CAOS into the various Creatures games.
 */
object Injector {

    /**
     * Gets the actual version of a C1e game.
     * Perhaps too heavy handed, but trying to assert that the correct game is connected
     * Sends a DDE version check request before each user CAOS injection
     */
    private fun getActualVersion(project: Project, variant: CaosVariant): CaosVariant {
        if (variant.isNotOld) {
            return variant
        }
        val rawCode = "dde: putv vrsn"
        val response = injectPrivate(project, variant, rawCode) { connection, formattedCode ->
            connection.inject(formattedCode)
        }
        if (response !is InjectionStatus.Ok)
            return variant
        return try {
            if (response.response.toInt() < 6) {
                CaosVariant.C1
            } else {
                CaosVariant.C2
            }
        } catch (e: Exception) {
            variant
        }
    }

    /**
     * Checks version info before injection
     */
    fun inject(project: Project, variant: CaosVariant, rawCaosIn: String): Boolean {
        if (!isValidVariant(project, variant))
            return false
        //
        val response = injectPrivate(project, variant, rawCaosIn) { connection, formattedCaos ->
            connection.inject(formattedCaos)
        }
        onResponse(project, response)
        return response is InjectionStatus.Ok
    }

    /**
     * Injects an event script into the Creatures scriptorium
     */
    fun injectEventScript(project: Project, variant: CaosVariant, family: Int, genus: Int, species: Int, eventNumber: Int, rawCaosIn: String): Boolean {
        val response = injectPrivate(project, variant, rawCaosIn) { connection, formattedCaos ->
            connection.injectEventScript(family, genus, species, eventNumber, formattedCaos)
        }
        onResponse(project, response)
        return response is InjectionStatus.Ok
    }

    /**
     * Ensures that variant is supported, and if C1e, that the correct game is running
     */
    private fun isValidVariant(project:Project, variant:CaosVariant) : Boolean {
        if (!canConnectToVariant(variant)) {
            val error = "Injection to ${variant.fullName} is not yet implemented"
            invokeLater {
                CaosInjectorNotifications.show(project, "ConnectionException", error, NotificationType.ERROR)
            }
            return false
        }
        if (variant.isOld) {
            val actualVersion = getActualVersion(project, variant)
            if (actualVersion != variant) {
                postError(project, "Connection Error", "Grammar set to variant [${variant}], but ide is connected to ${actualVersion.fullName}")
                return false
            }
        }
        return true
    }


    /**
     * Responsible for actually injecting the CAOS code.
     */
    private fun injectPrivate(project: Project, variant: CaosVariant, caosIn: String, run:(connection: CaosConnection, formattedCode: String) -> InjectionStatus?): InjectionStatus? {
        val connection = connection(variant, project)
                ?: return InjectionStatus.BadConnection("Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again")
        if (creditsCalled[variant].orFalse()) {
            creditsCalled[variant] = true
            connection.showAttribution(project, variant)
        }

        // Old variant Creatures games crash on extra spaces and any comments
        // So strip them here.
        val caos = if (variant.isOld)
            CaosScriptsQuickCollapseToLine.collapse(variant, caosIn)
        // Comments and additional spaces are allowed in C2e CAOS
        else
            caosIn
        if (!connection.isConnected() && !connection.connect(false)) {
            return null
        }
        return run(connection, caos)
    }

    /**
     * Processes CAOS injection response, and notifies the user
     */
    private fun onResponse(project: Project, response: InjectionStatus?) {
        when (response) {
            is InjectionStatus.Ok -> postOk(project, response)
            is InjectionStatus.BadConnection -> postError(project, "Connection Failed", response.error)
            is InjectionStatus.Bad -> postError(project, "Injection Failed", response.error)
            else -> postError(project, "Invalid response", "Caos injection failed to respond")
        }
    }

    /**
     * Responsible for displaying an OK status response to the user
     */
    @JvmStatic
    internal fun postOk(project: Project, response: InjectionStatus.Ok) {
        val prefix = "&gt;"
        val message = response.response.trim().nullIfEmpty()?.let {
            "<pre>\n$prefix" + it.split("\n").joinToString("\n$prefix").escapeHTML() + "</pre>"
        } ?: ""
        invokeLater {
            CaosInjectorNotifications.show(project, "Injection Success", message, NotificationType.INFORMATION)
        }
    }

    /**
     * Logs an INFO based message to the user such as empty CAOS string
     */
    @JvmStatic
    internal fun postInfo(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.INFORMATION)
        }
    }

    /**
     * Posts an ERROR message to the user.
     */
    @JvmStatic
    internal fun postError(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.ERROR)
        }
    }

    /**
     * Logs warning message to users CAOS notification panel
     */
    @Suppress("unused")
    @JvmStatic
    fun postWarning(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.WARNING)
        }
    }


    /**
     * Creates the actual connection to the game
     * If connection fails or is unsupported, returns null
     */
    private fun connection(variant: CaosVariant, project: Project): CaosConnection? {
        val conn = getConnectionObject(variant, project)
        if (!conn.connect()) {
            return null
        }
        return conn
    }

    /**
     * Gets the raw connection object without testing connection or actually connecting
     */
    private fun getConnectionObject(variant: CaosVariant, project: Project): CaosConnection {
        val injectUrl = runReadAction { CaosScriptProjectSettings.getInjectURL(project) }
        if (injectUrl != null) {
            if (injectUrl.startsWith("wine:")) {
                return WineConnection(injectUrl.substring(5), variant)
            }
            return PostConnection(injectUrl, variant)
        }
        return when (variant) {
            CaosVariant.C1 -> DDEConnection(variant)
            CaosVariant.C2 -> DDEConnection(variant)
            else -> C3Connection(variant)
        }
    }

    /**
     * Checks if it is possible to connect to variant
     */
    fun canConnectToVariant(variant: CaosVariant): Boolean {
        return when (variant) {
            CaosVariant.C1 -> true
            CaosVariant.C2 -> true
            CaosVariant.CV -> true
            CaosVariant.C3 -> true
            CaosVariant.DS -> true
            CaosVariant.SM -> true
            else -> false
        }
    }


    /**
     * Holds whether the credits for the Connection were called yet this run of the IDE
     * All code to inject CAOS was written by other people. I need to credit them
     */
    private val creditsCalled = mutableMapOf(
            CaosVariant.C1 to false,
            CaosVariant.C2 to false,
            CaosVariant.CV to false,
            CaosVariant.C3 to false,
            CaosVariant.DS to false,
            CaosVariant.SM to false
    )

}

/**
 * Defines a CAOS connection
 * Plugin supports multiple game connections such as DDE (C1e), MemoryMapped(C2e), HTTP(Any)
 */
internal interface CaosConnection {
    fun inject(caos: String): InjectionStatus
    fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber: Int, caos: String): InjectionStatus
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun connect(silent: Boolean = false): Boolean
    fun showAttribution(project: Project, variant: CaosVariant)
}

/**
 * The status for an injection request
 */
internal sealed class InjectionStatus {
    data class Ok(val response: String) : InjectionStatus()
    data class Bad(val error: String) : InjectionStatus()
    data class BadConnection(val error: String) : InjectionStatus()
}