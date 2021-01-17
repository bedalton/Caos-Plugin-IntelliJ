package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import org.junit.Test

class Caos2CobC1Test {

    @Test
    fun Caos2CompilerCompilesCorrectly() {
        val agentName = "TestAgent"
        val cobName = "Test Cob Name"
        val expiryYear = 2021
        val expiryMonth = 10
        val expiryDay = 12
        val creationYear = 2020
        val creationMonth = 5
        val creationDay = 14
        val lastUsageYear = 2020
        val lastUsageMonth = 5
        val lastUsageDay = 1
        val version = 3
        val revision = 6
        val reuseInterval = 10
        val authorName = "The COBBLER Magnificent"
        val authorUrl =  "https://www.openc2e.org/cobbler-magnificent"
        val authorEmail = "cobblerm@openc2e.org"
        val authorComments = "I LOVE MAKING COBS!!!!"
        val agentDescription = "This Cob is simply a test COB, and will crash your game if injected"

        // Scripts
        val installScriptBody = "dde: puts [Install Script]"
        val removalScriptBody = "dde: puts [Removal Script]"
        val activate1ScriptBody = "doif actv eq 1,setv actv 0,endi"
        val timerScriptBody = "addv obv0 1"
        """
            **Caos2Cob
            *# AgentName = "$agentName"
            *# Cob Name = "$cobName"
            *# expiry = $expiryYear-$expiryMonth-$expiryDay
            *# Last Usage Date = $lastUsageYear-$lastUsageMonth-$lastUsageDay
            *# Author Name = "$authorName"
            *# Author URL = "$authorUrl"
            *# Author Email = "$authorEmail"
            *# Author Comments = "$authorComments"
            *# agent description = "$agentDescription"
            *# Creation Date = $creationYear-$creationMonth-$creationDay
            *# Version = $version
            *# revision = $revision
            *# Reuse Interval = $reuseInterval
            
            iscr
                $installScriptBody
            endm
            
            scrp 1 188 100 9
                $timerScriptBody
            endm
            
            scrp 1 188 100 1
                $activate1ScriptBody
            endm
            
            rscr
                $removalScriptBody
            endm
        """.trimIndent()
    }
}