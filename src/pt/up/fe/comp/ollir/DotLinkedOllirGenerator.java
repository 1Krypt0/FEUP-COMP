package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.analysis.Scope;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;


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
        ArgumentsOllirGenerator argumentsOllirGenerator =
                new ArgumentsOllirGenerator(this.symbolTable, this.methodName);
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
        Type variableType = null;

        if (caller.getKind().equals("DotLinked")) {
            variableType = new Type(symbolTable.getClassName(), false);
            caller_var = symbolTable.tempVar() + "." + symbolTable.getClassName();
            String instruction = visit(caller, null);
            code.append(String.format("%s :=.%s %s;\n", caller_var, symbolTable.getClassName(), instruction));

        } else if (caller.getKind().equals("ID")) {
            String name = caller.get("name");
            Scope scope = symbolTable.getVariableScope(name, methodName);

            if (scope.equals(Scope.LOCAL)) {
                variableType = symbolTable.getLocalVariableType(name, this.methodName);
                caller_var = name + "." + OllirUtils.getCode(variableType);
            } else if (scope.equals(Scope.ARGUMENT)) {
                int pos = symbolTable.getArgPosition(name, methodName);
                variableType = symbolTable.getArgumentType(methodName, name);
                String argumentType = OllirUtils.getCode(variableType);
                String temp_var = symbolTable.tempVar() + "." + argumentType; // SHOULD THIS BE THE NAME OF THE FIELD?
                code.append(String.format("%s :=.%s $%d.%s.%s;\n",
                        temp_var, argumentType, pos + 1, name, argumentType));
                caller_var = temp_var;
            } else if (scope.equals(Scope.FIELD)) {
                variableType = symbolTable.getFieldType(name);
                String fieldTypeCode = OllirUtils.getCode(variableType);
                caller_var = symbolTable.tempVar() + "." + fieldTypeCode; // SHOULD THIS BE THE NAME OF THE FIELD?

                code.append(String.format("%s :=.%s getfield(this, %s.%s).%s ;\n",
                        caller_var, fieldTypeCode, name, fieldTypeCode, fieldTypeCode));
            } else if (symbolTable.isImport(name) || scope.equals(Scope.CLASS)) {
                caller_var = name;
                variableType = new Type("Static Import", false);
            }


        } else if (caller.getKind().equals("ClassCreation")) {
            String className = caller.get("name");
            variableType = new Type(className, false);
            caller_var = symbolTable.tempVar() + "." + className;

            code.append(String.format("%s :=.%s new(%s).%s;\n", caller_var, className, className, className));
            code.append(String.format("invokespecial( %s , \"<init>\").V;\n", caller_var));
        } else if (caller.getKind().equals("This")) {
            // SHOULD THIS BE STORED IN A TEMPORARY VARIABLE?
            caller_var = "this";
            variableType = new Type(symbolTable.getClassName(), false);
        }

        if (callee.getKind().equals("MethodCall")) {
            return generateCodeForMethodCall(callee, (String) returnType, variableType, caller_var);
        } else if (callee.getKind().equals("Length")) {
            return generateCodeForArrayAccess(caller_var, (String) returnType);
        }


        // TODO: Implement Negation


        return "";
    }

    private String generateCodeForArrayAccess(String caller_var, String returnType) {
        String arrayLength = "arraylength(" + caller_var + ").i32";
        //if (returnType != null) return arrayLength;
        String temp_var = symbolTable.tempVar() + ".i32";
        code.append(String.format("%s :=.i32 %s;\n", temp_var, arrayLength));
        return temp_var;
    }


    String generateCodeForMethodCall(JmmNode callee, String returnType, Type variableType, String caller_var) {
        String methodName = callee.get("name");
        ArgumentsOllirGenerator argumentsOllirGenerator = new ArgumentsOllirGenerator(symbolTable, this.methodName);
        String argsString = argumentsOllirGenerator.visit(callee, 1);
        code.append(argumentsOllirGenerator.getCode());
        String returnTypeCode;


        if (symbolTable.isMethod(methodName)) returnTypeCode = "."
                + OllirUtils.getCode(symbolTable.getReturnType(methodName)); // if it exists, the return type is found
        else returnTypeCode = returnType == null ? ".V" : "." + returnType; // if it doesn't exist, the return type is forced


        String invoke = (variableType.getName() != null
                && variableType.getName().contains("Static")) ? "invokestatic" : "invokevirtual";
        String method_call = String.format("%s( %s , \"%s\" %s)%s",
                invoke, caller_var, methodName, argsString, returnTypeCode);


        if (returnType == null) return method_call;

        String temp_var = symbolTable.tempVar() + returnTypeCode;
        code.append(String.format("%s :=%s %s;\n", temp_var, returnTypeCode, method_call));
        return temp_var;
    }






    public String defaultVisit(JmmNode jmmNode, Object o) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

    public String getCode() {
        return code.toString();
    }
}
