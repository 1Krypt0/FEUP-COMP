package pt.up.fe.comp.analysis;

import AST.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymbolTableFiller extends PreorderJmmVisitor<ProgramSymbolTable, Integer> {

    private final List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    public SymbolTableFiller() {
        this.reports = new ArrayList<>();

        addVisit(AstNode.Import, this::visitImportDecl);
        addVisit(AstNode.Class_Decl, this::classDeclVisit);
        addVisit(AstNode.Method_Declaration, this::methodDeclVisit);
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

    private Integer methodDeclVisit(JmmNode methodDecl, ProgramSymbolTable symbolTable) {
        System.out.println("Visiting method decl");

        // Can be NewMethodDecl or Main
        JmmNode methodDeclaration = methodDecl.getJmmChild(0);
        String methodName = methodDeclaration.get("name");

        if(symbolTable.hasMethod(methodName)) {
            this.reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC, Integer.parseInt(methodDecl.get("line")),
                    Integer.parseInt(methodDecl.get("col")), "Method already declared: " + methodName ));
            return -1;
        }

        symbolTable.addMethod(methodName, null , Collections.emptyList());

        // return type
        // parameters

        return 0;
    }

}
