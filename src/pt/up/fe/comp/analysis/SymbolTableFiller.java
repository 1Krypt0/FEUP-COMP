package pt.up.fe.comp.analysis;

import AST.AstNode;
import AST.AstUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        addVisit(AstNode.Main, this::methodDeclVisit);
        // addVisit(AstNode.Var_Decl, this::varDeclVisit);

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

        // TODO: DOUBT is this correct can we visit the fields here or difernciate in the grammar itself ?
        List<JmmNode> varDeclarations = classDecl.getChildren().subList(0, classDecl.getChildren().size())
                    .stream().filter(node -> node.getKind().equals("VarDecl")).collect(Collectors.toList());

        if (varDeclarations.size() > 0) {
            List<Symbol> fields = AstUtils.parseFields(varDeclarations);
            fields.forEach(symbolTable::addField);
        }

        return 0;
    }

    private Integer methodDeclVisit(JmmNode methodDecl, ProgramSymbolTable symbolTable) {
        System.out.println("Visiting method decl");

        // NewMethodDecl
        String methodName = methodDecl.get("name");

        if(symbolTable.hasMethod(methodName)) {
            this.reports.add(new Report(ReportType.ERROR,Stage.SEMANTIC, Integer.parseInt(methodDecl.get("line")),
                    Integer.parseInt(methodDecl.get("col")), "Method already declared: " + methodName ));
            return -1;
        }

        // TODO: DOUBT check if String[] is a valid type
        // TODO: DOUBT should we change the type to have the name of variable?

        JmmNode returnType = methodDecl.getJmmChild(0);

        if(methodDecl.getKind().equals("Main")) {
            Symbol main = new Symbol(new Type("String", true), methodDecl.get("arg_array"));
            //new list with main
            List<Symbol> main_list = new ArrayList<>();
            main_list.add(main);

            symbolTable.addMethod(methodName, new Type("void", false) , main_list);
        }
        else{
            boolean isArray = returnType.getOptional("is_array").map(Boolean::parseBoolean).orElse(false);
            String type = returnType.get("type");
            List<JmmNode> params = methodDecl.getChildren().subList(0, methodDecl.getChildren().size())
                    .stream().filter(node -> node.getKind().equals("Param")).collect(Collectors.toList());

            List<Symbol> parameters =  params.stream().map(AstUtils::builParamTypeObject).collect(Collectors.toList());
            symbolTable.addMethod(methodName, new Type(type, isArray) , parameters);
        }

        return 0;
    }


}
