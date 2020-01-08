package com.openc2e.plugins.intellij.caos.stubs.types;

import org.jetbrains.annotations.NotNull;

public interface CaosScriptStubTypes {
    //@NotNull
    //CaosScriptCommandStubType COMMAND = new CaosScriptCommandStubType("CaosScript_COMMAND");

    //@NotNull
    //CaosScriptCommandCallStubType COMMAND_CALL = new CaosScriptCommandCallStubType("CaosScript_COMMAND_CALL");

    @NotNull
    CaosScriptCommandTokenStubType COMMAND_TOKEN = new CaosScriptCommandTokenStubType("CaosScript_COMMAND_TOKEN");

    @NotNull
    CaosScriptExpressionStubType EXPRESSION = new CaosScriptExpressionStubType("CaosScript_EXPRESSION");

    @NotNull
    CaosScriptVarTokenStubType VAR_TOKEN = new CaosScriptVarTokenStubType("CaosScript_VAR_TOKEN");
    CaosScriptFileStubType FILE = new CaosScriptFileStubType();
}
