package AST;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {

    PROGRAM,
    Import,
    Chained_Import,
    ID,
    Class_Decl,
    Method_Declaration,
    Main, Var_Decl, Method_Body;

    private final String name;

    AstNode() {
        this.name = SpecsStrings.toCamelCase(name(),"_", true);
    }

    @Override
    public String toString() {
        return name;
    }

}