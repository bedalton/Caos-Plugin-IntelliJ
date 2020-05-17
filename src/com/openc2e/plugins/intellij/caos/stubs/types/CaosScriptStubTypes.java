package com.openc2e.plugins.intellij.caos.stubs.types;

import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptConstantAssignment;
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
    CaosScriptTargAssignmentStubType TARG_ASSIGNMENT = new CaosScriptTargAssignmentStubType("CaosScript_CTarg");

    @NotNull
    CaosScriptLValueStubType LVALUE = new CaosScriptLValueStubType("CaosScript_LVALUE");

    @NotNull
    CaosScriptRValueStubType RVALUE = new CaosScriptRValueStubType("CaosScript_RVALUE");

    @NotNull
    CaosScriptAssignmentStubType ASSIGNMENT = new CaosScriptAssignmentStubType("CaosScript_CAssignment");

    @NotNull
    CaosScriptConstantAssignmentStubType CONSTANT_ASSIGNMENT = new CaosScriptConstantAssignmentStubType("CaosScript_CONSTANT_ASSIGNMENT")

    CaosScriptFileStubType FILE = new CaosScriptFileStubType();
}
