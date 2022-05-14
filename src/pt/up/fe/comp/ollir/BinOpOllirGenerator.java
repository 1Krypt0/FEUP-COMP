package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;




public class BinOpOllirGenerator extends AJmmVisitor<Object, String> {


    private final ProgramSymbolTable symbolTable;
    final StringBuilder code;
    private final String methodName;

    BinOpOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;
        addVisit(AstNode.Bin_Op, this::visitBinOp);
        addVisit(AstNode.Integer_Literal, this::visitIntegerLiteral);
        // addVisit(AstNode.Array_Access, this::visitArrayAccess);
        // addVisit(AstNode.Method_Call, this::visitMethodCall);
        addVisit(AstNode.False, this::visitBool);
        addVisit(AstNode.True, this::visitBool);
        // TODO: we need to check if the variable is in the same method or is a field (getField)
        // addVisit(AstNode.This, this::visitThis);
        addVisit("ID", this::visitId);

        // addVisit(AstNode.This, this::visitThis);
    }

    private String visitId(JmmNode idNode, Object o) {
        // TODO: Code for putField

       //TODO: Correct name of variable in case it's an argument


        Integer variableIndex = symbolTable.getArgumentPosition(methodName, idNode.get("name"));

        if(variableIndex != null) {
            //TODO: include this behavior in the OllirUtils

            Type type = symbolTable.getArgumentType(methodName, idNode.get("name"));
            return String.format("$%d", variableIndex) + "." + OllirUtils.getIdCode(idNode.get("name"), symbolTable);
        }

        return OllirUtils.getIdCode(idNode.get("name"), symbolTable);
    }

    private String visitBool(JmmNode booleanNode, Object o) {
        return OllirUtils.getBoolCode(booleanNode);
    }

    private String visitIntegerLiteral(JmmNode node, Object needsVariable) {
        return OllirUtils.getIntegerLiteralCode(node);
    }


    public String visitBinOp (JmmNode binOpExpression, Object needsVariable) {

        // see what type of operation we are doing

        String operation = binOpExpression.get("op");
        List<String> arithmeticOperations = OllirUtils.getArithmeticOperations();

        if(arithmeticOperations.contains(operation)) {
            return visitArithmeticOp(binOpExpression, needsVariable);
        }
        else if(operation.equals("and")) {
            return visitAnd(binOpExpression, needsVariable);
        }
        return "";
    }

    private String visitAnd(JmmNode andExpression, Object needsVariable) {

        // TODO: in the future this method will evaluate the expression and store the result
        // Therefore it's defined separately from the visitArithmeticOpAuxliar method

        System.out.println("Visiting And");

        JmmNode left = andExpression.getJmmChild(0);
        JmmNode right = andExpression.getJmmChild(1);

        String leftResult = visit(left, !left.getKind().equals("True") && !left.getKind().equals("False"));
        String rightResult = visit(right,!right.getKind().equals("True") && !right.getKind().equals("False"));
        String instruction = leftResult + " &&.bool " + rightResult;

        if(needsVariable != null) {
            // String tempVar = "t" + symbolTable.getTempVarCount(); TODO
            String tempVar = "t" + "_" + andExpression.get("line") + "_" + andExpression.get("col") + ".bool";
            this.code.append(tempVar).append(" :=.bool ").append(instruction).append(";").append("\n");
            return tempVar;
        }
        return instruction;

    }


    public String visitArithmeticOp(JmmNode arithmeticOp, Object needsVariable) {

        System.out.println("Visiting Arithmetic Op");

        String operation = arithmeticOp.get("op");
        String operationCode = OllirUtils.getArithmeticOperationCode(operation);


        JmmNode left = arithmeticOp.getJmmChild(0);
        JmmNode right = arithmeticOp.getJmmChild(1);

        String leftResult = visit(left, !left.getKind().equals("IntegerLiteral"));
        String rightResult = visit(right, !right.getKind().equals("IntegerLiteral"));
        String instruction = leftResult + " " + operationCode + " " + rightResult;

        if(needsVariable != null) {
            // String tempVar = "t" + symbolTable.getTempVarCount(); TODO
            String tempVar = "t" + "_" + arithmeticOp.get("line") + "_" + arithmeticOp.get("col") + ".i32";
            this.code.append(tempVar).append(" :=.i32 ").append(instruction).append(";").append("\n");
            return tempVar;
        }
        return instruction;
    }

    public String getCode() {
        return this.code.toString();
    }


}
