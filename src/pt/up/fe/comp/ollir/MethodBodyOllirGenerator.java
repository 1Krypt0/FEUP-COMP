package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.analysis.Scope;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.regex.Matcher;

public class MethodBodyOllirGenerator extends AJmmVisitor<Object, String> {

    private final ProgramSymbolTable symbolTable;
    private final StringBuilder code;
    private String methodName;


    public MethodBodyOllirGenerator(ProgramSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = "";

        addVisit(AstNode.Method_Body, this::visitMethodBody);
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.If, this::visitIf);
        addVisit(AstNode.While_Loop, this::visitWhile);


        // used in assignments
        addVisit(AstNode.Assign, this::visitAssign);
        addVisit(AstNode.Array_Creation, this::visitArrayCreation);
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit(AstNode.Array_Access, this::visitArrayAccess);
        addVisit(AstNode.Bin_Op, this::visitBinOp);
        addVisit(AstNode.Integer_Literal, this::visitIntegerLiteral);
        addVisit(AstNode.I_D, this::visitId);
        // addVisit(AstNode.Method_Call, this::visitMethodCall);
        addVisit(AstNode.True, this::visitBool);
        addVisit(AstNode.False, this::visitBool);

        setDefaultVisit(this::visitDefault);
    }

    private String visitMethodBody(JmmNode jmmNode, Object integer) {
        this.methodName = jmmNode.getJmmParent().get("name");
        for (JmmNode child : jmmNode.getChildren()) visit(child, null);
        return "";
    }


    private String visitWhile(JmmNode jmmNode, Object integer) {
        JmmNode condition = jmmNode.getJmmChild(0).getJmmChild(0);
        JmmNode body = jmmNode.getJmmChild(1).getJmmChild(0);
        //DOUBT: what to do if while body is empty? Is it optimizable?

        String end_label = symbolTable.label("END_LOOP_LABEL", jmmNode);
        String body_label = symbolTable.label("BODY_LOOP_LABEL", jmmNode);
        String loop_label = symbolTable.label("LOOP_LABEL", jmmNode);

        // get the instruction for the condition operation
        ExprOllirGenerator exprVisitor =
                new ExprOllirGenerator(this.symbolTable, this.methodName);
        String instruction = exprVisitor.visit(condition, null);

        code.append(loop_label).append(":\n");
        code.append(exprVisitor.getCode());
        code.append(String.format("if (%s) goto %s;\n", instruction, body_label));
        code.append("goto ").append(end_label).append(";\n");

        // loop body
        code.append(body_label).append(":\n");
        for (JmmNode child : body.getChildren()) visit(child, integer);
        code.append("goto ").append(loop_label).append(";\n");

        code.append(end_label).append(":\n");
        return "";
    }


    private String visitIf(JmmNode jmmNode, Object o) {
        System.out.println("Visiting if");
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode if_code_block = jmmNode.getJmmChild(1).getJmmChild(0);
        JmmNode else_node = jmmNode.getJmmChild(2);

        boolean hasElse = else_node.getChildren().size() > 0;
        String end_label = symbolTable.label("END_IF_LABEL", jmmNode);
        String else_label = symbolTable.label("ELSE_IF_LABEL", jmmNode);

        // get the instruction for the condition operation
        ExprOllirGenerator exprVisitor =
                new ExprOllirGenerator(this.symbolTable, this.methodName);
        String instruction = exprVisitor.visit(condition, null);
        code.append(exprVisitor.getCode());

        // negate the condition if it is a simple condition (var < var)
        String negated_if_condition = negateCondition(instruction, jmmNode);
        code.append(String.format("if (%s) goto %s;\n",
                negated_if_condition, hasElse ? else_label : end_label));

        // visit the if body
        for (JmmNode if_statement : if_code_block.getChildren()) visit(if_statement, 0);

        if (hasElse) {
            // add extra else goto labels
            code.append(String.format("goto %s;\n", end_label));
            code.append(else_label).append(":\n");
            JmmNode else_code_block = else_node.getJmmChild(0);

            // visit the else body
            for (JmmNode else_statement : else_code_block.getChildren()) visit(else_statement, 0);
        }

        code.append(end_label).append(":\n");
        return "";
    }


    private String visitClassCreation(JmmNode classDecl, Object o) {
        System.out.println("Class Creation");
        String className = classDecl.get("name");

        if (o == null) return "new(" + className + ")." + className;

        String tempVar = symbolTable.tempVar() + "." + className;
        code.append(String.format("%s :=.%s new(%s).%s;\n",
                tempVar, className, className, className));

        return tempVar;
    }


    private String visitBool(JmmNode jmmNode, Object o) {
        return jmmNode.getKind().equals("True") ? "true.bool" : "false.bool";
    }

    private String visitIntegerLiteral(JmmNode jmmNode, Object o) {
        return jmmNode.get("value") + ".i32";
    }

    private String visitArrayCreation(JmmNode arrayCreationNode, Object o) {
        // This is the BinOp that has the length of the array
        JmmNode arrayAccessNode = arrayCreationNode.getChildren().get(0);
        arrayAccessNode = arrayAccessNode.getChildren().get(0);

        ExprOllirGenerator exprOllirGenerator = new ExprOllirGenerator(symbolTable, this.methodName);
        String arrayLength = exprOllirGenerator.visit(arrayAccessNode, arrayAccessNode);
        code.append(exprOllirGenerator.getCode());

        return "new(array, " + arrayLength + ").array.i32";
    }


    private String visitId(JmmNode jmmNode, Object o) {
        String variable = jmmNode.get("name");
        Scope varScope = symbolTable.getVariableScope(variable, methodName);
        String arrayAccess = "";

        // band-aid for array access, we need to get the assign node to see
        // if it has 2 children and get the array expression
        if (isArrayAccess(jmmNode)) {
            JmmNode arrayAccessOp = jmmNode.getJmmChild(0).getJmmChild(0);
            ExprOllirGenerator exprOllirGenerator = new ExprOllirGenerator(symbolTable, this.methodName);
            arrayAccess = "[" + exprOllirGenerator.visit(arrayAccessOp, 0) + "]";
            code.append(exprOllirGenerator.getCode());
        }

        if (varScope.equals(Scope.ARGUMENT)) {
            // Type type = symbolTable.getVariableType(variable, methodName);
            // int index = symbolTable.getArgPosition(variable, methodName);
            // return String.format("$%d.%s%s.%s", index, variable, arrayAccess, OllirUtils.getCode(type));
            return OllirUtils.getIdCode(variable, arrayAccess, this.symbolTable, this.methodName);
        } else if (varScope.equals(Scope.LOCAL)) {
            Type type = symbolTable.getVariableType(variable, methodName);
            return String.format("%s%s.%s", variable, arrayAccess, OllirUtils.getCode(type));
        } else if (varScope.equals(Scope.FIELD)) {
            Type type = symbolTable.getVariableType(variable, methodName);
            String tempVar = symbolTable.tempVar();

            code.append(String.format("%s :=.%s getfield(this, %s.%s).%s;\n",
                    tempVar, OllirUtils.getCode(type), variable, OllirUtils.getCode(type), OllirUtils.getCode(type)));
            return String.format("%s%s.%s", tempVar, arrayAccess, OllirUtils.getCode(type));
        }

        return jmmNode.get("name");
    }

    private String visitDotLinked(JmmNode jmmNode, Object o) {
        System.out.println("Dot Linked");

        DotLinkedOllirGenerator dotLinkedOllirGenerator =
                new DotLinkedOllirGenerator(symbolTable, this.methodName);

        // Enters the dot linked generation visitor,
        // implying the void return type (since it's not an assign)
        String instruction;
        if(!jmmNode.getJmmParent().getKind().equals("Assign")){
            instruction = dotLinkedOllirGenerator.visit(jmmNode, null);
            code.append(dotLinkedOllirGenerator.getCode());
            code.append(instruction);
            return "";
        }
        else {
            String variable = (String) o;
            // get the substring of the last . in the variable
            String varType = variable.substring(variable.lastIndexOf(".") + 1);
            instruction = dotLinkedOllirGenerator.visit(jmmNode, varType);
            code.append(dotLinkedOllirGenerator.getCode());
            return instruction;
        }
    }

    private String visitBinOp(JmmNode jmmNode, Object o) {
        ExprOllirGenerator exprOllirGenerator = new ExprOllirGenerator(symbolTable, this.methodName);
        String instruction = exprOllirGenerator.visit(jmmNode, null);
        code.append(exprOllirGenerator.getCode());
        return instruction;
    }

    private String visitAssign(JmmNode assignNode, Object integer) {
        System.out.println("Visiting assign");
        JmmNode assignedNode = assignNode.getJmmChild(0);
        String variableName = assignNode.get("name");
        String arrayAccess = "";


        // TODO: handle the field access
        if (isArrayAccess(assignNode)) {
            JmmNode arrayAccessOp = assignNode.getJmmChild(0).getJmmChild(0);
            ExprOllirGenerator exprOllirGenerator = new ExprOllirGenerator(symbolTable, this.methodName);
            arrayAccess = "[" + exprOllirGenerator.visit(arrayAccessOp, 0) + "]";
            code.append(exprOllirGenerator.getCode());
            assignedNode = assignNode.getJmmChild(1);
        }
        String variable = OllirUtils.getIdCode(variableName, arrayAccess, symbolTable, this.methodName);

        // left variables only included in array access ops
        String right_variable = visit(assignedNode, variable);
        assignmentGenerator(variable, assignedNode, right_variable);

        if (assignedNode.getKind().equals("ClassCreation"))
            code.append(String.format("invokespecial(%s, \"<init>\").V;\n", variable));

        return "";
    }


    private boolean isArrayAccess(JmmNode assignNode) {
        return !assignNode.getChildren().isEmpty()
                && assignNode.getJmmChild(0).getKind().equals("ArrayAccess");
    }


    /**
     * Generates the code for an assignment line
     *
     * @param variableCode the string code (in ollir) of the lhs variable
     * @param assignedNode the node that's about to be assigned
     * @param instruction  the final visit of the assigned node
     */
    private void assignmentGenerator(String variableCode, JmmNode assignedNode, String instruction) {
        String varName;

        if (variableCode.startsWith("$")){
            // remove the arg position from the variable code
            String argPos = variableCode.substring(0, variableCode.indexOf('.') + 1);
            variableCode = variableCode.replaceAll(Matcher.quoteReplacement(argPos), "");
        }

        // get the name of the variable, in case it's an array ollir code and a simple var
        if (variableCode.contains("[")) varName = variableCode.substring(0, variableCode.indexOf("["));
        else varName = variableCode.substring(0, variableCode.indexOf('.')); // from var.i32 to var

        Scope varScope = symbolTable.getVariableScope(varName, methodName);

        if (varScope.equals(Scope.LOCAL) || varScope.equals(Scope.ARGUMENT)) {
            // if it's a dot linked expression, force the assign to return the type of the left-hand side
            // else simply create the assignment as a normal assignment
            if (assignedNode.getKind().equals("DotLinked"))
                code.append(String.format("%s :=.%s %s;\n", variableCode,
                        OllirUtils.getCode(symbolTable.getVariableType(varName, methodName)), instruction));
            else
                code.append(String.format("%s %s %s;\n", variableCode,
                        OllirUtils.getBinOpAssignCode(assignedNode, this.symbolTable, methodName), instruction));

        } else if (varScope.equals(Scope.FIELD)) {
            // TODO: if left side is field then store instruction in temp var
            // see if it's necessary new(Class) vs 2 p.e. Literal/Id or complex instruction
            Type varType = symbolTable.getFieldType(varName);
            String tempVar = symbolTable.tempVar();
            code.append(String.format("%s :=.%s %s;\n", tempVar, OllirUtils.getCode(varType), instruction));
            code.append(String.format("putfield(this, %s.%s , %s).V;\n",
                    varName, OllirUtils.getCode(varType), tempVar));
        }
    }


    /**
     * Returns if the condition matches the var < var expression
     *
     * @param condition the condition of the if statement
     * @return true if the expression is simple
     */
    private boolean isSimpleCondition(String condition) {
        //TODO: correctly match the condition
        return condition.matches("^\\s*<.bool\\s*;\\s*$");
    }

    /**
     * If the condition is simple, negate the < to a >=
     * else negate the temporary variable with !
     *
     * @param instruction the instruction to negate
     * @param node        the node of the if statement
     * @return the negated instruction
     */
    private String negateCondition(String instruction, JmmNode node) {
        if (isSimpleCondition(instruction)) return instruction.replace("<", ">=");

        String temp_op_temp = symbolTable.tempVar() + ".bool";
        code.append(temp_op_temp).append(" :=.bool ").append(instruction).append(";\n");
        return "!.bool " + temp_op_temp;
    }


    private String visitArrayAccess(JmmNode jmmNode, Object integer) {
        // ArrayAccessOllirGenerator arrayAccessOllirGenerator = new ArrayAccessOllirGenerator(symbolTable, this.methodName);
        //code.append(arrayAccessOllirGenerator.getCode());
        // return arrayAccessOllirGenerator.visit(jmmNode, "left");
        return "";
    }


    public String getCode() {
        return code.toString();
    }

    private String visitDefault(JmmNode jmmNode, Object integer) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

}
