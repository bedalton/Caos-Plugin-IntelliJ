package com.openc2e.plugins.intellij.caos.stubs.types;

import org.jetbrains.annotations.NotNull;

public interface CaosScriptStubTypes {
    //@NotNull
    //CaosScriptCommandStubType COMMAND = new CaosScriptCommandStubType("CaosScript_COMMAND");

    @NotNull
    CaosScriptCommandCallStubType COMMAND_CALL = new CaosScriptCommandCallStubType("CaosScript_COMMAND_CALL");

    @NotNull
    CaosScriptExpressionStubType EXPRESSION = new CaosScriptExpressionStubType("CaosScript_EXPRESSION");

    @NotNull
    CaosScriptSubroutineStubType SUBROUTINE = new CaosScriptSubroutineStubType("CaosScript_SUBROUTINE");

    @NotNull
    CaosScriptVarTokenStubType VAR_TOKEN = new CaosScriptVarTokenStubType("CaosScript_VAR_TOKEN");

    @NotNull
    CaosScriptLValueStubType LVALUE = new CaosScriptVarTokenStubType("CaosScript_LVALUE");

    @NotNull
    CaosScriptRValueStubType RVALUE = new CaosScriptVarTokenStubType("CaosScript_RVALUE");

    @NotNull
    CaosScriptAssignmentStubType ASSIGNMENT = new CaosScriptAssignmentStubType("CaosScript_CAssignment");

    CaosScriptFileStubType FILE = new CaosScriptFileStubType();
}
