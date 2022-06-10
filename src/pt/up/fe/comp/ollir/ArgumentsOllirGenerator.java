package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ArgumentsOllirGenerator extends AJmmVisitor<Integer, String> {

    private final StringBuilder code;
    private final ProgramSymbolTable symbolTable;
    private final String methodName;


    public ArgumentsOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {

        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.methodName = methodName;

        addVisit(AstNode.Method_Call, this::visitMethodCall);
        addVisit(AstNode.Arg , this::visitArg);

        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, Integer integer) {
        return "";
    }

    private String visitArg(JmmNode argNode, Integer integer) {
        ExprOllirGenerator statementOllirGenerator =
                new ExprOllirGenerator(this.symbolTable, this.methodName);

        String instruction = statementOllirGenerator.visit(argNode.getJmmChild(0), 0);

        code.append(statementOllirGenerator.getCode());

        return ", "+  instruction;
    }

    private String visitMethodCall(JmmNode jmmNode, Integer integer) {
        StringBuilder argString = new StringBuilder();

        for (JmmNode child : jmmNode.getChildren()) argString.append(visit(child, integer));

        return argString.toString();
    }


    public String getCode() {
        return code.toString();
    }
}
