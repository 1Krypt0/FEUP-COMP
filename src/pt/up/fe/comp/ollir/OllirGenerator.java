package pt.up.fe.comp.ollir;

import AST.AstNode;
import AST.AstUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer,Integer> {

    private final StringBuilder codeString;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable) {
        this.codeString = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit(AstNode.Init, this::initVisit);
        addVisit(AstNode.Class_Decl, this::classDeclVisitor);
        addVisit(AstNode.Method_Declaration, this::methodDeclVisitor);
        addVisit(AstNode.Main, this::methodDeclVisitor);
    }

    private Integer methodDeclVisitor(JmmNode methodDecl, Integer integer) {

        System.out.println("Optimizing method " + methodDecl.get("name"));

        String methodName = methodDecl.get("name");
        codeString.append(".method public ");
        if(methodDecl.getKind().equals("Main")) {
            codeString.append("static");
        }
        codeString.append(" ").append(methodName);


        List<Symbol> parameters =  symbolTable.getParameters(methodName);
        String parameterString = parameters.stream().map(OllirUtils::getCode).collect(Collectors.joining(", "));
        codeString.append("(").append(parameterString).append(").");

        codeString.append(OllirUtils.getCode(symbolTable.getReturnType(methodName)));


        codeString.append(" {\n");
        codeString.append("}");
        return 0;
    }

    private Integer classDeclVisitor(JmmNode classDecl, Integer integer) {

        System.out.println("Generating Ollir code for class " + classDecl.get("name"));

        codeString.append("public ");
        codeString.append(symbolTable.getClassName());
        String superClass = symbolTable.getSuper();
        if (superClass != null) {
            codeString.append(" extends ").append(superClass);
        }

        codeString.append(" {\n");

        codeString.append(OllirUtils.createLocalFields(symbolTable));


        codeString.append(OllirUtils.createConstructor(symbolTable)).append("\n");

        for (JmmNode node : classDecl.getChildren()) {
            visit(node);
        }

        codeString.append("}");
        return 0;
    }

    public String getCode() {
        return this.codeString.toString();
    }

    private Integer initVisit(JmmNode root, Integer dummy) {

        System.out.println("Generating Ollir code");

        symbolTable.getImports().forEach(importString -> {
            codeString.append("import ");
            codeString.append(importString);
            codeString.append(";\n");
        });


        for (JmmNode node : root.getChildren()) {
            visit(node);
        }

        return 0;
    }

}
