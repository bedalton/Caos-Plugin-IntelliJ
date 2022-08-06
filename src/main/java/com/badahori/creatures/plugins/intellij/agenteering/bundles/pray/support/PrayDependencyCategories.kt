package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

import bedalton.creatures.agents.pray.compiler.pray.bestDependencyCategoryForFile

internal object PrayDependencyCategories {

    private const val DIRECTORY_RESTRICTED_WORD = "restricted"

    val privateCategoryDirectories = listOf(
        8, // Bootstrap
        9, // World Files
        11 // Pray Files
    )

    val categoryNames by lazy {
        mapOf(
            0 to "Main",
            1 to "Sounds",
            2 to "Images",
            3 to "Genetics",
            4 to "Body Data",
            5 to "Overlay",
            6 to "Backgrounds",
            7 to "Catalogue",
            8 to "Bootstrap",
            9 to "Worlds",
            10 to "Creatures",
            11 to "Pray Files",
        )
    }

    val userAccessible by lazy {
        categoryNames
            .filter { it.key !in privateCategoryDirectories }
            .toMap()
    }


    fun dependencyCategoryName(categoryId: Int, appendDenied: Boolean): String? {
        val directory = categoryNames[categoryId]
            ?: return null
        if (appendDenied && categoryId in privateCategoryDirectories) {
            return "$directory ($DIRECTORY_RESTRICTED_WORD)"
        } else {
            return directory
        }
    }

    fun getBestCategory(fileName: String): Int? {
        return bestDependencyCategoryForFile(fileName)
    }

}