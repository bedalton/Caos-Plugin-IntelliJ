package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types;

import org.jetbrains.annotations.NotNull;

public interface CaosScriptStubTypes {
    @NotNull
    CaosScriptCommandCallStubType COMMAND_CALL = new CaosScriptCommandCallStubType("CaosScript_COMMAND_CALL");

    @NotNull
    CaosScriptSubroutineStubType SUBROUTINE = new CaosScriptSubroutineStubType("CaosScript_SUBROUTINE");

    @NotNull
    CaosScriptVarTokenStubType VAR_TOKEN = new CaosScriptVarTokenStubType("CaosScript_VAR_TOKEN");

    @NotNull
    CaosScriptTargAssignmentStubType TARG_ASSIGNMENT = new CaosScriptTargAssignmentStubType("CaosScript_CTarg");

    @NotNull
    CaosScriptLValueStubType LVALUE = new CaosScriptLValueStubType("CaosScript_LVALUE");

    @NotNull
    CaosScriptRValueStubType RVALUE = new CaosScriptRValueStubType("CaosScript_RVALUE");

    @NotNull
    CaosScriptTokenRValueStubType TOKEN_RVALUE = new CaosScriptTokenRValueStubType("CaosScript_TOKEN_RVALUE");

    @NotNull
    CaosScriptAssignmentStubType VAR_ASSIGNMENT = new CaosScriptAssignmentStubType("CaosScript_CAssignment");

    @NotNull
    CaosScriptNamedGameVarStubType NAMED_GAME_VAR = new CaosScriptNamedGameVarStubType("CaosScript_NAMED_GAME_VAR");

    @NotNull
    CaosScriptEventScriptStubType EVENT_SCRIPT = new CaosScriptEventScriptStubType("CaosScript_EVENT_SCRIPT");

    @NotNull
    CaosScriptRndvStubType RNDV = new CaosScriptRndvStubType("CaosScript_C_RNDV");


    @NotNull
    CaosScriptMacroStubType MACRO = new CaosScriptMacroStubType("CaosScript_MACRO");

    @NotNull
    CaosScriptInstallScriptStubType INSTALL_SCRIPT = new CaosScriptInstallScriptStubType("CaosScript_INSTALL_SCRIPT");

    @NotNull
    CaosScriptRemovalScriptStubType REMOVAL_SCRIPT = new CaosScriptRemovalScriptStubType("CaosScript_REMOVAL_SCRIPT");

    @NotNull
    CaosScriptFileStubType FILE = new CaosScriptFileStubType();

    @NotNull
    CaosScriptRValuePrimeStubType RVALUE_PRIME = new CaosScriptRValuePrimeStubType("CaosScript_RVALUE_PRIME");
}
