package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Stack;

public class MethodBodyOllirGenerator extends AJmmVisitor<Object, String> {

    private final SymbolTable symbolTable;
    private final StringBuilder code;
    private String methodName;


    public MethodBodyOllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = "";
        // ASSIGN
        // IF
        //WHILE

        // IGNORE
        /*
        addVisit(AstNode.Var_Decl, this::visitVarDecl);
        addVisit(AstNode.Type, this::visitType);*/


        addVisit(AstNode.Method_Body, this::visitMethodBody);
        addVisit(AstNode.Assign, this::visitAssign);

        // addVisit(AstNode.Return, this::visitReturn);
        // addVisit(AstNode.Array_Access, this::visitArrayAccess);

        /*
        addVisit(AstNode.If, this::visitIf);
        addVisit(AstNode.While, this::visitWhile);
        addVisit(AstNode.Method_Call, this::visitMethodCall);
        */

        setDefaultVisit(this::visitDefault);

    }


    private String visitMethodBody(JmmNode jmmNode, Object integer) {

        this.methodName = jmmNode.getJmmParent().get("name");
        // Should we start the parse here?

        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, null);
        }

        return "";
    }

    private String visitDefault(JmmNode jmmNode, Object integer) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

    private String visitArrayAccess(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitReturn(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitMethodCall(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitWhile(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitIf(JmmNode jmmNode, Integer integer) {
        return "";
    }

    private String visitAssign(JmmNode assignNode, Object integer) {
        System.out.println("Visiting assign");

        // TODO: we need to check if the variable is in the same method or is a field

        // a could be an array access
        // a field (getFields method)
        // a dot expression??

        // a =
        // AndExpression / BinOP -> Another visitor check
        // methodCall
        // arrayAccess
        // ArrayCreation
        // new Object ??

        // TODO: Could be an array access
        // TODO: Could be a this dot expression
        // TODO: Could be a literal bool or int


        String variableName = assignNode.get("name");
        Symbol variableSymbol = ((ProgramSymbolTable) symbolTable).getLocalVariable(this.methodName, variableName);
        String variableCode = OllirUtils.getCode(variableSymbol);
        System.out.println("Variable code: " + variableCode);
        BinOpOllirGenerator binOpOllirGenerator = new BinOpOllirGenerator((ProgramSymbolTable) symbolTable);

        JmmNode binOp = assignNode.getJmmChild(0);

        // BinOp takes care of a BinOp, a literal, an ID (unless it's a this.)
        // Should it handle a method call
        // Should it handle an array access
        // Should it handle an array creation
        String instruction = binOpOllirGenerator.visit(binOp, null);
        System.out.println("Instruction: " + instruction);
        code.append(binOpOllirGenerator.getCode());
        code.append(variableCode).append(" ").
                append(OllirUtils.getBinOpAssignCode(binOp, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                    append(" ").append(instruction).append(";").append("\n");

        return "";
    }

    private String visitVarDecl(JmmNode jmmNode, Integer integer) {
        return ""; //TODO: Ignore and place in default visitor
    }

    private String visitType(JmmNode jmmNode, Integer integer) {
        return "";//TODO: Ignore and place in default visitor
    }

    public String getCode() {
        return code.toString();
    }
}
