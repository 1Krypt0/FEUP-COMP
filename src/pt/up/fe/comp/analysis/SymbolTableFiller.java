package pt.up.fe.comp.analysis;

import AST.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class SymbolTableFiller extends PreorderJmmVisitor<ProgramSymbolTable, Integer> {

    public SymbolTableFiller() {
        addVisit(AstNode.Import, this::visitImportDecl);
        addVisit(AstNode.Class_Decl, this::classDeclVisit);
        // addVisit(AstNode.Chained_Import, this::visitImportDecl);
    }

    private Integer visitImportDecl(JmmNode importDecl, ProgramSymbolTable symbolTable) {
        System.out.println("Visiting import decl");

        var visitor = new ImportVisitor();
        visitor.visit(importDecl, symbolTable);

        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, ProgramSymbolTable symbolTable) {
        System.out.println("Visiting class decl");

        symbolTable.setClassName(classDecl.get("name"));

        classDecl.getOptional("extended_class").ifPresent(symbolTable::setSuperClass);

        return 0;
    }

}
