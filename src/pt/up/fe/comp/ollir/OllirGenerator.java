package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<String, Integer> {

    private final StringBuilder codeString;
    private final ProgramSymbolTable symbolTable;

    public OllirGenerator(ProgramSymbolTable symbolTable) {
        this.codeString = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit(AstNode.Init, this::initVisit);
        addVisit(AstNode.Class_Decl, this::classDeclVisitor);
        addVisit(AstNode.Method_Declaration, this::methodDeclVisitor);
        addVisit(AstNode.Main, this::methodDeclVisitor);
        addVisit(AstNode.Return_Statement, this::visitReturn);
        addVisit(AstNode.Method_Body, this::methodBodyVisitor);
        setDefaultVisit(this::defaultVisit);
    }

    private Integer methodDeclVisitor(JmmNode methodDecl, String integer) {

        //TODO:
        System.out.println("Optimizing method " + methodDecl.get("name"));

        String methodName = methodDecl.get("name");
        codeString.append(".method public ");
        if(methodDecl.getKind().equals("Main")) {
            codeString.append("static ");
        }
        codeString.append(methodName);


        List<Symbol> parameters =  symbolTable.getParameters(methodName);
        String parameterString = parameters.stream().map(OllirUtils::getCode).collect(Collectors.joining(", "));
        codeString.append("(").append(parameterString).append(").");

        codeString.append(OllirUtils.getCode(symbolTable.getReturnType(methodName)));


        codeString.append(" {\n");

        for (JmmNode node : methodDecl.getChildren()) {
            visit(node);
        }

        if(methodDecl.getKind().equals("Main")) codeString.append("ret.V;\n");
        codeString.append("}\n");
        return 0;
    }


    private Integer visitReturn(JmmNode returnNode, String integer) {
        System.out.println("Visit return");

        ExprOllirGenerator statementOllirGenerator =
                new ExprOllirGenerator(this.symbolTable,  returnNode.getJmmParent().get("name"));

        String instruction =  statementOllirGenerator.visit(returnNode.getJmmChild(0), 0);
        codeString.append(statementOllirGenerator.getCode());
        // Two options: get return from symbol table or from return instruction
        String retType = instruction.split("\\.")[1];
        codeString.append("ret.").append(retType).append(" ").append(instruction).append(";").append("\n");
        return 0;
    }

    private Integer methodBodyVisitor(JmmNode methodBody, String integer) {

        System.out.println("Visit method body for method " + methodBody.getJmmParent().get("name"));

        MethodBodyOllirGenerator methodBodyVisitor = new MethodBodyOllirGenerator(symbolTable);
        methodBodyVisitor.visit(methodBody);
        codeString.append(methodBodyVisitor.getCode());
        return  0;
    }


    private Integer classDeclVisitor(JmmNode classDecl, String integer) {

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

    private Integer initVisit(JmmNode root, String dummy) {
        System.out.println("Generating Ollir code");

        symbolTable.getImports().forEach(importString ->
                codeString.append(String.format("import %s ;\n", importString)));

        for (JmmNode node : root.getChildren()) visit(node);
        return 0;
    }


    private Integer defaultVisit(JmmNode node, String dummy) {
        System.out.println("Unhandled node: " + node.getKind());
        return 0;
    }

}
