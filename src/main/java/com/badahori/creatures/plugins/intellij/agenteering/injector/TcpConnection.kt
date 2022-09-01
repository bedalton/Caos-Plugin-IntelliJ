@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.openapi.project.Project

/**
 * Class for managing a connection to C3 for CAOS injection
 */
internal class TcpConnection(private val gameName: String?, private val url: String) : CaosConnection {

    override val supportsJect: Boolean
        get() = false


    private fun notImplemented(): InjectionStatus {
        return InjectionStatus.BadConnection("TCP injection is not implemented")
    }

    override fun inject(caos: String): InjectionStatus {
        if (gameName == null) {
            return InjectionStatus.BadConnection("Cannot inject CAOS without game name")
        }
        return notImplemented()
    }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
        if (gameName == null) {
            return InjectionStatus.BadConnection("Cannot inject CAOS without game name")
        }
        val project = caos.project
        if (project.isDisposed) {
            return InjectionStatus.BadConnection("Project already disposed")
        }
        CaosNotifications.showError(caos.project, "TCP Inject", "TCP inject does not support JECT injection")
        return InjectionStatus.BadConnection("TCP injection does not support bootstrap injection")
    }

    override fun injectEventScript(
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
    ): InjectionStatus {
        if (gameName == null) {
            return InjectionStatus.BadConnection("Cannot inject CAOS without game name")
        }
        return notImplemented()
    }

    override fun disconnect(): Boolean {
        return true
    }

    override fun isConnected(): Boolean {
        return false
    }

    override fun connect(silent: Boolean): Boolean {
        return false
    }

    override fun showAttribution(project: Project, variant: CaosVariant) {
        // TODO credit TCP inject creator when implemented
    }


}