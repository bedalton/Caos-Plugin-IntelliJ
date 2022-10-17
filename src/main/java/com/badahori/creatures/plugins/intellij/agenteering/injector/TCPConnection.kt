@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.openapi.project.Project
import java.net.URL


/**
 * Connection class for managing POST based CAOS injection
 * Requires CAOS server executable to be running to handle caos injection post requests
 */
internal class TCPConnection(private val variant: CaosVariant?, private val gameInterfaceName: TCPInjectorInterface) : CaosConnection {

    override val supportsJect: Boolean
        get() = false

    private val url: URL? by lazy {
        gameInterfaceName.getURL(variant)
    }

    override fun inject(caos: String): InjectionStatus {
//        val url = url
//            ?: return InjectionStatus.BadConnection("Invalid URL for POST http connection")
        return InjectionStatus.ActionNotSupported("20kdc's TCP connection protocol is not yet supported")
    }

    override fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus {
        return InjectionStatus.ActionNotSupported("JECT not supported by TCP connections")
    }

    override fun injectEventScript(
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String
    ): InjectionStatus {

        if (!isImplemented()) {
            return InjectionStatus.ActionNotSupported("20kdc's TCP connection protocol is not yet supported")
        }

        val expectedHeader = "scrp $family $genus $species $eventNumber"
        val removalRegex = "^scrp\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s*".toRegex()
        // Does not have expected event script header
        val caosFormatted = if (!caos.trim().lowercase().startsWith(expectedHeader)) {
            // If script has a header, but for a different family, genus, species and event number
            // Replace the bad header with the correct one
            val stripped = if (removalRegex.matches(caos)) {
                caos.replace(removalRegex, "").trim()
            } else {
                caos.trim()
            }
            // Combine the expected header with the script body
            "$expectedHeader $stripped"
        } else
            caos
        return inject(caosFormatted)
    }

    override fun disconnect(): Boolean = true

    override fun isConnected(): Boolean = true

    override fun connect(silent: Boolean): Boolean = true

    override fun showAttribution(project: Project, variant: CaosVariant) {
//        postInfo(project, "", "Requires CAOS injector server from Bedalton")
    }

    companion object {
        @JvmStatic
        fun isImplemented() = false
    }

}