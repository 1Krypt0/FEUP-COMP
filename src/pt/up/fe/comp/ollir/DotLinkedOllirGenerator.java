package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

/*
    Some examples:
    this.testFoo2(1, 1 + c).testFoo3(a);
    b.length;
    io.println(c, d); -> Should take care of the return type, void if not assigned (static/imported)
 */
public class DotLinkedOllirGenerator extends AJmmVisitor<Object, String> {

    final StringBuilder code;
    final ProgramSymbolTable symbolTable;
    final String methodName;

    public DotLinkedOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;

        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.Method_Call, this::visitMethodCall);
        setDefaultVisit(this::defaultVisit);
    }


    private String visitMethodCall(JmmNode methodCall, Object hasReturnType) {
        /* Parses arguments and return type*/
        String methodName = methodCall.get("name");
        Type returnType = symbolTable.getReturnType(methodName);

        ArgumentsOllirGenerator argumentsOllirGenerator = new ArgumentsOllirGenerator(this.symbolTable, this.methodName);
        String args = argumentsOllirGenerator.visit(methodCall, 1);

        code.append(argumentsOllirGenerator.getCode());

        /*if(hasReturnType != null) {
            String returnTypeCode = (returnType == null) ? ".V" : "." + OllirUtils.getCode(returnType);
            return args + ")" + returnTypeCode + ";\n";
        }*/
        return args + ");\n";
    }


    private String visitDotLinked(JmmNode dotLinkedNode, Object returnType) {

        // TODO: the inside of invoke changes but could be parsed separately

        JmmNode caller = dotLinkedNode.getJmmChild(0);
        JmmNode callee = dotLinkedNode.getJmmChild(1);

        String caller_var = "";
        int argumentPosition;
        Type variableType = null;

        if (caller.getKind().equals("DotLinked")) {
            variableType = new Type(symbolTable.getClassName(), false);
            String type = symbolTable.getClassName();
            caller_var = "temp" + "_" + caller.get("col") + "_" + caller.get("line") + "." + type;
            String instruction = visit(caller, null);
            code.append(caller_var).append(" :=.").append(symbolTable.getClassName()).append(" ").append(instruction);
        } else if (caller.getKind().equals("ID")) {

            String name = caller.get("name");

            // search through scopes
            if (symbolTable.isLocalVariable(name, methodName)) {
                variableType = symbolTable.getLocalVariableType(name, this.methodName);
                caller_var = name + "." + OllirUtils.getCode(variableType);
                 /* if(Objects.equals(variableType.getName(), this.symbolTable.getClassName())){
                    caller_var = name + "." + OllirUtils.getCode(variableType);
                } else if(this.symbolTable.isImport(variableType.getName())){
                    caller_var = name + "." + OllirUtils.getCode(variableType);
                } else if(variableType.isArray()){
                    caller_var = name + "." + OllirUtils.getCode(variableType);
                }*/
            } else if ((argumentPosition = symbolTable.getArgPosition(name, methodName)) != -1) {
                variableType = symbolTable.getArgumentType(methodName, name);
                String argumentType = OllirUtils.getCode(variableType);
                String temp_var = "temp" + "_" + caller.get("col") + "_" + caller.get("line") + "." + argumentType; // SHOULD THIS BE THE NAME OF THE FIELD?

                code.append(temp_var).append(" :=.").append(argumentType).append(" ").append("$").
                        append(argumentPosition + 1).append(".").append(name).append(".").
                        append(argumentType).append(";\n");

                caller_var = temp_var;
            } else if (symbolTable.isField(name)) {
                variableType = symbolTable.getFieldType(name);
                String fieldTypeCode = OllirUtils.getCode(variableType);
                caller_var = symbolTable.tempVar() + "." + fieldTypeCode; // SHOULD THIS BE THE NAME OF THE FIELD?
                String instruction = caller_var + " :=." + fieldTypeCode + " getfield(this," + name + "." + fieldTypeCode + ")" + "." + fieldTypeCode + ";\n";
                code.append(instruction);
                // if type import
                // if type is class type
                // if array
            } else if (symbolTable.isImport(name) || symbolTable.getClassName().equals(name)) {
                caller_var = name;
                variableType = new Type("Static Import", false);
            }
        } else if (caller.getKind().equals("ClassCreation")) {
            String className = caller.get("name");
            variableType = new Type(className, false);
            caller_var = "temp" + "_" + caller.get("col") + "_" + caller.get("line") + "." + className;
            code.append(caller_var).append(".").append(className).append(" :=.").append(className).append(" new(").append(className).append(").").append(className).append(";\n");
            code.append("invokespecial(").append(caller_var).append(", \"<init>\").V;\n");

        } else if (caller.getKind().equals("This")) {
            // SHOULD THIS BE STORED IN A TEMPORARY VARIABLE?
            caller_var = "this";
            variableType = new Type(symbolTable.getClassName(), false);
        }

        // TODO: Implement Negation

        if (callee.getKind().equals("MethodCall")) {
            String methodName = callee.get("name");
            ArgumentsOllirGenerator argumentsOllirGenerator = new ArgumentsOllirGenerator(symbolTable, this.methodName);
            code.append(argumentsOllirGenerator.getCode());
            String argsString = argumentsOllirGenerator.visit(callee, 1);
            String returnTypeCode;

            if (symbolTable.isMethod(methodName)) {
                returnTypeCode = "." + OllirUtils.getCode(symbolTable.getReturnType(methodName));
            } else {
                returnTypeCode = (returnType == null) ? ".V" : ((returnType instanceof String) ? (String) returnType : null);
            }

            String invoke = (variableType.getName() != null && variableType.getName().contains("Static")) ? "invokestatic" : "invokevirtual";
            return invoke + "(" + caller_var + ",\"" + methodName + "\"" + argsString + ")" + returnTypeCode + ";\n";
        } else if (callee.getKind().equals("Length")) {
            return "arraylength(" + caller_var + ").i32";
        } else if (callee.getKind().equals("ArrayAccess")) {
            // TODO: use caller variable and compound []
        }


        /*
        // DOES THIS EXIST IN OLLIR?
        else if (callee.getKind().equals("ID")){
            // ( this.varOfSameClassType ) .foo() or varOfSameClassType.a
        }
         */

        return "";
    }


    public String defaultVisit(JmmNode jmmNode, Object o) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

    public String getCode() {
        return code.toString();
    }
}
