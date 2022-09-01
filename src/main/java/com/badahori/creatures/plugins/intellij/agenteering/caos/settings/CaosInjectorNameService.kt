package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.forKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants


interface HasGameInterfaces {
    var gameInterfaceNamesRaw: List<GameInterfaceName>
}
internal fun HasGameInterfaces.addGameInterfaceName(interfaceName: GameInterfaceName) {
    gameInterfaceNamesRaw = (gameInterfaceNamesRaw + interfaceName).distinct()
}
internal fun HasGameInterfaces.removeGameInterfaceName(interfaceName: GameInterfaceName) {
    gameInterfaceNamesRaw = gameInterfaceNamesRaw.filter { it != interfaceName }
}

internal val HasGameInterfaces.gameInterfaceNames: List<GameInterfaceName>
    get() {
        return gameInterfaceNames
            .flatMap { gameInterfaceName ->
                if (gameInterfaceName.code != "*")
                    listOf(gameInterfaceName)
                else
                    CaosConstants
                        .VARIANTS
                        .map { variant ->
                            gameInterfaceName.copy(
                                code = variant.code,
                                variant = variant
                            )
                        }
            }
    }

internal fun HasGameInterfaces.gameInterfaceNames(variant: CaosVariant?): List<GameInterfaceName> {
    val interfaces = gameInterfaceNames
    if (variant.nullIfUnknown() == null)
        return interfaces
    return interfaces.filter { it.isVariant(variant) }

}

internal val CaosVariant.lastInterfacePrefix get() = "$code=="

internal fun HasGameInterfaces.gameInterfaceForKey(variant: CaosVariant?, key: String): GameInterfaceName? {
    return gameInterfaceNames.forKey(variant, key)
}
