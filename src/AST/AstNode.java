package AST;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {

    PROGRAM,
    Import,
    Chained_Import,
    ID;

    private final String name;

    AstNode() {
        this.name = SpecsStrings.toCamelCase(name(),"_", true);
    }

    @Override
    public String toString() {
        return name;
    }

}
