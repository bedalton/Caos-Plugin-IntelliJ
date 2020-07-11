package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant

open class CaosInjectorException(message:String? = null, throwable: Throwable? = null) : Exception (message, throwable)

class CaosInjectionException(message:String? = null, throwable: Throwable? = null): CaosInjectorException(message, throwable)

class CaosConnectionException(message:String = "Failed to connect to vivarium", throwable: Throwable? = null) : CaosInjectorException(message, throwable)

class CaosInjectorOutOfVariantException(val variant:CaosVariant, message:String? = null) : CaosInjectorException(message, null)