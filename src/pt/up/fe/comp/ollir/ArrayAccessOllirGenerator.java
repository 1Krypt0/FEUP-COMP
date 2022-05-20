package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayAccessOllirGenerator extends AJmmVisitor<Object, String> {

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

    private String visitDefault(JmmNode jmmNode, Object side) {
        return "";
    }

    private String visitArrayAccessAssign(JmmNode arrayAssignAccess, Object o) {

        // JmmNode assignedVarIdNode = arrayAssignAccess.getChildren().stream().filter(node -> node.getKind().equals("ID")).findFirst().get();
        JmmNode arrayAccessNode = arrayAssignAccess.getChildren().stream().filter(node -> node.getKind().equals("ArrayAccess")).findFirst().get();


        String arrayName = arrayAssignAccess.get("name");
        JmmNode accessExpression = arrayAccessNode.getJmmChild(0);
        int argPos;

        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator(symbolTable, this.methodName);


        String instruction =  binOpOllirGenerator.visit(accessExpression, this.methodName);
        code.append(binOpOllirGenerator.getCode());

        if(this.symbolTable.hasLocalVariable(this.methodName, arrayName)) {
            return arrayName + "[" + instruction + "].i32";

        } else if((argPos = this.symbolTable.getArgumentPosition(this.methodName, arrayName)) != -1) {
            return "$" + argPos + "." + arrayName + "[" + instruction + "].i32";
        }
        else if (symbolTable.hasField(arrayName)){
            if (o.equals("right")) {
                // it's a getfield instruction
                String temp_var = "temp_" + arrayAssignAccess.get("col") + "_" + arrayAssignAccess.get("row");
                code.append(temp_var).append(" :=.array.i32 ").append("getfield(this, ").append(arrayName).append(")");

                return temp_var + "[ " + instruction + " ].i32";
            } else if(o.equals("left")) {
                // code.append("putfield(this, " + varName + "." + OllirUtils.getCode(varType) + ", " + instruction +  ").V;\n");
            }
        }

        return "";
    }


    public String getCode() {
        return code.toString();
    }


}
