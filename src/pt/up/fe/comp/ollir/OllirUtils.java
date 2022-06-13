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

    /**
     * Get the code string for a type, appended after the dot.
     *
     * @param type
     * @return the code string for the type
     */
    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) code.append("array.");

        code.append(getOllirType(type.getName()));

        return code.toString();
    }


    /**
     * Gets the Ollir type for a JMM type.
     * If void or int, returns V and i32
     * Else returns the JMM type name (annotated in the tree)
     *
     * @param jmmType the JMM type name
     * @return the Ollir type
     */

    public static String getOllirType(String jmmType) {

        if (jmmType.equals("void")) {
            return "V";
        } else if (jmmType.equals("int")) {
            return "i32";
        }
        return jmmType;
    }


    /**
     * Creates the constructor from a template string.
     *
     * @param symbolTable the symbol table
     * @return the constructor string
     */

    public static String createConstructor(SymbolTable symbolTable) {
        return ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}";
    }


    /**
     * Creates the local fields from the symbol table
     *
     * @param symbolTable the symbol table
     * @return the local fields string
     */

    public static String createLocalFields(SymbolTable symbolTable) {
        StringBuilder fields = new StringBuilder();

        for (Symbol symbol : symbolTable.getFields()) {
            fields.append(".field ").append(getCode(symbol)).append(";\n");
        }

        return fields.toString();
    }

    /**
     * Returns the list of all node annotations that correspond to a operation between two int32.
     *
     * @return the list of annotations
     **/
    public static List<String> getArithmeticOperations() {
        return List.of("add", "sub", "mult", "div", "lt");
    }

    /**
     * From a bin op node return the type of the assign to the variable on the left hand side.
     *
     * @param assignNode  the assign node
     * @param symbolTable the symbol table
     * @return the type of the variable on the left hand side
     */
    public static String getBinOpAssignCode(JmmNode assignNode, ProgramSymbolTable symbolTable, String methodName) {
        switch (assignNode.getKind()) {
            case "BinOp":
                String operation = assignNode.get("op");
                if (operation.equals("lt") || operation.equals("and")) return ":=.bool";
                else return ":=.i32";
            case "True":
            case "False":
            case  "Negation":
                return ":=.bool";
            case "IntegerLiteral":
                return ":=.i32";
            case "ID":
                Type type = symbolTable.getVariableType(assignNode.get("name"), methodName);
                return ":=." + getCode(type);
            case "ArrayCreation":
                return ":=.array.i32";
            case "ClassCreation":
                return ":=." + assignNode.get("name");
            default:
                return null;
        }
    }

    /**
     * From a bin op (with an arithmetic operation) node return the operator code.
     *
     * @param operation the operation annotated in the node
     * @return the operator code
     */
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
                return "<.bool";
            default:
                throw new IllegalArgumentException("Invalid arithmetic Op: " + operation);
        }
    }

    /**
     * Returns the bool variable code for a boolean literal
     *
     * @param booleanNode the boolean literal node
     * @return the boolean var code
     */
    public static String getBoolCode(JmmNode booleanNode) {
        return (booleanNode.getKind().equals("True")) ? "true.bool" : "false.bool";
    }


    /**
     * Returns the int variable code for an integer literal.
     *
     * @param node the integer literal node
     * @return the int var code
     */
    public static String getIntegerLiteralCode(JmmNode node) {
        return node.get("value") + ".i32";
    }


    /**
     * Returns the variable code for an identifier (with $argNo if necessary).
     *
     * @param idName      the variable node
     * @param symbolTable the symbol table
     * @param methodName  the method name (local scope)
     * @return the variable code
     */
    public static String getIdCode(String idName, String arrayAccess, ProgramSymbolTable symbolTable, String methodName) {
        StringBuilder code = new StringBuilder();
        Integer variableIndex = symbolTable.getArgPosition(idName, methodName);

        if (variableIndex != -1) code.append(String.format("$%d.", variableIndex));

        if (!arrayAccess.equals("")) code.append(String.format("%s%s.%s", idName, arrayAccess, "i32"));

        else
            code.append(String.format("%s.%s", idName, getCode(symbolTable.getVariableType(idName, methodName))));

        return code.toString();
    }


}
