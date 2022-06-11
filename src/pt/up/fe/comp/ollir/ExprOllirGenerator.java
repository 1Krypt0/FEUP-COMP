package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.analysis.Scope;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;




public class ExprOllirGenerator extends AJmmVisitor<Object, String> {


    private final ProgramSymbolTable symbolTable;
    final StringBuilder code;
    private final String methodName;

    ExprOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
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
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit("DotLinked", this::visitDotLinked);

        // addVisit(AstNode.This, this::visitThis);
    }

    private String visitClassCreation(JmmNode classNode, Object o) {
        String className = classNode.get("name");
        String caller_var = symbolTable.tempVar() + "." + className;
        code.append(caller_var).append(".").append(className).append(" :=.").append(className).append(" new(").append(className).append(").").append(className).append(";\n");
        code.append("invokespecial(").append(caller_var).append(", \"<init>\").V;\n");

        return caller_var;
    }

    private String visitDotLinked(JmmNode jmmNode, Object o) {

        DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator(this.symbolTable, this.methodName);
        String instruction = dotLinkedOllirGenerator.visit(jmmNode, ".V");
        code.append(dotLinkedOllirGenerator.getCode());

        return instruction;
    }

    private String visitId(JmmNode idNode, Object o) {
        // TODO: Code for getField

        Scope scope = this.symbolTable.getVariableScope(idNode.get("name"), this.methodName);

        if(scope.equals(Scope.LOCAL) || scope.equals(Scope.ARGUMENT)) {
            return OllirUtils.getIdCode(idNode.get("name"), "", this.symbolTable, this.methodName);
        }else if(symbolTable.hasField(idNode.get("name"))) {
            Type type = symbolTable.getFieldType(idNode.get("name"));
            String temp_var = symbolTable.tempVar() + "." + OllirUtils.getCode(type);

            String type_code = OllirUtils.getCode(type);

            code.append(String.format("%s :=.%s getfield(this, %s.%s).%s;\n",
                    temp_var, type_code, idNode.get("name") , type_code, type_code));

            return temp_var;
        }
        return "";
    }

    private String visitBool(JmmNode booleanNode, Object o) {
        return OllirUtils.getBoolCode(booleanNode);
    }

    private String visitIntegerLiteral(JmmNode node, Object needsVariable) {

        if(needsVariable != null) {
            String temp_var = symbolTable.tempVar() + ".i32";
            code.append(temp_var).append(" :=.i32 ").append(node.get("value")).append(".i32;\n");
            return temp_var;
        }
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
            String tempVar = symbolTable.tempVar() + ".bool";
            this.code.append(tempVar).append(":=.bool ").append(instruction).append(";").append("\n");
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
            String tempType = (operation.equals(("lt")) ? ".bool " : ".i32 ");
            String tempVar = symbolTable.tempVar() + tempType;
            this.code.append(tempVar).append(" :=").append(tempType).append(instruction).append(";").append("\n");
            return tempVar;
        }
        return instruction;
    }

    public String getCode() {
        return this.code.toString();
    }


}
