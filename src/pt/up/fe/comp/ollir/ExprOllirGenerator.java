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




   /* addVisit(AstNode.Array_Creation, this::visitArrayCreation);
    addVisit(AstNode.Class_Creation, this::visitClassCreation);
    addVisit(AstNode.Array_Access, this::visitArrayAccess);
    addVisit(AstNode.Bin_Op, this::visitBinOp);
    addVisit(AstNode.Integer_Literal, this::visitIntegerLiteral);
    addVisit(AstNode.I_D, this::visitId);
    // addVisit(AstNode.Method_Call, this::visitMethodCall);
    addVisit(AstNode.True, this::visitBool);
    addVisit(AstNode.False, this::visitBool);*/


    ExprOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;


        addVisit(AstNode.Bin_Op, this::visitBinOp);
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit(AstNode.Integer_Literal, this::visitIntegerLiteral);
        addVisit(AstNode.I_D, this::visitId); // TODO
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.False, this::visitBool);
        addVisit(AstNode.True, this::visitBool);

        // addVisit(AstNode.Array_Access, this::visitArrayAccess); //TODO
        // TODO: we need to check if the variable is in the same method or is a field (getField)
        // addVisit(AstNode.This, this::visitThis);
        setDefaultVisit(this::defaultVisitor);
        // addVisit(AstNode.Method_Call, this::visitMethodCall); // TODO
    }

    // private String visitMethodCall(JmmNode jmmNode, Object o) {return "";}
    // private String visitArrayAccess(JmmNode jmmNode, Object o) {return "";}


    private String visitClassCreation(JmmNode classDecl, Object o) {
        System.out.println("Class Creation");
        String className = classDecl.get("name");

        String tempVar = symbolTable.tempVar() + "." + className;
        code.append(String.format("%s :=.%s new(%s).%s;\n",
                tempVar, className, className, className));
        code.append(String.format("invokespecial(%s, \"<init>\").V;\n", tempVar));
        return tempVar;
    }


    private String visitDotLinked(JmmNode jmmNode, Object o) {
        DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator(this.symbolTable, this.methodName);

        // TODO: infer type
        String instruction = dotLinkedOllirGenerator.visit(jmmNode, ".i32");
        code.append(dotLinkedOllirGenerator.getCode());

        return instruction;
    }


    private String visitId(JmmNode idNode, Object needsVariable) {
        // TODO: needsVariable
        Scope scope = this.symbolTable.getVariableScope(idNode.get("name"), this.methodName);
        String arrayAccess = "";

        // if it's an array access, pass it to a temp first
        if (!idNode.getChildren().isEmpty()) { //TODO: this could be an ArrayAccess vistor
            JmmNode arrayAccessOp = idNode.getJmmChild(0).getJmmChild(0);
            ExprOllirGenerator exprOllirGenerator = new ExprOllirGenerator(symbolTable, this.methodName);
            arrayAccess = "[" + exprOllirGenerator.visit(arrayAccessOp, 0) + "]";
            code.append(exprOllirGenerator.getCode());
        }


        if (scope.equals(Scope.LOCAL) || scope.equals(Scope.ARGUMENT)) {
            Type varType = symbolTable.getVariableType(idNode.get("name"), this.methodName);
            String tempVar = this.symbolTable.tempVar() + "." + (arrayAccess.isEmpty() ? OllirUtils.getCode(varType) : "i32");
            String varCode = OllirUtils.getIdCode(idNode.get("name"), arrayAccess, this.symbolTable, this.methodName);
            if(needsVariable == null) return varCode;
            code.append(String.format("%s :=.%s %s;\n", tempVar,
                    (arrayAccess.isEmpty() ? OllirUtils.getCode(varType) : "i32"), varCode));
            return tempVar;
        } else if (symbolTable.hasField(idNode.get("name"))) {
            Type type = symbolTable.getFieldType(idNode.get("name"));
            String temp_var = symbolTable.tempVar() + "." + OllirUtils.getCode(type);

            String type_code = OllirUtils.getCode(type);
            code.append(String.format("%s :=.%s getfield(this, %s.%s).%s;\n",
                    temp_var, type_code, idNode.get("name"), type_code, type_code));
            return temp_var;
        }
        return "";
    }

    private String visitBool(JmmNode booleanNode, Object o) {
        return OllirUtils.getBoolCode(booleanNode);
    }

    private String visitIntegerLiteral(JmmNode node, Object needsVariable) {
        if (needsVariable != null) {
            String temp_var = symbolTable.tempVar() + ".i32";
            code.append(temp_var).append(" :=.i32 ").append(node.get("value")).append(".i32;\n");
            return temp_var;
        }
        return OllirUtils.getIntegerLiteralCode(node);
    }


    public String visitBinOp(JmmNode binOpExpression, Object needsVariable) {
        // see what type of operation we are doing
        String operation = binOpExpression.get("op");

        if (OllirUtils.getArithmeticOperations().contains(operation)) return visitArithmeticOp(binOpExpression, needsVariable);
        else if (operation.equals("and")) return visitAnd(binOpExpression, needsVariable);
        return "";
    }

    private String visitAnd(JmmNode andExpression, Object needsVariable) {
        // Therefore it's defined separately from the visitArithmeticOpAuxliar method

        System.out.println("Visiting And");

        JmmNode left = andExpression.getJmmChild(0);
        JmmNode right = andExpression.getJmmChild(1);

        // TODO: force variable
        String leftResult = visit(left, !left.getKind().equals("True") && !left.getKind().equals("False"));
        String rightResult = visit(right, !right.getKind().equals("True") && !right.getKind().equals("False"));
        String instruction = leftResult + " &&.bool " + rightResult;

        if (needsVariable != null) {
            // String tempVar = "t" + symbolTable.getTempVarCount(); TODO
            String tempVar = symbolTable.tempVar() + ".bool";
            this.code.append(String.format("%s.bool :=.bool %s;\n", symbolTable.tempVar(), instruction));
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

        // TODO: force variable
        String leftResult = visit(left, !left.getKind().equals("IntegerLiteral"));
        String rightResult = visit(right, !right.getKind().equals("IntegerLiteral"));
        String instruction = leftResult + " " + operationCode + " " + rightResult;

        if (needsVariable != null) {
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
    private String defaultVisitor(JmmNode jmmNode, Object o) {
        System.out.println("Not implemented " + jmmNode.getKind());
        return "";
    }


}
