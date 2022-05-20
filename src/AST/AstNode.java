package AST;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {

    PROGRAM,
    Import,
    Chained_Import,
    ID,
    I_D,
    Class_Decl,
    Method_Declaration,
    Main, Var_Decl,
    Method_Body,
    Init,
    Assign,
    If,
    While,
    Method_Call,
    Return_Statement,
    Negation,
    Array_Access,
    Type,
    Bin_Op,
    Integer_Literal,
    False,
    True,
    Array_Creation,
    Class_Creation, Dot_Linked, This, Length, Arg, While_Condition;

    private final String name;

    AstNode() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String toString() {
        return name;
    }

}
