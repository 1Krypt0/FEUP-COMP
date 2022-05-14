package pt.up.fe.comp.ollir;


import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getOllirType(String jmmType) {

        if (jmmType.equals("void")) {
            return "V";
        } else if (jmmType.equals("int")) {
            return "i32";
        }

        return jmmType;
    }

    public static String createConstructor(SymbolTable symbolTable) {

        return ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}";
    }

    public static String createLocalFields(SymbolTable symbolTable) {
        StringBuilder fields = new StringBuilder();

        for (Symbol symbol : symbolTable.getFields()) {
            fields.append(".field ").append(getCode(symbol)).append(";\n");
        }

        return fields.toString();
    }

    public static List<String> getArithmeticOperations() {
        return List.of("add", "sub", "mult", "div", "lt");
    }

    public static String getBinOpAssignCode(JmmNode assignNode, ProgramSymbolTable symbolTable, String methodName) {
        if(assignNode.getKind().equals("BinOp")) {
            String operation = assignNode.get("op");
            if (operation.equals("lt")) return ":=.bool"; // lt is arithmetic in operands but has boolean result
            if(operation.equals("and")) return ":=.bool";
            if (OllirUtils.getArithmeticOperations().contains(operation)) return ":=.i32";
            else return " ";
        }
        else if(assignNode.getKind().equals("True") || assignNode.getKind().equals("False")) {
            return ":=.bool";
        }
        else if(assignNode.getKind().equals("IntegerLiteral")) {
            return ":=.i32";
        }
        else if(assignNode.getKind().equals("ID")) {
            Type type = symbolTable.getVariableType(assignNode.get("name"));
            return ":=." + getCode(type);
        }

        return "";
        // Int
        // Bool
        // Array Access
        // Method Call
        // ID
        //
    }

    public static String getArithmeticOperationCode(String operation) {
        switch (operation) {
            case "add":
                return "+.i32";
            case "sub":
                return "-.i32";
            case "mult":
                return "*.i32";
            case "div":
                return "/.i32";
            case "lt":
                return "<.i32";
            default:
                throw new IllegalArgumentException("Invalid arithmetic Op: " + operation);
        }
    }

    public static String getBoolCode(JmmNode booleanNode) {
        return (booleanNode.getKind().equals("True")) ? "true.bool" : "false.bool";
    }

    public static String getIntegerLiteralCode(JmmNode node) {
        return node.get("value") + ".i32";
    }

    public static String getIdCode(JmmNode idNode, ProgramSymbolTable symbolTable) {
        return idNode.get("name") + "." +  getCode(symbolTable.getVariableType(idNode.get("name")));
    }
}
