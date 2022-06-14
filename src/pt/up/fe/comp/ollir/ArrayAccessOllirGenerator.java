package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.analysis.Scope;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ArrayAccessOllirGenerator extends AJmmVisitor<String, String> {

    private final StringBuilder code;
    private final ProgramSymbolTable symbolTable;
    private final String methodName;


    public ArrayAccessOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;

        addVisit(AstNode.Assign, this::visitArrayAccessAssign);
        setDefaultVisit(this::visitDefault);
    }


    private String visitArrayAccessAssign(JmmNode arrayAssignAccess, String assign_info) {
        JmmNode arrayAccessNode = arrayAssignAccess.getChildren().stream().
                filter(node -> node.getKind().equals("ArrayAccess")).findFirst().get();

        String arrayName = arrayAssignAccess.get("name"); // name of the array
        JmmNode accessExpression = arrayAccessNode.getJmmChild(0); // [ inner expression ]

        String instruction = evaluateExpression(accessExpression);

        // construct the assignment instruction according to scope
        switch (symbolTable.getVariableScope(arrayName, this.methodName)) {
            case LOCAL:
                return arrayName + "[" + instruction + "].i32";
            case ARGUMENT:
                return "$" + this.symbolTable.getArgPosition(arrayName, this.methodName) +
                        "." + arrayName + "[" + instruction + "].i32";
            case FIELD:
                return fieldAccess(arrayAccessNode, arrayName, instruction, assign_info);
            default:
                return "";
        }
    }

    /**
     * Evaluates the inner expression of the [] and returns its instruction (appending aux code)
     *
     * @param accessExpression - the node of the expression inside the [ ]
     * @return the instruction to access the array
     */
    private String evaluateExpression(JmmNode accessExpression) {
        // evaluate the expression
        ExprOllirGenerator exprVisitor = new ExprOllirGenerator(symbolTable, this.methodName);
        String instruction = exprVisitor.visit(accessExpression, this.methodName);
        code.append(exprVisitor.getCode());
        return instruction;
    }

    /**
     * Constructs the instruction to access or alter a field array
     * There are two possible cases:
     * <p>
     * 1. The array is accessed as an assigned (i.e. c = array[i]), a simple get field instruction will suffice
     * from that field temp var we access the array position evaluated in the visitor
     * <p>
     * 2. The array is accessed as a assignee arr[i] = x,
     * in that case, x will be passed through assign_info as ollir code
     *
     * @param arrayAccessNode array access node
     * @param arrayName       name of the array
     * @param instruction     expression evaluated
     * @param assign_info     if assignee the string "assignee" else the name of the assigned variable
     * @return instruction
     */
    String fieldAccess(JmmNode arrayAccessNode, String arrayName, String instruction, String assign_info) {
        if (assign_info == null) throw new RuntimeException("Assign info is null");

        String array_temp = symbolTable.tempVar();
        code.append(String.format("%s :=.array.i32 getfield(this, %s.array.i32).array.i32;\n",
                array_temp, arrayName));
        String access = array_temp + "[ " + instruction + " ].i32";


        return access;
        /*// if it's assigned, x = arr[i], we return arr[i]
        if (assign_info.equals("assigned")) return access;

        // else it's the assignee, we append to the code arr[i] = x, and return ""
        code.append(String.format("%s :=.i32 %s;\n", access, assign_info));*/
        // return "";
    }


    public String getCode() {
        return code.toString();
    }

    private String visitDefault(JmmNode jmmNode, Object side) {
        return "";
    }

}
