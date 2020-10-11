package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types;

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CaosScriptStubTypes {
    //@NotNull
    //CaosScriptCommandStubType COMMAND = new CaosScriptCommandStubType("CaosScript_COMMAND");

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
    CaosScriptConstantAssignmentStubType CONSTANT_ASSIGNMENT = new CaosScriptConstantAssignmentStubType("CaosScript_CONSTANT_ASSIGNMENT");

    @NotNull
    CaosScriptNamedVarAssignmentStubType NAMED_VAR_ASSIGNMENT = new CaosScriptNamedVarAssignmentStubType("CaosScript_NAMED_VAR_ASSIGNMENT");

    @NotNull
    CaosScriptNamedVarStubType NAMED_VAR = new CaosScriptNamedVarStubType("CaosScript_NAMED_VAR");

    @NotNull
    CaosScriptNamedConstantStubType NAMED_CONSTANT = new CaosScriptNamedConstantStubType("CaosScript_NAMED_CONSTANT");

    @NotNull
    CaosScriptExpectsIntStubType EXPECTS_INT = new CaosScriptExpectsIntStubType("CaosScript_EXPECTS_INT");

    @NotNull
    CaosScriptExpectsFloatStubType EXPECTS_FLOAT = new CaosScriptExpectsFloatStubType("CaosScript_EXPECTS_FLOAT");

    @NotNull
    CaosScriptExpectsAgentStubType EXPECTS_AGENT = new CaosScriptExpectsAgentStubType("CaosScript_EXPECTS_AGENT");

    @NotNull
    CaosScriptExpectsQuoteStringStubType EXPECTS_QUOTE_STRING = new CaosScriptExpectsQuoteStringStubType("CaosScript_EXPECTS_STRING");

    @NotNull
    CaosScriptExpectsC1StringStubType EXPECTS_C1_STRING = new CaosScriptExpectsC1StringStubType("CaosScript_EXPECTS_C1_STRING");

    @NotNull
    CaosScriptExpectsByteStringStubType EXPECTS_BYTE_STRING = new CaosScriptExpectsByteStringStubType("CaosScript_EXPECTS_ByteString");

    @NotNull
    CaosScriptExpectsDecimalStubType EXPECTS_DECIMAL = new CaosScriptExpectsDecimalStubType("CaosScript_EXPECTS_DECIMAL");

    @NotNull
    CaosScriptExpectsValueStubType EXPECTS_VALUE = new CaosScriptExpectsValueStubType("CaosScript_EXPECTS_VALUE");

    @NotNull
    CaosScriptExpectsTokenStubType EXPECTS_TOKEN = new CaosScriptExpectsTokenStubType("CaosScript_EXPECTS_TOKEN");

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

    CaosScriptFileStubType FILE = new CaosScriptFileStubType();
    CaosScriptRValuePrimeStubType RVALUE_PRIME = new CaosScriptRValuePrimeStubType("CaosScript_RVALUE_PRIME");
    @NotNull
    CaosScriptExpressionStubType EXPRESSION = new CaosScriptExpressionStubType("CaosScript_EXPRESSION");
}
