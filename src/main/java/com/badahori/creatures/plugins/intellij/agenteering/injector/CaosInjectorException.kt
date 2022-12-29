package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

open class CaosInjectorException(message:String? = null, throwable: Throwable? = null) : Exception (message, throwable)

class CaosConnectionException(message:String = "Failed to connect to vivarium", throwable: Throwable? = null) : CaosInjectorException(message, throwable)


internal data class CaosInjectorExceptionWithStatus(private val initialInjectionStatus: InjectionStatus?, val fileName: String) : Exception(
    when (initialInjectionStatus) {
        is InjectionStatus.Bad -> initialInjectionStatus.error
        is InjectionStatus.Ok -> "OK Response was thrown... Response: " + initialInjectionStatus.response
        is InjectionStatus.BadConnection -> initialInjectionStatus.formattedError()
        is InjectionStatus.Pending -> "Error thrown while pending injection completion"
        else -> "Exception thrown without injection response"
    }
) {
    val injectionStatus: InjectionStatus? get() = if (initialInjectionStatus is InjectionStatus.Pending) {
        initialInjectionStatus.resultOrNull()
    } else {
        initialInjectionStatus
    }
}
