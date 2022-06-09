package pt.up.fe.comp.ollir;

import AST.AstNode;
import freemarker.core.ast.Dot;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class MethodBodyOllirGenerator extends AJmmVisitor<Object, String> {

    private final SymbolTable symbolTable;
    private final StringBuilder code;
    private String methodName;


    public MethodBodyOllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = "";

        addVisit(AstNode.Method_Body, this::visitMethodBody);
        addVisit(AstNode.Assign, this::visitAssign);
        addVisit(AstNode.Array_Creation, this::visitArrayCreation);
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        // addVisit(AstNode.Array_Access, this::visitArrayAccess);



        addVisit(AstNode.If, this::visitIf);

        /*
        addVisit(AstNode.While, this::visitWhile);
        */

        setDefaultVisit(this::visitDefault);
    }



    private String visitIf(JmmNode jmmNode, Object o) {
        System.out.println("Visiting if");
        JmmNode condition = jmmNode.getJmmChild(0);
        JmmNode if_block = jmmNode.getJmmChild(1).getJmmChild(0);
        JmmNode else_node = jmmNode.getJmmChild(2);

        // condition
        StatementOllirGenerator conditionGenerator =
                new StatementOllirGenerator((ProgramSymbolTable) this.symbolTable, methodName);
        String instruction = conditionGenerator.visit(condition);
        this.code.append(conditionGenerator.getCode());

        this.code.append("if( ").append(instruction).append(" ) goto if_body;\n");
        if(!else_node.getChildren().isEmpty()){
            JmmNode else_block = else_node.getJmmChild(0);
            for (JmmNode else_statement : else_block.getChildren())
            {
                visit(else_statement);
            }
        }
        this.code.append("goto endif;\n");
        this.code.append("if_body:\n");
        for (JmmNode if_statement : if_block.getChildren()) visit(if_statement);
        this.code.append("endif:\n");



        // if (condition)  goTo label_if_body
        // [else_body]
        // goto label_end;
        // label_if_body:
        // [if_body]
        // label_end


        // if (condition) goto then
        // goto end
        // then


        return "";
    }


    private String visitDotLinked(JmmNode jmmNode, Object o) {

        System.out.println("Dot Linked");

        DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
        String instruction = dotLinkedOllirGenerator.visit(jmmNode, ".V"); // could be not null if it's an assign

        code.append(dotLinkedOllirGenerator.getCode());

        code.append(instruction);
        return "";
    }

    private String visitClassCreation(JmmNode classDecl, Object o) {
        String className = classDecl.get("name");

        if(o != null) {
            String tempVar = "temp_" + classDecl.get("col") + "_" + classDecl.get("line") + "." + className;
            code.append(tempVar).append(" :=.").append(className).append(" new(").append(className).append(").").append(className).append(";\n");
            return tempVar;
        }

        return "new(" + className + ")." + className;
    }

    private String visitArrayCreation(JmmNode arrayCreationNode, Object o) {

        JmmNode arrayAccessNode = arrayCreationNode.getChildren().get(0); // This is the BinOp that has the length of the array
        JmmNode arrayAccessOperationNode = arrayAccessNode.getChildren().get(0); // This is the array

        arrayAccessNode = arrayAccessNode.getChildren().get(0);
        //TODO: BandAid Should a class separation : right side of Assigment Exprs -> BinOp or other and visit in AssingmentVisitor

        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
        String arrayLength = binOpOllirGenerator.visit(arrayAccessNode, arrayAccessNode);
        code.append(binOpOllirGenerator.getCode());

        // if it returned an expression and not

        return "new(array, " + arrayLength + ").array.i32";
    }


    private String visitMethodBody(JmmNode jmmNode, Object integer) {

        this.methodName = jmmNode.getJmmParent().get("name");

        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, null); // TODO: should we use code.append here ?
        }

        return "";
    }

    private String visitArrayAccess(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitAssign(JmmNode assignNode, Object integer) {
        System.out.println("Visiting assign");

        String variableName = assignNode.get("name");
        Symbol variableSymbol = ((ProgramSymbolTable) symbolTable).getLocalVariable(this.methodName, variableName);

        String variable = OllirUtils.getIdCode(variableName, (ProgramSymbolTable) symbolTable, this.methodName);

        StatementOllirGenerator statementOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);

        JmmNode assignedNode = assignNode.getJmmChild(0);

        //TODO: ArrayAccess for assignNode


        //if it's a putfield visit binOp and MethodCall and return temp var and not instruction
        String varScope = OllirUtils.getVariableScope(variableName, methodName, (ProgramSymbolTable) symbolTable);

        if(assignNode.getChildren().size() == 2 && assignNode.getJmmChild(0).getKind().equals("ArrayAccess")) {
            ArrayAccessOllirGenerator arrayAccessOllirGenerator = new ArrayAccessOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
            variable = arrayAccessOllirGenerator.visit(assignNode, "left");
            code.append(arrayAccessOllirGenerator.getCode());
            assignedNode = assignNode.getJmmChild(1);
        }



        if (assignedNode.getKind().equals("ArrayCreation")) {
            if(!varScope.equals("field")){
                String instruction = visit(assignedNode, null);
                assignmentGenerator(variable, assignedNode, instruction);
            } else {
                String tempVar = "temp_" + assignedNode.get("col") + "_" + assignedNode.get("line") + ".array.i32";
                String instruction = visit(assignedNode, null);
                code.append(tempVar).append(" :=.array.i32 ").append(instruction).append(";\n");
                assignmentGenerator(variable, assignedNode, tempVar);
            }

        } else if (assignedNode.getKind().equals("ClassCreation")) {
            String instruction = visit(assignedNode, (varScope.equals("field") ? "field" : null));
            assignmentGenerator(variable, assignedNode, instruction);
            code.append("invokespecial(").append(variable).append(", \"<init>\").V;\n");
        }

        if (assignedNode.getKind().equals("BinOp") || assignedNode.getKind().equals("IntegerLiteral")) {
            String instruction = statementOllirGenerator.visit(assignedNode, (varScope.equals("field")) ? "field" : null);
            System.out.println("Instruction: " + instruction);
            code.append(statementOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }

        if (assignedNode.getKind().equals("ID")) {

            if(assignNode.getChildren().size() == 2 && assignNode.getJmmChild(1).getKind().equals("ArrayAccess")){
                ArrayAccessOllirGenerator arrayAccessOllirGenerator = new ArrayAccessOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
                String instruction = arrayAccessOllirGenerator.visit(assignNode, "right");
                code.append(arrayAccessOllirGenerator.getCode());
                code.append(variableName).append(".i32 :=.i32 ").append(instruction).append(";\n"); //TODO: place in assigmentGenerator


            } else {
                // BinaryOp takes care of field aswell
                String instruction = statementOllirGenerator.visit(assignedNode, null);
                System.out.println("Instruction: " + instruction);
                code.append(statementOllirGenerator.getCode());
                assignmentGenerator(variable, assignedNode, instruction);
            }
        }

        if (assignedNode.getKind().equals("DotLinked")) {
            DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
            String instruction = dotLinkedOllirGenerator.visit(assignedNode, null);
            code.append(dotLinkedOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }



        return "";
    }

    private void assignmentGenerator(String variableCode, JmmNode assignedNode, String instruction) {


        String varName;
        if(variableCode.contains("[")){
            varName = variableCode.substring(0, variableCode.indexOf("["));
        }
        else {
            varName = variableCode.substring(0, variableCode.indexOf('.'));
        }

        if (((ProgramSymbolTable) symbolTable).hasLocalVariable(this.methodName, varName)
                || ((ProgramSymbolTable) symbolTable).getArgumentPosition(this.methodName, varName) != -1) {

            if(assignedNode.getKind().equals("DotLinked")) {
                Type type = ((ProgramSymbolTable) symbolTable).getVariableType(varName);
                code.append(variableCode).append(" :=.").append(OllirUtils.getCode(type)).append(" ").append(instruction);
            }
            else {
                code.append(variableCode).append(" ").
                        append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                        append(" ").append(instruction).append(";").append("\n");
            }
        }
        else{
            // fieldName is the substring of variableCode until the first '.'
            if(((ProgramSymbolTable) symbolTable).hasField(varName)){
                Type varType = ((ProgramSymbolTable) symbolTable).getFieldType(varName);
                code.append("putfield(this, ").append(varName).append(".").append(OllirUtils.getCode(varType)).append(", ").append(instruction).append(").V;\n");
            }
        }

    }

    public String getCode() {
        return code.toString();
    }

    private String visitDefault(JmmNode jmmNode, Object integer) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

    private String visitWhile(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitIf(JmmNode jmmNode, Integer integer) {
        return "";
    }
}
