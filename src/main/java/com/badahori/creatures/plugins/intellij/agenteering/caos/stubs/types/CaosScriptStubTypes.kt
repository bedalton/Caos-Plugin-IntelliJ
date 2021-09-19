package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

interface CaosScriptStubTypes {
    companion object {
        @JvmStatic
        val COMMAND_CALL = CaosScriptCommandCallStubType("CaosScript_COMMAND_CALL")
        @JvmStatic
        val CAOS_2_BLOCK = CaosScriptCaos2BlockStubType("CaosScript_CAOS_2_BLOCK")
        @JvmStatic
        val CAOS_2_TAG = CaosScriptCaos2TagStubType("CaosScript_CAOS_2_TAG")
        @JvmStatic
        val CAOS_2_COMMAND = CaosScriptCaos2CommandStubType("CaosScript_CAOS_2_COMMAND")
        @JvmStatic
        val SUBROUTINE = CaosScriptSubroutineStubType("CaosScript_SUBROUTINE")
        @JvmStatic
        val VAR_TOKEN = CaosScriptVarTokenStubType("CaosScript_VAR_TOKEN")
        @JvmStatic
        val TARG_ASSIGNMENT = CaosScriptTargAssignmentStubType("CaosScript_CTarg")
        @JvmStatic
        val LVALUE = CaosScriptLValueStubType("CaosScript_LVALUE")
        @JvmStatic
        val RVALUE = CaosScriptRValueStubType("CaosScript_RVALUE")
        @JvmStatic
        val TOKEN_RVALUE = CaosScriptTokenRValueStubType("CaosScript_TOKEN_RVALUE")
        @JvmStatic
        val VAR_ASSIGNMENT = CaosScriptAssignmentStubType("CaosScript_CAssignment")
        @JvmStatic
        val NAMED_GAME_VAR = CaosScriptNamedGameVarStubType("CaosScript_NAMED_GAME_VAR")
        @JvmStatic
        val EVENT_SCRIPT = CaosScriptEventScriptStubType("CaosScript_EVENT_SCRIPT")
        @JvmStatic
        val RNDV = CaosScriptRndvStubType("CaosScript_C_RNDV")
        @JvmStatic
        val MACRO = CaosScriptMacroStubType("CaosScript_MACRO")
        @JvmStatic
        val INSTALL_SCRIPT = CaosScriptInstallScriptStubType("CaosScript_INSTALL_SCRIPT")
        @JvmStatic
        val REMOVAL_SCRIPT = CaosScriptRemovalScriptStubType("CaosScript_REMOVAL_SCRIPT")
        @JvmStatic
        val SUBROUTINE_NAME = CaosScriptSubroutineNameStubType("CaosScript_SUBROUTINE_NAME")
        @JvmStatic
        val FILE = CaosScriptFileStubType()
        @JvmStatic
        val RVALUE_PRIME = CaosScriptRValuePrimeStubType("CaosScript_RVALUE_PRIME")
    }
}