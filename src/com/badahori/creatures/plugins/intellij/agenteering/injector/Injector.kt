package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.now
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Injector {

    private fun getActualVersion(project: Project, variant: CaosVariant) : CaosVariant {
        if (variant.isNotOld) {
            return variant;
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
        } catch(e:Exception) {
            variant
        }
    }

    fun inject(project: Project, variant: CaosVariant, caosIn:String) : Boolean {
        if (!canConnectToVariant(variant)) {
            val error = "Injection to ${variant.fullName} is not yet implemented"
            CaosInjectorNotifications.show(project, "ConnectionException", error, NotificationType.ERROR)
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
        when (response) {
            is InjectionStatus.Ok -> postOk(project, response)
            is InjectionStatus.BadConnection -> postError(project, "Connection Failed", response.error)
            is InjectionStatus.Bad -> postError(project, "Injection Failed", response.error)
        }
        return response is InjectionStatus.Ok
    }
    private fun injectPrivate(project: Project, variant: CaosVariant, caosIn:String) : InjectionStatus? {
        val connection = connection(project, variant)
                ?: return null
        val caos = sanitize(caosIn)
        if (!connection.isConnected() && !connection.connect(false)) {
            return null
        }
        return connection.inject(caos)
    }

    private fun connection(project: Project, variant: CaosVariant): CaosConnection? {
        val conn = getConnection(variant)
        if (conn == null || !conn.connect()) {
            CaosInjectorNotifications.show(project, "Connection Failed", "Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again", NotificationType.ERROR)
            return null
        }
        return conn
    }

    @JvmStatic
    internal fun postOk(project: Project, response:InjectionStatus.Ok) {
        val responseText = response.response.nullIfEmpty()?.let {
            "\n\tOutput: $it"
        } ?: ""
        val message = "status: OK$responseText"
        CaosInjectorNotifications.show(project, "Injection Success", message, NotificationType.INFORMATION)
    }

    @JvmStatic
    internal fun postInfo(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.INFORMATION)
    }

    @JvmStatic
    internal fun postError(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.ERROR)
    }

    @JvmStatic
    fun postWarning(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.WARNING)
    }

    private fun getConnection(variant: CaosVariant) : CaosConnection? {
        return when (variant) {
            CaosVariant.C1 -> DDEConnection(variant)
            CaosVariant.C2 -> DDEConnection(variant)
            else -> C3Connection(variant)
        }
    }

    private fun sanitize(caos:String) : String {
        var out = caos
        out = out.replace("[ ]+".toRegex(), " ")
        out.replace("[ ]*,[ ]*".toRegex(), ",")
        return out.trim()
    }

    fun canConnectToVariant(variant: CaosVariant): Boolean {
        return when (variant) {
            CaosVariant.C1 -> true
            CaosVariant.C2 -> true
            CaosVariant.CV -> false
            CaosVariant.C3 -> true
            CaosVariant.DS -> true
            else -> false
        }
    }

}

internal interface CaosConnection {
    fun inject(caos: String): InjectionStatus
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun connect(silent: Boolean = false): Boolean
}

private data class CaosResponse(val caos:String, val success:Boolean, val response:String? = null, val error:String? = null, val time:Long = now)

internal sealed class InjectionStatus {
    data class Ok(val response:String): InjectionStatus()
    data class Bad(val error:String) : InjectionStatus()
    data class BadConnection(val error:String) : InjectionStatus()
}