package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ArrayAccessOllirGenerator extends AJmmVisitor<Object, String> {

    private StringBuilder code;
    private SymbolTable symbolTable;
    private String methodName;


    public ArrayAccessOllirGenerator(SymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;


        addVisit(AstNode.Array_Access, this::visitArrayAccess);
        setDefaultVisit(this::visitDefault);
    }

    private String visitDefault(JmmNode jmmNode, Object o) {
        return "";
    }

    private String visitArrayAccess(JmmNode arrayAccessNode, Object o) {
        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
        JmmNode accessExpression = arrayAccessNode.getJmmChild(0);
        String instruction =  binOpOllirGenerator.visit(accessExpression, null);

        code.append(binOpOllirGenerator.getCode());

        return instruction;
    }

    public String getCode() {
        return code.toString();
    }


}
