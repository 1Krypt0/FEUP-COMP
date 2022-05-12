package AST;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {

    PROGRAM,
    Import,
    Chained_Import,
    I_D,
    Class_Decl,
    Method_Declaration,
    If,
    Main,
    Var_Decl,
    Method_Body,
    While_Condition,
    True,
    False,
    Bin_Op,
    Negation,
    Integer_Literal,
    Array_Access, Return_Statement, Assign, Dot_Linked;

    private final String name;

    AstNode() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String toString() {
        return name;
    }

}
