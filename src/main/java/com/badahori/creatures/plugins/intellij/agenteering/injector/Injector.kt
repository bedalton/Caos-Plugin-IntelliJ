package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.escapeHTML
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

object Injector {

    private fun getActualVersion(project: Project, variant: CaosVariant): CaosVariant {
        if (variant.isNotOld) {
            return variant
        }
        val code = "dde: putv vrsn"
        val response = injectPrivate(project, variant, code)
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

    fun inject(project: Project, variant: CaosVariant, caosIn: String): Boolean {
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
        val response = injectPrivate(project, variant, caosIn)
        onResponse(project, response)
        return response is InjectionStatus.Ok
    }
    fun injectEventScript(project: Project, variant: CaosVariant, family: Int, genus:Int, species: Int, eventNumber: Int, caosIn: String): Boolean {
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
        val response = injectPrivate(project, variant, family, genus, species, eventNumber, caosIn)
        onResponse(project, response)
        return response is InjectionStatus.Ok
    }

    private fun onResponse(project: Project, response: InjectionStatus?) {
        when (response) {
            is InjectionStatus.Ok -> postOk(project, response)
            is InjectionStatus.BadConnection -> postError(project, "Connection Failed", response.error)
            is InjectionStatus.Bad -> postError(project, "Injection Failed", response.error)
            else -> postError(project, "Invalid response", "Caos injection failed to respond")
        }
    }

    private fun injectPrivate(project: Project, variant: CaosVariant, caosIn: String): InjectionStatus? {
        val connection = connection(variant, project)
                ?: return InjectionStatus.BadConnection("Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again")
        if (!creditsCalled.containsKey(variant)) {
            creditsCalled[variant] = false
        }
        if (creditsCalled[variant].orFalse()) {
            creditsCalled[variant] = true
            connection.showAttribution(project, variant)
        }
        val caos = sanitize(caosIn)
        if (!connection.isConnected() && !connection.connect(false)) {
            return null
        }
        return connection.inject(caos)
    }

    private fun injectPrivate(project: Project, variant: CaosVariant, family: Int, genus:Int, species: Int, eventNumber: Int, caosIn: String): InjectionStatus? {
        val connection = connection(variant, project)
                ?: return InjectionStatus.BadConnection("Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again")
        if (!creditsCalled.containsKey(variant)) {
            creditsCalled[variant] = false
        }
        if (creditsCalled[variant].orFalse()) {
            creditsCalled[variant] = true
            connection.showAttribution(project, variant)
        }
        val caos = sanitize(caosIn)
        if (!connection.isConnected() && !connection.connect(false)) {
            return null
        }
        return connection.injectEventScript(family, genus, species, eventNumber, caos)
    }

    private fun connection(variant: CaosVariant, project: Project): CaosConnection? {
        val conn = getConnection(variant, project)
        if (conn == null || !conn.connect()) {
            return null
        }
        return conn
    }

    @JvmStatic
    internal fun postOk(project: Project, response: InjectionStatus.Ok) {
        val prefix = "&gt;"
        val message = response.response.trim().nullIfEmpty()?.let {
            "<pre>\n$prefix" + it.split("\n").joinToString("\n$prefix").escapeHTML()+"</pre>"
        } ?: ""
        invokeLater {
            CaosInjectorNotifications.show(project, "Injection Success", message, NotificationType.INFORMATION)
        }
    }

    @JvmStatic
    internal fun postInfo(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.INFORMATION)
        }
    }

    @JvmStatic
    internal fun postError(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.ERROR)
        }
    }

    @JvmStatic
    fun postWarning(project: Project, title: String, message: String) {
        invokeLater {
            CaosInjectorNotifications.show(project, title, message, NotificationType.WARNING)
        }
    }

    private fun getConnection(variant: CaosVariant, project: Project): CaosConnection? {
        val injectUrl = runReadAction { CaosScriptProjectSettings.getInjectURL(project) }
        if (injectUrl != null) {
            if (injectUrl.startsWith("wine:")) {
                return WineConnection(injectUrl.substring(5), variant);
            }
            return PostConnection(injectUrl, variant)
        }
        return when (variant) {
            CaosVariant.C1 -> DDEConnection(variant)
            CaosVariant.C2 -> DDEConnection(variant)
            else -> C3Connection(variant)
        }
    }

    private fun sanitize(caos: String): String {
        var out = caos
        out = out.replace("[ ]+".toRegex(), " ")
        out = out.replace("[ ]*,[ ]*".toRegex(), ",").trim()
        return out
    }

    fun canConnectToVariant(variant: CaosVariant): Boolean {
        return when (variant) {
            CaosVariant.C1 -> true
            CaosVariant.C2 -> true
            CaosVariant.CV -> true
            CaosVariant.C3 -> true
            CaosVariant.DS -> true
            else -> false
        }
    }


    private val creditsCalled = mutableMapOf(
            CaosVariant.C1 to false,
            CaosVariant.C2 to false,
            CaosVariant.CV to false,
            CaosVariant.C3 to false,
            CaosVariant.DS to false
    )

}

internal interface CaosConnection {
    fun inject(caos: String): InjectionStatus
    fun injectEventScript(family:Int, genus:Int, species:Int, eventNumber:Int, caos:String): InjectionStatus
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun connect(silent: Boolean = false): Boolean
    fun showAttribution(project: Project, variant: CaosVariant)
}

internal sealed class InjectionStatus {
    data class Ok(val response: String) : InjectionStatus()
    data class Bad(val error: String) : InjectionStatus()
    data class BadConnection(val error: String) : InjectionStatus()
}