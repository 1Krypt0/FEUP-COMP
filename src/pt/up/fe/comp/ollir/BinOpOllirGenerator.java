package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class BinOpOllirGenerator extends AJmmVisitor<Object, String> {


    private final ProgramSymbolTable symbolTable;
    final StringBuilder code;

    BinOpOllirGenerator(ProgramSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        addVisit(AstNode.Bin_Op, this::visitAddExpr);
        addVisit(AstNode.Integer_Literal, this::visitIntegerLiteral);
    }

    private String visitIntegerLiteral(JmmNode node, Object needsVariable) {
        return node.get("value") + ".i32";
    }

    public String visitAddExpr(JmmNode addExpr, Object needsVariable) {

        System.out.println("Visiting BinOp");

        JmmNode left = addExpr.getJmmChild(0);
        JmmNode right = addExpr.getJmmChild(1);

        String leftResult = visit(left, !left.getKind().equals("IntegerLiteral"));
        String rightResult = visit(right, !right.getKind().equals("IntegerLiteral"));
        String instruction = leftResult + " +.i32 " + rightResult;

        if(needsVariable != null) {
            // String tempVar = "t" + symbolTable.getTempVarCount(); TODO
            String tempVar = "t" + "_" + addExpr.get("line") + "_" + addExpr.get("col") + ".i32";
            this.code.append(tempVar).append(" :=.i32 ").append(instruction).append(";").append("\n");
            return tempVar;
        }
        return instruction;
    }

    public String getCode() {
        return this.code.toString();
    }


}
