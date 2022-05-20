package pt.up.fe.comp.ollir;

import AST.AstNode;
import freemarker.core.ast.Dot;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

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


        /*
        addVisit(AstNode.If, this::visitIf);
        addVisit(AstNode.While, this::visitWhile);
        */

        setDefaultVisit(this::visitDefault);
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
        return "new(" + className + ")." + className;
    }

    private String visitArrayCreation(JmmNode arrayCreationNode, Object o) {

        JmmNode arrayAccessNode = arrayCreationNode.getChildren().get(0); // This is the BinOp that has the length of the array
        JmmNode arrayAccessOperationNode = arrayAccessNode.getChildren().get(0); // This is the array

        arrayAccessNode =  arrayAccessNode.getChildren().get(0);
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

        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable,this.methodName);

        JmmNode assignedNode = assignNode.getJmmChild(0);

        if(assignedNode.getKind().equals("ArrayCreation")) {
            String instruction = visit(assignedNode, null);
            assignmentGenerator(variable, assignedNode, instruction);
        }
        else if(assignedNode.getKind().equals("ClassCreation")) {
            String instruction = visit(assignedNode, null);
            assignmentGenerator(variable, assignedNode, instruction);
            String className =  assignedNode.get("name");
            code.append("invokespecial(").append(variable).append(", \"<init>\").V;\n");
        }

        if(assignedNode.getKind().equals("BinOp") || assignedNode.getKind().equals("IntegerLiteral")) {
            String instruction = binOpOllirGenerator.visit(assignedNode, null);
            System.out.println("Instruction: " + instruction);
            code.append(binOpOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }


        if(assignedNode.getKind().equals("ID")){
            String instruction = binOpOllirGenerator.visit(assignedNode, null);
            System.out.println("Instruction: " + instruction);
            code.append(binOpOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }

        /*if(assignedNode.getKind().equals("ArrayAccess")) {
            ArrayAccessOllirGenerator arrayAccessOllirGenerator = new ArrayAccessOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
            String instruction = arrayAccessOllirGenerator.visit(assignedNode, null);
            code.append(arrayAccessOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }*/

        /*if(assignedNode.getKind().equals("DotLinked")) {
            DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
            String instruction = dotLinkedOllirGenerator.visit(assignedNode, null);
            code.append(dotLinkedOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }*/

        return "";
    }

    private void assignmentGenerator(String variableCode, JmmNode assignedNode, String instruction) {

        if(assignedNode.getKind().equals("BinOp") || assignedNode.getKind().equals("IntegerLiteral") || assignedNode.getKind().equals("True") || assignedNode.getKind().equals("False")) {
            code.append(variableCode).append(" ").
                    append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                    append(" ").append(instruction).append(";").append("\n");
        }
        else if(assignedNode.getKind().equals("ID")) {
            String scope = OllirUtils.getVariableScope(assignedNode.get("name"), this.methodName,(ProgramSymbolTable) this.symbolTable);
            if(scope.equals("local")) {
                code.append(variableCode).append(" ").
                        append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                        append(" ").append(instruction).append(";").append("\n");
            }
            else if(scope.equals("argument")) {
                code.append(variableCode).append(" ").
                        append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                        append(" ").append(instruction).append(";").append("\n");
            }
            else if(scope.equals("import")) {

            }
            // else
            code.append(variableCode).append(" ").
                    append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                    append(" ").append(instruction).append(";").append("\n");

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
