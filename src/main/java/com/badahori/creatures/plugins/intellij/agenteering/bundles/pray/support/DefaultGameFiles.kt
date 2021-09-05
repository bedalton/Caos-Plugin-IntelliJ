package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

object DefaultGameFiles {

    val C3Files: List<String> by lazy {
        this::class.java.classLoader?.getResource("support/DefaultC3Files.txt")!!.readText(Charsets.UTF_8)
            .split("\n")
            .filter { it.isNotBlank() }
            .map { it.trim()}
    }
    val DSFiles: List<String> by lazy {
        this::class.java.classLoader?.getResource("support/DefaultC3Files.txt")!!.readText(Charsets.UTF_8)
            .split("\n")
            .filter { it.isNotBlank() }
            .map { it.trim()}
    }

    val C3DSFiles: List<String> by lazy {
        (C3Files + DSFiles).distinct()
    }

}