package com.badahori.creatures.plugins.intellij.agenteering.injector

open class CaosInjectorException(message:String? = null, throwable: Throwable? = null) : Exception (message, throwable)

class CaosConnectionException(message:String = "Failed to connect to vivarium", throwable: Throwable? = null) : CaosInjectorException(message, throwable)
