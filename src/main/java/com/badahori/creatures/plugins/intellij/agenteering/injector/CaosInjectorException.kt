package com.badahori.creatures.plugins.intellij.agenteering.injector

open class CaosInjectorException(message:String? = null, throwable: Throwable? = null) : Exception (message, throwable)

class CaosConnectionException(message:String = "Failed to connect to vivarium", throwable: Throwable? = null) : CaosInjectorException(message, throwable)


internal data class CaosInjectorExceptionWithStatus(val injectionStatus: InjectionStatus?) : Exception(
    when (injectionStatus) {
        is InjectionStatus.Bad -> injectionStatus.error
        is InjectionStatus.Ok -> "OK Response was thrown... Response: " + injectionStatus.response
        is InjectionStatus.BadConnection -> injectionStatus.error
        else -> "Injection failed without injection response"
    }
)
